# Manga Download Manager — Design Spec

**Date:** 2026-05-25
**Branch:** feat/downloading
**Status:** Approved

---

## Overview

Replace the existing `DownloadManager`-based chapter download in `GenericManga` with a
coroutine-driven `MangaDownloadManager` that supports progress monitoring, cancellation, status
checking, offline chapter retrieval, and batch sequential downloads across Android and JVM Desktop.

---

## Architecture

### New files

```
mangaworld/shared/src/
  commonMain/kotlin/com/programmersbox/manga/shared/downloads/
    MangaDownloadManager.kt          expect class + data models
    DownloadCore.kt                  internal shared Ktor download loop

  androidMain/kotlin/com/programmersbox/manga/shared/downloads/
    MangaDownloadManager.android.kt  WorkManager-backed actual class
    DownloadChapterWorker.kt         CoroutineWorker (thin wrapper over core)

  jvmMain/kotlin/com/programmersbox/manga/shared/downloads/
    MangaDownloadManager.jvm.kt      Channel + CoroutineScope actual class
```

### Modified files

- `mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt`
  — replace `downloadFullChapter` / `downloadChapter` with delegation to `MangaDownloadManager`
  — add Koin registrations for `MangaDownloadManager` and `DownloadChapterWorker`
- `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericMangaDesktop.kt`
  — add Koin registration for JVM `MangaDownloadManager`

### File storage layout (unchanged from existing convention)

```
Downloads/MangaWorld/{mangaTitle}/{chapter.name}/{000.png, 001.png, …}
```

Images are named with zero-padded three-digit indices to guarantee sort order.
Filesystem is the source of truth — no database needed.

---

## Data Model

```kotlin
// commonMain — MangaDownloadManager.kt

sealed interface DownloadState {
    data object Queued : DownloadState
    data class Downloading(val imagesDownloaded: Int, val totalImages: Int) : DownloadState
    data object Completed : DownloadState
    data class Failed(val reason: String) : DownloadState
    data object Cancelled : DownloadState
}

data class ChapterDownloadProgress(
    val chapterUrl: String,   // unique identifier
    val chapterName: String,
    val mangaTitle: String,
    val state: DownloadState,
)
```

---

## Public API (`expect class`)

```kotlin
expect class MangaDownloadManager {

    /** Add one chapter to the sequential download queue. */
    fun downloadChapter(chapter: KmpChapterModel, mangaTitle: String)

    /** Add multiple chapters to the queue in order. */
    fun downloadChapters(chapters: List<KmpChapterModel>, mangaTitle: String)

    /** Cancel a specific chapter by URL (queued or in-progress). */
    fun cancelDownload(chapterUrl: String)

    /** Cancel all queued and active downloads. */
    fun cancelAll()

    /**
     * Returns the local folder path if the chapter is fully downloaded, null otherwise.
     * Synchronous filesystem check — no network.
     * Use: isChapterDownloaded == getDownloadedChapterPath(...) != null
     */
    fun getDownloadedChapterPath(chapter: KmpChapterModel, mangaTitle: String): String?

    /** Live stream of all active/queued/recently-completed downloads. */
    fun observeDownloads(): Flow<List<ChapterDownloadProgress>>

    /** Delete a chapter's folder from disk. */
    fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String)
}
```

`isChapterDownloaded` is not a separate method — callers use `getDownloadedChapterPath(…) != null`.

---

## Shared Download Core (`DownloadCore.kt`, `commonMain`, `internal`)

```kotlin
internal data class DownloadRequest(
    val chapterUrl: String,
    val chapterName: String,
    val mangaTitle: String,
    val imageUrls: List<String>,
    val headers: Map<String, String>,
)

internal suspend fun executeDownload(
    client: HttpClient,
    request: DownloadRequest,
    maxRetries: Int = 3,
    onProgress: (imagesDownloaded: Int, totalImages: Int) -> Unit,
    writeBytes: suspend (index: Int, bytes: ByteArray) -> Unit,
)
```

**Loop behaviour:**

1. `onProgress(0, imageUrls.size)` immediately on start.
2. For each URL (with index):
   - Retry loop up to `maxRetries` attempts with exponential backoff (`2^attempt × 1000 ms`).
   - `client.prepareGet(url) { headers { … } }.execute { bodyAsBytes() }`
   - Success → `writeBytes(index, bytes)` → `onProgress(index + 1, total)`
   - Network / IO error → wait and retry.
   - HTTP 4xx → skip image, log warning, no retry (image unavailable, not transient).
3. Throws on unrecoverable failure after all retries exhausted.

`writeBytes` is a platform-supplied lambda — keeps `java.io.File` out of `commonMain`.
Both platforms pass `File(destDir, "%03d.png".format(index)).writeBytes(bytes)`.

---

## Android Implementation (`actual class`, WorkManager)

**Constructor:** `(context: Context, httpClient: HttpClient)`

**Root dir:** `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)/MangaWorld/`

