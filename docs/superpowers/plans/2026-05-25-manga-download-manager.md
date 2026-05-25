# Manga Download Manager Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `DownloadManager`-based chapter download in `GenericManga` with a coroutine-driven `MangaDownloadManager` supporting progress monitoring, cancellation, status checking, offline chapter path retrieval, and sequential batch downloads across Android (WorkManager) and JVM Desktop (coroutines).

**Architecture:** `expect class MangaDownloadManager` in `mangaworld/shared` `commonMain`. A shared `executeDownload` suspend function in `DownloadCore.kt` handles the Ktor byte-fetch loop and retry logic for both platforms. The Android `actual` enqueues `DownloadChapterWorker` instances via WorkManager; the JVM `actual` drives a `Channel`-backed sequential coroutine queue.

**Tech Stack:** Ktor `ktor-client-core` (commonMain HTTP), `androidx.work:work-runtime` (Android, transitive via kmpuiviews), `koin-androidx-workmanager` (Worker DI), `kotlinx.serialization` (WorkManager input serialization), `kotlinx-coroutines-test` + `ktor-client-mock` (tests)

---

## File Map

| Action | Path | Responsibility |
|--------|------|---------------|
| Create | `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.kt` | `expect class`, `DownloadState`, `ChapterDownloadProgress` |
| Create | `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadCore.kt` | `DownloadRequest`, `executeDownload`, `String.sanitize()` |
| Create | `mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.android.kt` | WorkManager-backed `actual class` |
| Create | `mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadChapterWorker.kt` | `CoroutineWorker` wrapping `executeDownload` |
| Create | `mangaworld/shared/src/jvmMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.jvm.kt` | `Channel` + `CoroutineScope` `actual class` |
| Create | `mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/downloads/DownloadCoreTest.kt` | Unit tests for `executeDownload` |
| Modify | `mangaworld/shared/build.gradle.kts` | Add `jvmTest` deps (`ktor-client-mock`, `coroutinesTest`) and version catalog entry |
| Modify | `gradle/libs.versions.toml` | Add `ktorMock` entry |
| Modify | `mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt` | Inject `MangaDownloadManager`, register Worker, replace `downloadFullChapter` |
| Modify | `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericMangaDesktop.kt` | Inject `MangaDownloadManager`, wire `downloadChapter` |

---

## Task 1: Data models, `expect class`, and `DownloadCore`

**Files:**
- Create: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.kt`
- Create: `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadCore.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `mangaworld/shared/build.gradle.kts`

- [ ] **Step 1: Add `ktorMock` to version catalog**

In `gradle/libs.versions.toml`, find the `[libraries]` section near `ktorCore` and add one line:

```toml
ktorMock = { module = "io.ktor:ktor-client-mock", version.ref = "ktorVersion" }
```

- [ ] **Step 2: Add `jvmTest` dependencies to `mangaworld/shared/build.gradle.kts`**

Inside the existing `sourceSets { }` block, after `jvmMain.dependencies { ... }`, add:

```kotlin
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutinesTest)
            implementation(libs.ktorMock)
        }
```

- [ ] **Step 3: Create `MangaDownloadManager.kt`**

Create `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.kt`:

```kotlin
package com.programmersbox.manga.shared.downloads

import com.programmersbox.kmpmodels.KmpChapterModel
import kotlinx.coroutines.flow.Flow

sealed interface DownloadState {
    data object Queued : DownloadState
    data class Downloading(val imagesDownloaded: Int, val totalImages: Int) : DownloadState
    data object Completed : DownloadState
    data class Failed(val reason: String) : DownloadState
    data object Cancelled : DownloadState
}

data class ChapterDownloadProgress(
    val chapterUrl: String,
    val chapterName: String,
    val mangaTitle: String,
    val state: DownloadState,
)

expect class MangaDownloadManager {
    fun downloadChapter(chapter: KmpChapterModel, mangaTitle: String)
    fun downloadChapters(chapters: List<KmpChapterModel>, mangaTitle: String)
    fun cancelDownload(chapterUrl: String)
    fun cancelAll()
    fun getDownloadedChapterPath(chapter: KmpChapterModel, mangaTitle: String): String?
    fun observeDownloads(): Flow<List<ChapterDownloadProgress>>
    fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String)
}
```

