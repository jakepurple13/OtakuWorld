# AnimeWorld Downloads Screen Revamp Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild AnimeWorld's downloads screen as option A (clean list): readable rows plus Downloading / Downloaded / Other videos sections, with failed downloads kept and offering Retry and Remove.

**Architecture:**
- Pure naming and formatting helpers live in `commonMain`, with tests.
- `EpisodeDownloadWorker` publishes title tags and byte progress. On failure it keeps its request file and writes a failure marker beside it.
- A new `DownloadQueueSource` (expect/actual) merges live WorkManager state with those markers.
- `VideoLibrarySource` adds size, date, a downloaded-vs-other flag and subtitle labels to each row, and reads off the main thread.
- `ViewVideoScreen` is redrawn on top of these.

**Tech Stack:** Kotlin 2.4 KMP, Compose Multiplatform Material 3, WorkManager, MediaStore, Koin, kotlinx.serialization.

**Spec:** `docs/superpowers/specs/2026-10-16-animeworld-downloads-screen-design.md`. Approved mockup: https://claude.ai/artifact/XZBooCNfQJapBePnR8a4Ms, artboard `Main.dc.html`.

## Global Constraints

- Branch `feat/animeworld-downloads-screen`, stacked on `feat/subtitle-settings-hls-downloads` (#2613).
- Android builds need `ANDROID_HOME=/Users/jacobrein/Library/Android/sdk`. Local flavor: `noFirebase`.
- All new composables live in `:animeworld:shared`.
- Colors are `MaterialTheme.colorScheme` roles (dynamic theming). Strings are hardcoded English.
- Sections, top to bottom: "Downloading · N" (hidden when empty), "Downloaded · N", "Other videos on this phone · N".
- Rows have a 16:9 thumbnail (112×63 dp) with a duration badge and a resume bar. Text lines:
  - line 1: show (max 2 lines);
  - line 2: "Episode N · Name";
  - line 3: "CC" tag plus subtitle labels, then size.
- Other-video rows: line 2 is the date and size.
- The `content://` path is never shown.
- Failed rows stay, survive an app restart, and show the failure reason with **Retry** and **Remove**. Retry re-enqueues the stored request with `ExistingWorkPolicy.REPLACE`. Cancelling a running or waiting download removes the row; it is not a failure.
- Keep: tap to play or cast, swipe start→end to play, swipe end→start to delete (confirm dialog), ⋮ Remove, multi-select delete from the top bar, empty state, sidecar cleanup on delete.
- No file or MediaStore IO on the main thread.
- `.part` files never appear in any section.
- Sort within sections stays `COALESCE(DATE_TAKEN, DATE_ADDED * 1000) DESC`.
- Desktop/iOS must compile. Desktop shows its scanned folder as Downloaded, with no Downloading section.

## Review Focus

1. A video in a sibling folder whose name starts with the download folder's (`/Movies/AnimeWorld2/x.mp4` vs `/Movies/AnimeWorld/`) must land in Other. Test in Task 1.
2. A download folder saved without a trailing slash still matches its own files. Test in Task 1.
3. An HLS download with an unknown total shows "42% · 39 MB", never "of 0 MB". Test in Task 1.
4. A failed download is still listed after the process restarts (marker on disk), and Remove clears it for good. Test in Task 2.
5. Work the system stops (lost network, quota) is neither a failure nor removed: no marker is written and the request file is kept. Test in Task 2.

---

### Task 1: Common naming and format helpers

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/downloads/DownloadsDisplay.kt`
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/downloads/DownloadsDisplayTest.kt`

**Interfaces:**
- Produces:
  - `data class EpisodeTitle(val show: String, val episode: String?)`
  - `fun splitEpisodeTitle(name: String): EpisodeTitle`. Input is a file name without its extension, or a request title.
  - `fun isInFolder(filePath: String?, folder: String): Boolean`
  - `fun formatBytes(bytes: Long): String`
  - `enum class QueuedDownloadState { Running, Waiting, Failed }`
  - `data class QueuedDownload(val key: String, val title: String, val state: QueuedDownloadState, val progress: Float? = null, val bytesDownloaded: Long? = null, val totalBytes: Long? = null, val error: String? = null)`. `key` is the WorkManager unique name (`folder + title`).
  - `fun QueuedDownload.statusLabel(): String`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun splitsShowAndEpisode() = assertEquals(EpisodeTitle("Dragon Ball Daima", "Episode 20 · Maximum"), splitEpisodeTitle("Dragon Ball Daima - Episode 20 - Maximum"))
@Test fun splitsAtEpisodeWhenShowHasDash() = assertEquals(EpisodeTitle("Re - Zero", "Episode 3 · The Whale"), splitEpisodeTitle("Re - Zero - Episode 3 - The Whale"))
@Test fun splitsOnFirstDashWithoutEpisodeWord() = assertEquals(EpisodeTitle("Show", "Special 1"), splitEpisodeTitle("Show - Special 1"))
@Test fun keepsCollisionSuffix() = assertEquals(EpisodeTitle("Show", "Episode 1 · Pilot (2)"), splitEpisodeTitle("Show - Episode 1 - Pilot (2)"))
@Test fun plainNameIsShowOnly() = assertEquals(EpisodeTitle("VID_20261015_102348", null), splitEpisodeTitle("VID_20261015_102348"))
@Test fun episodeWithoutName() = assertEquals(EpisodeTitle("Show", "Episode 4"), splitEpisodeTitle("Show - Episode 4"))

@Test fun inFolder() = assertTrue(isInFolder("/storage/emulated/0/Movies/AnimeWorld/a.ts", "/storage/emulated/0/Movies/AnimeWorld/"))
@Test fun inFolderWithoutTrailingSlash() = assertTrue(isInFolder("/storage/emulated/0/Movies/AnimeWorld/a.ts", "/storage/emulated/0/Movies/AnimeWorld"))
@Test fun siblingFolderIsNotInFolder() = assertFalse(isInFolder("/storage/emulated/0/Movies/AnimeWorld2/a.ts", "/storage/emulated/0/Movies/AnimeWorld/"))
@Test fun nullPathIsNotInFolder() = assertFalse(isInFolder(null, "/storage/emulated/0/Movies/AnimeWorld/"))
@Test fun subfolderIsInFolder() = assertTrue(isInFolder("/storage/emulated/0/Movies/AnimeWorld/Show/a.ts", "/storage/emulated/0/Movies/AnimeWorld/"))

@Test fun bytes() {
    assertEquals("0 B", formatBytes(0)); assertEquals("900 KB", formatBytes(900_000))
    assertEquals("92 MB", formatBytes(92_892_116)); assertEquals("1.2 GB", formatBytes(1_200_000_000))
}
@Test fun runningKnownTotal() = assertEquals("42% · 39 of 92 MB", QueuedDownload("k", "t", QueuedDownloadState.Running, 0.42f, 39_000_000, 92_000_000).statusLabel())
@Test fun runningUnknownTotal() = assertEquals("42% · 39 MB", QueuedDownload("k", "t", QueuedDownloadState.Running, 0.42f, 39_000_000, null).statusLabel())
@Test fun runningNoBytesYet() = assertEquals("0%", QueuedDownload("k", "t", QueuedDownloadState.Running, 0f, null, null).statusLabel())
@Test fun waiting() = assertEquals("Waiting", QueuedDownload("k", "t", QueuedDownloadState.Waiting).statusLabel())
@Test fun failed() = assertEquals("Failed: All files access is needed to save downloads", QueuedDownload("k", "t", QueuedDownloadState.Failed, error = "All files access is needed to save downloads").statusLabel())
@Test fun failedNoReason() = assertEquals("Failed", QueuedDownload("k", "t", QueuedDownloadState.Failed).statusLabel())
```

- [ ] **Step 2: Run** `ANDROID_HOME=/Users/jacobrein/Library/Android/sdk ./gradlew :animeworld:shared:jvmTest --tests "*DownloadsDisplayTest*"`. Expected: FAIL (unresolved references).
- [ ] **Step 3: Implement the helpers in `DownloadsDisplay.kt`.**
  - **Title splitting:** if `" - Episode "` occurs, split at its first occurrence. Otherwise split at the first `" - "`. Otherwise the whole name is the show.
  - **Episode text:** replace the first `" - "` with `" · "`.
  - **`isInFolder`:** normalize the folder to end with `/`, then compare by prefix.
  - **`formatBytes`:** use decimal units (1000). Show one decimal place for GB only, and whole numbers for B, KB and MB.
  - **Running label:** `"<round(progress*100)>%"`, plus `" · <bytes>"` when bytes are known. When the total is known too, render the bytes as `"39 of 92 MB"`, with the unit taken from the total.
- [ ] **Step 4: Run** the same command. Expected: PASS. Then run `:animeworld:shared:testAndroidHostTest` (commonTest runs there too). Expected: PASS.
- [ ] **Step 5: Commit** `feat(animeworld): download title and status helpers`.

### Task 2: Worker publishes progress and keeps failures

**Files:**
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/downloads/DownloadFailures.kt`
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/downloads/EpisodeDownloadWorker.kt`
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/downloads/EpisodeDownloader.kt`. The progress callback changes; update callers in `androidHostTest/.../EpisodeDownloaderTest.kt`.
- Test: `animeworld/shared/src/androidHostTest/kotlin/com/programmersbox/anime/shared/downloads/DownloadFailuresTest.kt`

**Interfaces:**
- Consumes: `writeRequestFile`, `readRequestFile`, `EpisodeDownloadRequest` (existing).
- Produces:
  - `data class DownloadProgress(val fraction: Float, val bytes: Long, val totalBytes: Long?)`. `EpisodeDownloader.download(request, onProgress: (DownloadProgress) -> Unit)`.
    - Progressive downloads: `totalBytes` = content length when > 0.
    - HLS: `totalBytes = null`, and `bytes` = bytes written so far.
  - `@Serializable data class DownloadFailure(val key: String, val title: String, val error: String, val atMs: Long)`
  - `fun writeFailure(requestFile: File, failure: DownloadFailure)`. Writes `<requestFile name without .json>.failed.json` beside it.
  - `fun listFailures(dir: File): List<DownloadFailure>`. Reads every `*.failed.json`. Skips any that are corrupt or have no matching request file. Sorted by `atMs` descending.
  - `fun clearFailure(dir: File, key: String)`. Deletes the marker only.
  - `fun requestFileFor(dir: File, key: String): File`. The same UUID name `writeRequestFile` uses.
  - Constants in `EpisodeDownloadWorker.kt`:
    - `const val DOWNLOAD_TAG = "episodeDownload"`
    - `const val TITLE_TAG_PREFIX = "episodeTitle:"` and `const val KEY_TAG_PREFIX = "episodeKey:"`. Both are added as tags at enqueue.
    - Progress `Data` keys: `"progress"` (Float), `"bytes"` (Long), `"total"` (Long, −1 when unknown).
  - `fun episodeDownloadsDir(context: Context): File` = `File(context.noBackupFilesDir, "episode-downloads")`.
  - `fun enqueueEpisodeDownload(context: Context, request: EpisodeDownloadRequest, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP)`.

- [ ] **Step 1: Write the failing tests** (JUnit, temp dir):

```kotlin
@Test fun failureSurvivesRereadFromDisk()        // write request file + writeFailure → listFailures(dir) in a fresh call returns it with key, title, error
@Test fun clearFailureRemovesOnlyMarker()        // clearFailure → listFailures empty, request file still exists
@Test fun failureWithoutRequestFileIsSkipped()   // marker present, request deleted → listFailures empty
@Test fun corruptMarkerIsSkipped()               // "{" in a .failed.json → listFailures empty, no throw
@Test fun requestFileForMatchesWriteRequestFile()// requestFileFor(dir, key) == writeRequestFile(dir, key, req)
@Test fun newestFailureFirst()                   // atMs 1 and 2 → order [2, 1]
```

- [ ] **Step 2: Run** `ANDROID_HOME=/Users/jacobrein/Library/Android/sdk ./gradlew :animeworld:shared:testAndroidHostTest --tests "*DownloadFailuresTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement `DownloadFailures.kt`.** Then change the worker:
  - Add the tags at enqueue.
  - Publish `bytes` and `total` with `progress`.
  - On a `Throwable` failure, and on the foreground give-up after `MAX_FOREGROUND_ATTEMPTS`, call `writeFailure`. Keep the request file and still post the failure notification.
  - Delete the request file only on success, or when `stopReason == WorkInfo.STOP_REASON_CANCELLED_BY_APP`. Other stops write no marker and keep the file.
  - A missing or unreadable request fails with no marker (there is nothing to retry).
- [ ] **Step 4: Run** `:animeworld:shared:testAndroidHostTest` (whole suite, including `EpisodeDownloaderTest`) and `:animeworld:assembleNoFirebaseDebug`. Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(animeworld): keep failed downloads and publish byte progress`.

### Task 3: DownloadQueueSource

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/downloads/DownloadQueueSource.kt` (the `expect class` and `buildQueue`)
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/downloads/DownloadQueueSource.android.kt`
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/downloads/DownloadQueueSource.jvm.kt` and `animeworld/shared/src/iosMain/kotlin/com/programmersbox/anime/shared/downloads/DownloadQueueSource.ios.kt`. Both: an empty flow and no-op actions; constructors take no arguments.
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt` (appModule: `singleOf(::DownloadQueueSource)`) and `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt` (same).
- Test: `animeworld/shared/src/commonTest/kotlin/com/programmersbox/anime/shared/downloads/BuildQueueTest.kt`

**Interfaces:**
- Consumes: `QueuedDownload` and `QueuedDownloadState` (Task 1); `DownloadFailure`, `listFailures`, `clearFailure`, `requestFileFor`, `readRequestFile`, `enqueueEpisodeDownload(…, policy)`, the tag constants and `episodeDownloadsDir` (Task 2).
- Produces:
  - `fun buildQueue(live: List<QueuedDownload>, failed: List<QueuedDownload>): List<QueuedDownload>`. Order: Running, then Waiting, then Failed. A failed entry whose `key` is also live is dropped.
  - `expect class DownloadQueueSource { fun observe(): Flow<List<QueuedDownload>>; fun cancel(key: String); fun retry(key: String); fun remove(key: String) }`
  - The Android actual takes `(context: Context)`:
    - `observe` = `WorkManager.getWorkInfosByTagFlow(DOWNLOAD_TAG)`. RUNNING maps to Running; ENQUEUED and BLOCKED map to Waiting; other states are ignored. Title and key come from the tags, progress from `progress`.
    - Each emission is combined with a refresh trigger and with `listFailures` read on `Dispatchers.IO`.
    - `cancel` = `cancelUniqueWork(key)`.
    - `retry`: read the request via `requestFileFor`, `clearFailure`, then `enqueueEpisodeDownload(context, request, ExistingWorkPolicy.REPLACE)`.
    - `remove` deletes both marker and request file.
    - `retry` and `remove` do their IO off the main thread and bump the refresh trigger.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun ordersRunningWaitingFailed()       // input failed, waiting, running → states [Running, Waiting, Failed]
@Test fun liveKeyHidesStaleFailure()         // live Running "k" + failed "k" → single Running "k"
@Test fun emptyInputsGiveEmptyQueue()
```

- [ ] **Step 2: Run** `./gradlew :animeworld:shared:jvmTest --tests "*BuildQueueTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** `buildQueue`, the three actuals and both Koin bindings.
- [ ] **Step 4: Run** `:animeworld:shared:jvmTest`, `:animeworld:shared:testAndroidHostTest`, `:animeworld:assembleNoFirebaseDebug` and `:animeworld:desktop:compileKotlinJvm`. Expected: all pass.
- [ ] **Step 5: Commit** `feat(animeworld): download queue source`.

### Task 4: Richer video library rows

**Files:**
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/SharedVideoContent.kt`
- Modify: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videos/VideoLibrarySource.android.kt`
- Modify: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videos/VideoLibrarySource.jvm.kt` and `animeworld/shared/src/iosMain/kotlin/com/programmersbox/anime/shared/videos/SharedVideoContent.ios.kt`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt`. Replace `singleOf(::VideoLibrarySource)` with `single { VideoLibrarySource(get(), downloadFolder = { get<Context>().folderLocation }) }`.

**Interfaces:**
- Consumes: `isInFolder` (Task 1); `OfflineSubtitles.find(File)` (existing, returns subtitles with labels).
- Produces: `SharedVideoContent` gains, all defaulted so existing callers compile:
  - `sizeBytes: Long = 0L`
  - `dateAddedMs: Long = 0L`
  - `isDownload: Boolean = false`
  - `subtitleLabels: List<String> = emptyList()`

  Android `VideoLibrarySource(context: Context, downloadFolder: () -> String)`:
  - Query `SIZE` and `DATE_ADDED` (×1000). `isDownload = isInFolder(DATA, downloadFolder())`.
  - `subtitleLabels` comes from `OfflineSubtitles.find(File(DATA))` for downloads only.
  - Rows whose `DATA` ends with `.part` are dropped.
  - `observeVideos()` runs its queries with `.flowOn(Dispatchers.IO)`. Observer changes re-query through the flow, not on the main looper.
  - The JVM actual sets `isDownload = true`, `sizeBytes = file.length()` and `dateAddedMs = file.lastModified()`.

- [ ] **Step 1:** No new pure logic (classification is Task 1's tested `isInFolder`). Run `:animeworld:assembleNoFirebaseDebug` and `:animeworld:desktop:compileKotlinJvm` after the change.
- [ ] **Step 2: Implement** the changes above.
- [ ] **Step 3: Run** both builds and `:animeworld:shared:testAndroidHostTest`. Expected: PASS.
- [ ] **Step 4: Commit** `feat(animeworld): size, date, download flag and subtitle labels for library rows`.

### Task 5: Downloads screen UI (option A)

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/DownloadRows.kt`
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/ViewVideoScreen.kt`

**Interfaces:**
- Consumes:
  - `splitEpisodeTitle`, `formatBytes`, `statusLabel`, `QueuedDownload` (Task 1)
  - `DownloadQueueSource` via `koinInject()` (Task 3)
  - the new `SharedVideoContent` fields (Task 4)
  - existing `VideoThumbnail`, `BottomSheetDeleteScaffold`, swipe, delete-dialog and cast parameters.
- Produces:
  - `@Composable fun SectionHeader(text: String, modifier: Modifier = Modifier)`
  - `@Composable fun VideoRow(item: SharedVideoContent, onClick: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun QueuedDownloadRow(item: QueuedDownload, onCancel: () -> Unit, onRetry: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier)`

Layout (structure from `Main.dc.html`):
- Top bar title "Downloads".
- `LazyColumn` items:
  - "Downloading · N" header and queue rows, only when the queue is non-empty;
  - "Downloaded · N" header and rows;
  - "Other videos on this phone · N" header, which toggles expanded. It starts collapsed (`rememberSaveable`) and shows a chevron.
- `VideoRow`:
  - 112×63 dp thumbnail, 12 dp corners, duration badge bottom-end, 3 dp resume bar at the bottom when `lastPlayedPositionMs > 0`.
  - Downloads: line 1 is the show (`titleMedium`, max 2 lines). Line 2 is the episode (`bodyMedium`, `onSurfaceVariant`, one line). Line 3 is an outlined "CC" tag when `subtitleLabels` is non-empty, then `"<labels joined ', '> · <size>"`.
  - Other videos: line 1 is the name without extension, line 2 is the `dateAddedMs` date plus size.
  - Trailing ⋮ with Remove.
- `QueuedDownloadRow`:
  - Placeholder thumbnail block (`surfaceContainerHighest`), split title, `LinearProgressIndicator` (Running only), `statusLabel()`.
  - Trailing: Cancel (X) for Running/Waiting, or **Retry** and **Remove** buttons for Failed.
- Swipe-to-play, swipe-to-delete and the delete dialog wrap `VideoRow` as today. The multi-select sheet still lists all library items.
- The empty state shows only when both the queue and the library are empty.

- [ ] **Step 1: Implement** the composables and the screen.
- [ ] **Step 2: Run** `:animeworld:assembleNoFirebaseDebug` and `:animeworld:desktop:compileKotlinJvm`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat(animeworld): downloads screen redesign`.

### Task 6: Device verification (controller)

On the Android test phone, with the build from Task 5 installed:
- [ ] Downloaded Daima rows read "Dragon Ball Daima" / "Episode N · Name" / CC labels and size. Camera clips sit under the collapsed "Other videos on this phone".
- [ ] Start a download: a Downloading row shows progress and bytes. Cancel removes the row and leaves no files.
- [ ] Force a failure (revoke All files access with `appops set … MANAGE_EXTERNAL_STORAGE default`, then download): a Failed row with the reason appears.
- [ ] Force-stop the app and reopen it: the row is still there.
- [ ] Re-grant access and tap Retry: the download completes and moves to Downloaded.
- [ ] Fail again, then Remove: the row is gone and stays gone after a restart.
- [ ] Swipe to play, swipe to delete (sidecars removed), ⋮ Remove, multi-select delete, and playing from a row all work.