### `downloadChapter`
1. Immediately emits `Queued` state for the chapter.
2. Launches coroutine: calls `chapter.getChapterInfo()`, extracts URLs + headers.
3. Serializes to JSON (kotlinx.serialization), builds `WorkRequest` with input data:
   `mangaTitle`, `chapterName`, `chapterUrl`, `imageUrlsJson`, `headersJson`.
4. Tags work with both `DOWNLOAD_TAG` and `chapterUrl`.
5. Enqueues via:
   ```kotlin
   workManager.beginUniqueWork("manga_downloads", APPEND, workRequest).enqueue()
   ```

### `DownloadChapterWorker` (`CoroutineWorker`, Koin-injected)
```kotlin
override suspend fun doWork(): Result {
    val request = DownloadRequest(/* deserialize from inputData */)
    return try {
        executeDownload(
            client = httpClient,
            request = request,
            onProgress = { done, total ->
                setProgress(workDataOf("done" to done, "total" to total))
            },
            writeBytes = { index, bytes ->
                File(destDir, "%03d.png".format(index)).writeBytes(bytes)
            }
        )
        Result.success()
    } catch (e: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
```

### `observeDownloads()`
`workManager.getWorkInfosByTagFlow(DOWNLOAD_TAG)` mapped to `List<ChapterDownloadProgress>`.
Progress fields (`imagesDownloaded`, `totalImages`) read from `WorkInfo.progress`.

### `cancelDownload(chapterUrl)`
`workManager.cancelAllWorkByTag(chapterUrl)`

### `getDownloadedChapterPath` / `deleteChapter`
Direct `java.io.File` calls against the root dir.

---

## JVM Implementation (`actual class`, Coroutines)

**Constructor:** `(httpClient: HttpClient, scope: CoroutineScope)`

**Root dir:** `System.getProperty("user.home")/Downloads/MangaWorld/`

### State
```kotlin
private val _downloads = MutableStateFlow<List<ChapterDownloadProgress>>(emptyList())
private val cancelledUrls = mutableSetOf<String>()
private val activeJob = AtomicReference<Pair<String, Job>?>(null)  // (chapterUrl, Job)
private val queue = Channel<DownloadRequest>(Channel.UNLIMITED)
```

### Init
Single consumer coroutine launched in `scope`:
```kotlin
for (request in queue) {
    if (request.chapterUrl in cancelledUrls) {
        cancelledUrls.remove(request.chapterUrl)
        updateState(request.chapterUrl, DownloadState.Cancelled)
        continue
    }
    val job = scope.launch {
        executeDownload(
            client = httpClient,
            request = request,
            onProgress = { done, total ->
                updateState(request.chapterUrl, DownloadState.Downloading(done, total))
            },
            writeBytes = { index, bytes ->
                File("${rootDir}/${request.mangaTitle}/${request.chapterName}",
                     "%03d.png".format(index)).writeBytes(bytes)
            }
        )
        updateState(request.chapterUrl, DownloadState.Completed)
    }
    activeJob.set(request.chapterUrl to job)
    job.join()
    activeJob.set(null)
}
```

### `downloadChapter`
Launches coroutine: calls `chapter.getChapterInfo()`, extracts URLs + headers, adds `Queued`
entry to `_downloads`, sends `DownloadRequest` to `queue`.

### `cancelDownload(chapterUrl)`
Adds to `cancelledUrls`. If it matches `activeJob`, cancels that `Job`.

### `cancelAll`
Cancels all child jobs of `scope`, closes and re-creates `queue`, clears state.

### `observeDownloads()`
Returns `_downloads.asStateFlow()`.

### `getDownloadedChapterPath` / `deleteChapter`
Direct `java.io.File` calls against root dir.

---

## Koin Registration

**Android (`GenericManga.kt` appModule):**
```kotlin
singleOf(::MangaDownloadManager)
// DownloadChapterWorker registered via KoinWorkerFactory
```

**JVM Desktop (`GenericMangaDesktop.kt`):**
```kotlin
single { MangaDownloadManager(get(), CoroutineScope(Dispatchers.IO + SupervisorJob())) }
```

---

## Call-site Changes

**`GenericManga.downloadChapter()`** (Android):
```kotlin
override fun downloadChapter(model: KmpChapterModel, allChapters, infoModel, navController) {
    mangaDownloadManager.downloadChapter(model, infoModel.title.ifBlank { infoModel.url })
}
```

**Opening a downloaded chapter** (detail/download screen):
```kotlin
val path = mangaDownloadManager.getDownloadedChapterPath(chapter, mangaTitle)
if (path != null) {
    ReadViewModel.navigateToMangaReader(navController, filePath = path, downloaded = true)
}
```

---

## Out of Scope

- UI implementation (DownloadScreen already exists)
- Authentication / auth headers beyond what `KmpStorage.headers` provides
- Source-specific scraping
- CBZ/CBR compression
- iOS implementation (stub only, no active iOS target)