- [ ] **Step 4: Create `DownloadCore.kt`**

Create `mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadCore.kt`:

```kotlin
package com.programmersbox.manga.shared.downloads

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

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
) {
    val urls = request.imageUrls
    onProgress(0, urls.size)

    urls.forEachIndexed { index, url ->
        var handled = false
        var attempt = 0

        while (!handled) {
            try {
                client.prepareGet(url) {
                    headers { request.headers.forEach { (k, v) -> append(k, v) } }
                }.execute { response ->
                    when {
                        response.status.isSuccess() -> {
                            writeBytes(index, response.bodyAsBytes())
                            handled = true
                        }
                        response.status.value in 400..499 -> {
                            handled = true // image unavailable, skip without retry
                        }
                        else -> throw Exception("HTTP ${response.status.value} for $url")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempt++
                if (attempt >= maxRetries) throw e
                delay((1L shl (attempt - 1)) * 1000L) // 1s, 2s, 4s
            }
        }

        onProgress(index + 1, urls.size)
    }
}

internal fun String.sanitize(): String = replace(Regex("[/\\\\:*?\"<>|]"), "_")
```

- [ ] **Step 5: Verify it compiles**

```bash
./gradlew :mangaworld:shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL` (iOS stub compilation may warn but should not fail)

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml mangaworld/shared/build.gradle.kts \
  mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.kt \
  mangaworld/shared/src/commonMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadCore.kt
git commit -m "feat: add MangaDownloadManager expect class and shared DownloadCore"
```

---

## Task 2: Tests for `DownloadCore`

**Files:**
- Create: `mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/downloads/DownloadCoreTest.kt`

- [ ] **Step 1: Create the test file with failing tests**

Create `mangaworld/shared/src/jvmTest/kotlin/com/programmersbox/manga/shared/downloads/DownloadCoreTest.kt`:

```kotlin
package com.programmersbox.manga.shared.downloads

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DownloadCoreTest {

    private fun makeRequest(urls: List<String>) = DownloadRequest(
        chapterUrl = "https://example.com/chapter/1",
        chapterName = "Chapter 1",
        mangaTitle = "Test Manga",
        imageUrls = urls,
        headers = emptyMap(),
    )

    @Test
    fun `happy path - writes bytes in index order and reports progress`() = runTest {
        val imageData = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
        val written = mutableListOf<Pair<Int, ByteArray>>()
        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        var callCount = 0

        val client = HttpClient(MockEngine {
            respond(imageData[callCount++], HttpStatusCode.OK)
        })

        executeDownload(
            client = client,
            request = makeRequest(listOf("img0", "img1")),
            onProgress = { done, total -> progressUpdates.add(done to total) },
            writeBytes = { index, bytes -> written.add(index to bytes) },
        )

        assertEquals(2, written.size)
        assertEquals(0, written[0].first)
        assertEquals(1, written[1].first)
        assert(imageData[0].contentEquals(written[0].second))
        assert(imageData[1].contentEquals(written[1].second))
        assertEquals(listOf(0 to 2, 1 to 2, 2 to 2), progressUpdates)
    }

    @Test
    fun `skips 404 images without writing bytes`() = runTest {
        val written = mutableListOf<Int>()
        var callCount = 0

        val client = HttpClient(MockEngine {
            val status = if (callCount++ == 1) HttpStatusCode.NotFound else HttpStatusCode.OK
            respond(byteArrayOf(callCount.toByte()), status)
        })

        executeDownload(
            client = client,
            request = makeRequest(listOf("img0", "img1", "img2")),
            onProgress = { _, _ -> },
            writeBytes = { index, _ -> written.add(index) },
        )

        assertEquals(listOf(0, 2), written) // index 1 skipped (404)
    }

    @Test
    fun `retries on 5xx and succeeds on third attempt`() = runTest {
        var attempt = 0
        val client = HttpClient(MockEngine {
            attempt++
            if (attempt < 3) respond(byteArrayOf(), HttpStatusCode.InternalServerError)
            else respond(byteArrayOf(42), HttpStatusCode.OK)
        })

        val written = mutableListOf<ByteArray>()

        executeDownload(
            client = client,
            request = makeRequest(listOf("img0")),
            maxRetries = 3,
            onProgress = { _, _ -> },
            writeBytes = { _, bytes -> written.add(bytes) },
        )

        assertEquals(3, attempt)
        assertEquals(1, written.size)
        assert(byteArrayOf(42).contentEquals(written[0]))
    }

    @Test
    fun `throws after exhausting retries`() = runTest {
        val client = HttpClient(MockEngine {
            respond(byteArrayOf(), HttpStatusCode.InternalServerError)
        })

        assertFailsWith<Exception> {
            executeDownload(
                client = client,
                request = makeRequest(listOf("img0")),
                maxRetries = 2,
                onProgress = { _, _ -> },
                writeBytes = { _, _ -> },
            )
        }
    }

    @Test
    fun `sanitize replaces illegal filename characters`() {
        assertEquals("manga_title", "manga/title".sanitize())
        assertEquals("ch_1_2", "ch:1\\2".sanitize())
        assertEquals("no change", "no change".sanitize())
    }
}
```

- [ ] **Step 2: Run tests — expect failures because `executeDownload` is not yet accessible**

```bash
./gradlew :mangaworld:shared:jvmTest
```

Expected: Tests run. They should PASS since `executeDownload` is already written in Task 1. If any fail, the test logic or `DownloadCore.kt` implementation has a bug — fix it before continuing.

> Note: `delay()` calls in retry logic are instant in `runTest` because it uses a `TestCoroutineScheduler`.

- [ ] **Step 3: Commit**

```bash
git add mangaworld/shared/src/jvmTest/
git commit -m "test: add DownloadCore unit tests"
```

---

## Task 3: `DownloadChapterWorker` (Android)

**Files:**
- Create: `mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadChapterWorker.kt`

- [ ] **Step 1: Create the Worker**

Create `mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadChapterWorker.kt`:

```kotlin
package com.programmersbox.manga.shared.downloads

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import java.io.File

class DownloadChapterWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val httpClient = HttpClient()

    override suspend fun doWork(): Result {
        val mangaTitle = inputData.getString(KEY_MANGA_TITLE) ?: return Result.failure()
        val chapterName = inputData.getString(KEY_CHAPTER_NAME) ?: return Result.failure()
        val chapterUrl = inputData.getString(KEY_CHAPTER_URL) ?: return Result.failure()
        val imageUrls = inputData.getString(KEY_IMAGE_URLS)
            ?.let { Json.decodeFromString<List<String>>(it) }
            ?: return Result.failure()
        val headers = inputData.getString(KEY_HEADERS)
            ?.let { Json.decodeFromString<Map<String, String>>(it) }
            ?: emptyMap()

        val destDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "MangaWorld/${mangaTitle.sanitize()}/${chapterName.sanitize()}"
        ).also { it.mkdirs() }

        val request = DownloadRequest(
            chapterUrl = chapterUrl,
            chapterName = chapterName,
            mangaTitle = mangaTitle,
            imageUrls = imageUrls,
            headers = headers,
        )

        return try {
            executeDownload(
                client = httpClient,
                request = request,
                onProgress = { done, total ->
                    setProgress(workDataOf(KEY_PROGRESS_DONE to done, KEY_PROGRESS_TOTAL to total))
                },
                writeBytes = { index, bytes ->
                    File(destDir, "%03d.png".format(index)).writeBytes(bytes)
                },
            )
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry()
            else Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    companion object {
        const val KEY_MANGA_TITLE = "mangaTitle"
        const val KEY_CHAPTER_NAME = "chapterName"
        const val KEY_CHAPTER_URL = "chapterUrl"
        const val KEY_IMAGE_URLS = "imageUrls"
        const val KEY_HEADERS = "headers"
        const val KEY_PROGRESS_DONE = "done"
        const val KEY_PROGRESS_TOTAL = "total"
        const val KEY_ERROR = "error"
        const val DOWNLOAD_TAG = "manga_chapter_download"
        const val DOWNLOAD_QUEUE = "manga_download_queue"
    }
}
```

- [ ] **Step 2: Verify Android compilation**

```bash
./gradlew :mangaworld:shared:compileDebugKotlinAndroid
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadChapterWorker.kt
git commit -m "feat: add DownloadChapterWorker"
```

---

## Task 4: Android `actual class MangaDownloadManager`

**Files:**
- Create: `mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.android.kt`

- [ ] **Step 1: Create the Android actual class**

Create `mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.android.kt`:

```kotlin
package com.programmersbox.manga.shared.downloads

import android.content.Context
import android.os.Environment
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.programmersbox.kmpmodels.KmpChapterModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

actual class MangaDownloadManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val rootDir: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "MangaWorld"
        )

    actual fun downloadChapter(chapter: KmpChapterModel, mangaTitle: String) {
        scope.launch {
            val storages = chapter.getChapterInfo().firstOrNull() ?: return@launch
            val urls = storages.mapNotNull { it.link }
            if (urls.isEmpty()) return@launch
            val headers = storages
                .flatMap { it.headers.entries }
                .associate { it.key to it.value }

            val inputData = workDataOf(
                DownloadChapterWorker.KEY_MANGA_TITLE to mangaTitle,
                DownloadChapterWorker.KEY_CHAPTER_NAME to chapter.name,
                DownloadChapterWorker.KEY_CHAPTER_URL to chapter.url,
                DownloadChapterWorker.KEY_IMAGE_URLS to Json.encodeToString(urls),
                DownloadChapterWorker.KEY_HEADERS to Json.encodeToString(headers),
            )

            val workRequest = OneTimeWorkRequestBuilder<DownloadChapterWorker>()
                .setInputData(inputData)
                .addTag(DownloadChapterWorker.DOWNLOAD_TAG)
                .addTag(chapter.url)
                .build()

            workManager
                .beginUniqueWork(
                    DownloadChapterWorker.DOWNLOAD_QUEUE,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    workRequest,
                )
                .enqueue()
        }
    }

    actual fun downloadChapters(chapters: List<KmpChapterModel>, mangaTitle: String) {
        chapters.forEach { downloadChapter(it, mangaTitle) }
    }

    actual fun cancelDownload(chapterUrl: String) {
        workManager.cancelAllWorkByTag(chapterUrl)
    }

    actual fun cancelAll() {
        workManager.cancelAllWorkByTag(DownloadChapterWorker.DOWNLOAD_TAG)
    }

    actual fun getDownloadedChapterPath(chapter: KmpChapterModel, mangaTitle: String): String? {
        val dir = File(rootDir, "${mangaTitle.sanitize()}/${chapter.name.sanitize()}")
        return if (dir.exists() && dir.listFiles()?.isNotEmpty() == true) dir.absolutePath else null
    }

    actual fun observeDownloads(): Flow<List<ChapterDownloadProgress>> =
        workManager.getWorkInfosByTagFlow(DownloadChapterWorker.DOWNLOAD_TAG).map { infos ->
            infos.map { info ->
                ChapterDownloadProgress(
                    chapterUrl = info.inputData.getString(DownloadChapterWorker.KEY_CHAPTER_URL)
                        ?: info.tags.firstOrNull { it != DownloadChapterWorker.DOWNLOAD_TAG }
                        ?: "",
                    chapterName = info.inputData.getString(DownloadChapterWorker.KEY_CHAPTER_NAME) ?: "",
                    mangaTitle = info.inputData.getString(DownloadChapterWorker.KEY_MANGA_TITLE) ?: "",
                    state = when (info.state) {
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.BLOCKED -> DownloadState.Queued
                        WorkInfo.State.RUNNING -> DownloadState.Downloading(
                            imagesDownloaded = info.progress.getInt(DownloadChapterWorker.KEY_PROGRESS_DONE, 0),
                            totalImages = info.progress.getInt(DownloadChapterWorker.KEY_PROGRESS_TOTAL, 0),
                        )
                        WorkInfo.State.SUCCEEDED -> DownloadState.Completed
                        WorkInfo.State.FAILED -> DownloadState.Failed(
                            info.outputData.getString(DownloadChapterWorker.KEY_ERROR) ?: "Unknown"
                        )
                        WorkInfo.State.CANCELLED -> DownloadState.Cancelled
                    },
                )
            }
        }

    actual fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String) {
        File(rootDir, "${mangaTitle.sanitize()}/${chapter.name.sanitize()}").deleteRecursively()
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :mangaworld:shared:compileDebugKotlinAndroid
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.android.kt
git commit -m "feat: add Android MangaDownloadManager actual (WorkManager-backed)"
```

---

## Task 5: JVM `actual class MangaDownloadManager`

**Files:**
- Create: `mangaworld/shared/src/jvmMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.jvm.kt`

- [ ] **Step 1: Create the JVM actual class**

Create `mangaworld/shared/src/jvmMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.jvm.kt`:

```kotlin
package com.programmersbox.manga.shared.downloads

import com.programmersbox.kmpmodels.KmpChapterModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicReference

actual class MangaDownloadManager(private val scope: CoroutineScope) {

    private val httpClient = HttpClient()
    private val queue = Channel<DownloadRequest>(Channel.UNLIMITED)
    private val _downloads = MutableStateFlow<List<ChapterDownloadProgress>>(emptyList())
    private val cancelledUrls = mutableSetOf<String>()
    private val mutex = Mutex()
    private val activeJob = AtomicReference<Pair<String, Job>?>(null)

    private val rootDir: String
        get() = "${System.getProperty("user.home")}/Downloads/MangaWorld"

    init {
        scope.launch {
            for (request in queue) {
                val cancelled = mutex.withLock { cancelledUrls.remove(request.chapterUrl) }
                if (cancelled) {
                    updateState(request.chapterUrl) { it.copy(state = DownloadState.Cancelled) }
                    continue
                }

                val job = scope.launch {
                    val destDir = File(
                        "$rootDir/${request.mangaTitle.sanitize()}/${request.chapterName.sanitize()}"
                    ).also { it.mkdirs() }

                    try {
                        executeDownload(
                            client = httpClient,
                            request = request,
                            onProgress = { done, total ->
                                updateState(request.chapterUrl) {
                                    it.copy(state = DownloadState.Downloading(done, total))
                                }
                            },
                            writeBytes = { index, bytes ->
                                File(destDir, "%03d.png".format(index)).writeBytes(bytes)
                            },
                        )
                        updateState(request.chapterUrl) { it.copy(state = DownloadState.Completed) }
                    } catch (e: CancellationException) {
                        updateState(request.chapterUrl) { it.copy(state = DownloadState.Cancelled) }
                        throw e
                    } catch (e: Exception) {
                        updateState(request.chapterUrl) {
                            it.copy(state = DownloadState.Failed(e.message ?: "Unknown"))
                        }
                    }
                }

                activeJob.set(request.chapterUrl to job)
                job.join()
                activeJob.set(null)
            }
        }
    }

    actual fun downloadChapter(chapter: KmpChapterModel, mangaTitle: String) {
        scope.launch {
            val storages = chapter.getChapterInfo().firstOrNull() ?: return@launch
            val urls = storages.mapNotNull { it.link }
            if (urls.isEmpty()) return@launch
            val headers = storages
                .flatMap { it.headers.entries }
                .associate { it.key to it.value }

            val request = DownloadRequest(
                chapterUrl = chapter.url,
                chapterName = chapter.name,
                mangaTitle = mangaTitle,
                imageUrls = urls,
                headers = headers,
            )

            mutex.withLock {
                _downloads.update { list ->
                    list + ChapterDownloadProgress(
                        chapterUrl = chapter.url,
                        chapterName = chapter.name,
                        mangaTitle = mangaTitle,
                        state = DownloadState.Queued,
                    )
                }
            }
            queue.send(request)
        }
    }

    actual fun downloadChapters(chapters: List<KmpChapterModel>, mangaTitle: String) {
        chapters.forEach { downloadChapter(it, mangaTitle) }
    }

    actual fun cancelDownload(chapterUrl: String) {
        scope.launch {
            mutex.withLock { cancelledUrls.add(chapterUrl) }
            val (url, job) = activeJob.get() ?: return@launch
            if (url == chapterUrl) job.cancel()
        }
    }

    actual fun cancelAll() {
        val pending = _downloads.value
            .filter { it.state is DownloadState.Queued || it.state is DownloadState.Downloading }
            .map { it.chapterUrl }
        scope.launch {
            mutex.withLock { cancelledUrls.addAll(pending) }
        }
        activeJob.get()?.second?.cancel()
        _downloads.update { list ->
            list.map { p ->
                if (p.state is DownloadState.Queued || p.state is DownloadState.Downloading)
                    p.copy(state = DownloadState.Cancelled)
                else p
            }
        }
    }

    actual fun getDownloadedChapterPath(chapter: KmpChapterModel, mangaTitle: String): String? {
        val dir = File("$rootDir/${mangaTitle.sanitize()}/${chapter.name.sanitize()}")
        return if (dir.exists() && dir.listFiles()?.isNotEmpty() == true) dir.absolutePath else null
    }

    actual fun observeDownloads(): Flow<List<ChapterDownloadProgress>> = _downloads.asStateFlow()

    actual fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String) {
        File("$rootDir/${mangaTitle.sanitize()}/${chapter.name.sanitize()}").deleteRecursively()
    }

    private fun updateState(
        chapterUrl: String,
        transform: (ChapterDownloadProgress) -> ChapterDownloadProgress,
    ) {
        _downloads.update { list -> list.map { if (it.chapterUrl == chapterUrl) transform(it) else it } }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :mangaworld:shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run all tests again to confirm nothing regressed**

```bash
./gradlew :mangaworld:shared:jvmTest
```

Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add mangaworld/shared/src/jvmMain/kotlin/com/programmersbox/manga/shared/downloads/MangaDownloadManager.jvm.kt
git commit -m "feat: add JVM MangaDownloadManager actual (coroutine channel queue)"
```

---

## Task 6: Android Koin registration and `GenericManga` wiring

**Files:**
- Modify: `mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt`

- [ ] **Step 1: Add imports to `GenericManga.kt`**

At the top of `mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt`, add these imports (remove any that are already present):

```kotlin
import com.programmersbox.manga.shared.downloads.DownloadChapterWorker
import com.programmersbox.manga.shared.downloads.MangaDownloadManager
import org.koin.androidx.workmanager.dsl.workerOf
```

- [ ] **Step 2: Register `MangaDownloadManager` and `DownloadChapterWorker` in the Koin module**

In `GenericManga.kt`, find the `appModule` val. Add two lines inside the `module { }` block:

```kotlin
val appModule = module {
    singleOf(::GenericManga) { bindsGenericInfo() }
    single { SystemAlerter(get(), get(), BuildConfig.APPLICATION_ID) }
    singleOf(::NetworkHelper)
    single { NotificationLogo(R.drawable.manga_world_round_logo) }
    singleOf(::ChapterHolder)
    single {
        MangaNewSettingsHandling(
            createProtobuf(
                context = get(),
                serializer = MangaNewSettingsSerializer,
                fileName = "MangaSettings.preferences_pb"
            )
        )
    }
    viewModelOf(::ReadViewModel)
    factoryOf(::DownloadedMediaHandler)
    viewModelOf(::DownloadViewModel)
    factoryOf(::MangaWorldZipper) bind Zipper::class
    singleOf(::MangaDownloadManager)       // ADD THIS
    workerOf(::DownloadChapterWorker)      // ADD THIS
}
```

- [ ] **Step 3: Inject `MangaDownloadManager` into `GenericManga`**

In `GenericManga.kt`, add `mangaDownloadManager: MangaDownloadManager` as a constructor parameter and replace the `downloadChapter` and `downloadFullChapter` functions.

Find the `class GenericManga(` declaration and add the parameter:

```kotlin
class GenericManga(
    val context: Context,
    val chapterHolder: ChapterHolder,
    mangaSettingsHandling: MangaNewSettingsHandling,
    settingsHandling: NewSettingsHandling,
    appConfig: AppConfig,
    navigationActions: NavigationActions,
    private val mangaDownloadManager: MangaDownloadManager,   // ADD
) : GenericSharedManga(
```

- [ ] **Step 4: Delete `downloadFullChapter` and replace `downloadChapter`**

Delete the entire `private fun downloadFullChapter(...)` function (lines 117–152 in the current file).

Replace the `override fun downloadChapter(...)` function with:

```kotlin
override fun downloadChapter(
    model: KmpChapterModel,
    allChapters: List<KmpChapterModel>,
    infoModel: KmpInfoModel,
    navController: NavigationActions,
) {
    mangaDownloadManager.downloadChapter(model, infoModel.title.ifBlank { infoModel.url })
}
```

- [ ] **Step 5: Remove now-unused imports from `GenericManga.kt`**

Delete these imports (no longer needed after removing `downloadFullChapter`):

```kotlin
import android.app.DownloadManager
import android.os.Environment
import com.programmersbox.helpfulutils.downloadManager
import kotlinx.coroutines.GlobalScope
```

- [ ] **Step 6: Build the Android app**

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt
git commit -m "feat: wire MangaDownloadManager into GenericManga, replace DownloadManager"
```

---

## Task 7: Desktop (JVM) wiring

**Files:**
- Modify: `mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericMangaDesktop.kt`

- [ ] **Step 1: Check the Desktop Koin module location**

Run:

```bash
find /Users/jacobrein/StudioProjects/OtakuWorld/mangaworld/desktop -name "*.kt" | xargs grep -l "module\|singleOf\|Koin" 2>/dev/null
```

Find the file containing the Koin `module { }` for the desktop app (likely `main.kt` or a `di/` file).

- [ ] **Step 2: Add `MangaDownloadManager` to the desktop Koin module**

In the desktop Koin module (wherever other singles are registered for Desktop), add:

```kotlin
import com.programmersbox.manga.shared.downloads.MangaDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Inside module { }:
single { MangaDownloadManager(CoroutineScope(Dispatchers.IO + SupervisorJob())) }
```

- [ ] **Step 3: Inject into `GenericMangaDesktop` and wire `downloadChapter`**

Add `mangaDownloadManager: MangaDownloadManager` to `GenericMangaDesktop`'s constructor:

```kotlin
class GenericMangaDesktop(
    val chapterHolder: ChapterHolder,
    settingsHandling: NewSettingsHandling,
    mangaSettingsHandling: MangaNewSettingsHandling,
    appConfig: AppConfig,
    navigationActions: NavigationActions,
    private val desktopSettings: MangaDesktopSettings,
    private val mangaDownloadManager: MangaDownloadManager,  // ADD
) : GenericSharedManga(
```

Replace the empty `downloadChapter` override:

```kotlin
override fun downloadChapter(
    model: KmpChapterModel,
    allChapters: List<KmpChapterModel>,
    infoModel: KmpInfoModel,
    navController: NavigationActions,
) {
    mangaDownloadManager.downloadChapter(model, infoModel.title.ifBlank { infoModel.url })
}
```

- [ ] **Step 4: Build the Desktop app**

```bash
./gradlew :mangaworld:desktop:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add mangaworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericMangaDesktop.kt
git add mangaworld/desktop/  # include any Koin module file modified
git commit -m "feat: wire MangaDownloadManager into Desktop GenericMangaDesktop"
```

---

## Self-Review

### Spec coverage check

| Spec requirement | Task |
|-----------------|------|
| Single class consumed by UI | `expect class MangaDownloadManager` — Task 1 |
| Image ordering (000, 001...) | `writeBytes = { index, bytes -> File(destDir, "%03d.png".format(index))...}` — Tasks 3, 4, 5 |
| Download status tracking | `DownloadState`, `observeDownloads()` — Tasks 1, 4, 5 |
| File system management | `rootDir`, `sanitize()`, `mkdirs()` — Tasks 1, 4, 5 |
| Progress monitoring | `onProgress` callback → `setProgress` / StateFlow — Tasks 3, 4, 5 |
| Chapter deletion | `deleteChapter()` — Tasks 4, 5 |
| Network resilience / retry | `executeDownload` retry loop with exponential backoff — Task 1 |
| Offline reading path | `getDownloadedChapterPath()` — Tasks 4, 5 |
| Batch downloads | `downloadChapters()` — Tasks 4, 5 |
| Android: WorkManager | `DownloadChapterWorker` + `WorkManager.beginUniqueWork` — Tasks 3, 4 |
| JVM: plain coroutines | `Channel` queue + `CoroutineScope` — Task 5 |
| Koin registration | `singleOf`, `workerOf` — Tasks 6, 7 |
| Replace existing `DownloadManager` call | Remove `downloadFullChapter`, inject manager — Task 6 |

All requirements covered. No gaps found.
