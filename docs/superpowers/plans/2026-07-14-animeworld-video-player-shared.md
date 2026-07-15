# AnimeWorld Video List/Player/Choice → Shared Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move AnimeWorld's video library screen, video player, and quality-choice screen into `:animeworld:shared`, with an `expect`/`actual` seam for the player, so `:animeworld:desktop` can eventually get real playback (the user implements the JVM `actual` themselves).

**Architecture:** `VideoScreen`/`StorageHolder`/`VideoSourceModel` become plain shared data. `chapterOnClick` becomes a concrete method on `GenericSharedAnime` (fetch → decide → either call the new abstract `playOrCast` or show the shared `VideoChoiceScreen`). The video player becomes an `expect @Composable fun VideoPlayerUi(videoScreen: VideoScreen)`, with the existing ExoPlayer code relocated near-verbatim into `animeworld/shared/src/androidMain` as the `actual`, and a stub `actual` in `jvmMain` (today's `VideoNotSupportedScreen` content) for the user to replace. The video library screen gets a new platform-neutral `SharedVideoContent` model and an `expect class VideoLibrarySource`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin, Navigation3, ExoPlayer/media3 (androidMain only).

## Global Constraints

- Do not modify `:mangaworld:shared`/`:mangaworld:desktop`, or `:novelworld:shared`/`:novelworld:desktop`.
- No unit tests required.
- No casting (Chromecast) code, UI, or dependency added to `:animeworld:shared` or `:animeworld:desktop`. Every place shared code needs cast-awareness, it takes a callback parameter (`isCastActive: () -> Boolean`, `onCastLoad: (KmpStorage) -> Unit`) that Android supplies with real logic and Desktop supplies as a no-op.
- Desktop downloads stay "automatic best-available quality" — `VideoChoiceScreen`'s download (non-streaming) branch stays Android-only, not extended to desktop.
- The JVM `actual VideoPlayerUi` is a stub only (today's `VideoNotSupportedScreen` content) — the user implements the real desktop player themselves in a later, separate effort.
- Follow existing code style exactly; when relocating a file, change only the package declaration and what's explicitly called out in that task's steps — do not reformat or "improve" logic while moving it.

---

### Task 1: Shared nav/state primitives — `VideoScreen`, `StorageHolder`, `VideoSourceModel`

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/VideoScreen.kt`
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/StorageHolder.kt`
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videochoice/VideoSourceModel.kt`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/videoplayer/VideoViewModel.kt` (remove `VideoScreen` + its `navigateToVideoPlayer` companion function — both move out)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt` (remove the `StorageHolder` class definition; update the import)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/videochoice/VideoSourceViewModel.kt` (delete — content moves to the new shared file)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/AnimeUtils.kt` (remove the old `navigateToVideoPlayer` `Context` extension — replaced by direct `NavigationActions.navigate(VideoScreen(...))` calls at each call site in later tasks)

**Interfaces:**
- Produces: `data class VideoScreen(showPath: String, showName: String, downloadOrStream: Boolean, referer: String) : NavKey` (package `com.programmersbox.anime.shared`). `class StorageHolder { var storageModel: KmpStorage? }` (same package). `data class VideoSourceModel(c: List<KmpStorage>, infoModel: KmpInfoModel, isStreaming: Boolean, model: KmpChapterModel)` with `companion object { var showVideoSources: VideoSourceModel? }` (package `com.programmersbox.anime.shared.videochoice`).

- [ ] **Step 1: Create `VideoScreen.kt`**

Create `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/VideoScreen.kt`:

```kotlin
package com.programmersbox.anime.shared

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class VideoScreen(
    val showPath: String,
    val showName: String,
    val downloadOrStream: Boolean,
    val referer: String,
) : NavKey
```

- [ ] **Step 2: Create `StorageHolder.kt`**

Create `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/StorageHolder.kt`:

```kotlin
package com.programmersbox.anime.shared

import com.programmersbox.kmpmodels.KmpStorage

class StorageHolder {
    var storageModel: KmpStorage? = null
}
```

- [ ] **Step 3: Create `VideoSourceModel.kt`**

Create `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videochoice/VideoSourceModel.kt`:

```kotlin
package com.programmersbox.anime.shared.videochoice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage

data class VideoSourceModel(
    val c: List<KmpStorage>,
    val infoModel: KmpInfoModel,
    val isStreaming: Boolean,
    val model: KmpChapterModel,
) {
    companion object {
        var showVideoSources by mutableStateOf<VideoSourceModel?>(null)
    }
}
```

- [ ] **Step 4: Delete the old `VideoSourceViewModel.kt`**

Delete `animeworld/src/main/java/com/programmersbox/animeworld/videochoice/VideoSourceViewModel.kt` (its content is now `VideoSourceModel.kt` above).

- [ ] **Step 5: Remove `VideoScreen`/`navigateToVideoPlayer` from `VideoViewModel.kt`**

In `animeworld/src/main/java/com/programmersbox/animeworld/videoplayer/VideoViewModel.kt`, remove the `@Serializable data class VideoScreen(...) : NavKey` block and the `companion object { const val VideoPlayerRoute = ...; fun navigateToVideoPlayer(...) { ... } }` block from `VideoViewModel`'s companion object (keep the rest of the class as-is — the `VideoViewModel` class itself, minus that nav-related companion content, is handled in Task 5). Add the import `import com.programmersbox.anime.shared.VideoScreen` where `VideoScreen` is still referenced in this file (the `VideoViewModel` constructor param `videoScreen: VideoScreen` and its usages).

- [ ] **Step 6: Remove `StorageHolder` from `GenericAnime.kt`, add the shared import**

In `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt`, delete the `class StorageHolder { var storageModel: KmpStorage? = null }` block. Add `import com.programmersbox.anime.shared.StorageHolder` alongside the other imports.

- [ ] **Step 7: Remove the old `navigateToVideoPlayer` `Context` extension from `AnimeUtils.kt`**

In `animeworld/src/main/java/com/programmersbox/animeworld/AnimeUtils.kt`, delete the `fun Context.navigateToVideoPlayer(...)` extension function entirely (the block shown in the earlier read, roughly lines 90-117, that branches on a feature flag between `VideoViewModel.navigateToVideoPlayer(...)` and starting `VideoPlayerActivity` via `Intent`). Callers are updated in later tasks to call `navigationActions.navigate(VideoScreen(...))` directly.

- [ ] **Step 8: Build to verify**

Run: `./gradlew :animeworld:shared:compileKotlinJvm` (verifies the 3 new shared files compile) — expect BUILD SUCCESSFUL. This step alone won't catch the now-broken references in `animeworld` (Android) from Steps 5-7 — those are expected to be temporarily broken until Tasks 2-6 finish updating their call sites. Do not attempt `:animeworld:assembleNoFirebaseDebug` yet; it will fail until Task 6 is complete. Note this in your report so the reviewer knows it's expected.

- [ ] **Step 9: Commit**

```bash
git add animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/VideoScreen.kt \
        animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/StorageHolder.kt \
        animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videochoice/VideoSourceModel.kt \
        animeworld/src/main/java/com/programmersbox/animeworld/videoplayer/VideoViewModel.kt \
        animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt \
        animeworld/src/main/java/com/programmersbox/animeworld/AnimeUtils.kt
git rm animeworld/src/main/java/com/programmersbox/animeworld/videochoice/VideoSourceViewModel.kt
git commit -m "refactor(animeworld): move VideoScreen/StorageHolder/VideoSourceModel to shared

Pure data/state moved to :animeworld:shared commonMain so both Android
and Desktop can share the same nav route and quality-choice state.
animeworld app module compilation is intentionally broken until Task 6
(the rest of the video-player migration) lands — VideoChoiceScreen,
ViewVideoScreen, and GenericAnime's chapterOnClick still reference the
old locations and are fixed in the following tasks."
```

---

### Task 2: Shared video-library model and data source — `SharedVideoContent` / `VideoLibrarySource`

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/SharedVideoContent.kt`
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videos/VideoLibrarySource.android.kt`
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videos/VideoLibrarySource.jvm.kt`

**Interfaces:**
- Consumes: `AnimeDesktopSettings` (jvmMain, `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/AnimeDesktopSettings.kt`, has `val downloadsDirectory: DataStoreHandler<String>`).
- Produces: `data class SharedVideoContent(videoName: String, path: String, duration: Long, lastPlayedPositionMs: Long)`. `expect class VideoLibrarySource { fun observeVideos(): Flow<List<SharedVideoContent>>; fun getResumePosition(path: String): Long; fun setResumePosition(path: String, positionMs: Long); fun delete(content: SharedVideoContent) }`.

- [ ] **Step 1: Create the shared model**

Create `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/SharedVideoContent.kt`:

```kotlin
package com.programmersbox.anime.shared.videos

data class SharedVideoContent(
    val videoName: String,
    val path: String,
    val duration: Long,
    val lastPlayedPositionMs: Long,
)
```

- [ ] **Step 2: Declare the expect class**

Add to the same file (`SharedVideoContent.kt`):

```kotlin
import kotlinx.coroutines.flow.Flow

expect class VideoLibrarySource {
    fun observeVideos(): Flow<List<SharedVideoContent>>
    fun getResumePosition(path: String): Long
    fun setResumePosition(path: String, positionMs: Long)
    fun delete(content: SharedVideoContent)
}
```

(Add the `Flow` import at the top with the others; final file has both the data class and the expect class.)

- [ ] **Step 3: Create the Android actual**

Create `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videos/VideoLibrarySource.android.kt`:

```kotlin
package com.programmersbox.anime.shared.videos

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.MediaStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private val externalContentUri: Uri
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

actual class VideoLibrarySource(private val context: Context) {

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("videos", Context.MODE_PRIVATE)

    private fun queryVideos(): List<SharedVideoContent> {
        val projections = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media._ID,
        )
        val results = mutableListOf<SharedVideoContent>()
        context.contentResolver.query(
            externalContentUri,
            projections,
            null,
            null,
            "LOWER (${MediaStore.Video.Media.DATE_TAKEN}) DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    val contentUri = ContentUris.withAppendedId(externalContentUri, id)
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    results.add(
                        SharedVideoContent(
                            videoName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)).orEmpty(),
                            path = contentUri.toString(),
                            duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
                            lastPlayedPositionMs = prefs.getLong(path.orEmpty(), 0L),
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return results
    }

    actual fun observeVideos(): Flow<List<SharedVideoContent>> = callbackFlow {
        trySend(queryVideos())
        val observer = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryVideos())
            }
        }
        context.contentResolver.registerContentObserver(externalContentUri, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

    actual fun getResumePosition(path: String): Long = prefs.getLong(path, 0L)

    actual fun setResumePosition(path: String, positionMs: Long) {
        prefs.edit().putLong(path, positionMs).apply()
    }

    actual fun delete(content: SharedVideoContent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                content.path.toUri()?.let {
                    context.contentResolver.delete(it, null, null)
                }
            } else {
                java.io.File(content.path).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun String.toUri(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
```

- [ ] **Step 4: Create the JVM actual**

Create `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videos/VideoLibrarySource.jvm.kt`:

```kotlin
package com.programmersbox.anime.shared.videos

import com.programmersbox.anime.shared.AnimeDesktopSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.prefs.Preferences

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm")

actual class VideoLibrarySource(private val animeDesktopSettings: AnimeDesktopSettings) {

    private val resumePositions = Preferences.userNodeForPackage(VideoLibrarySource::class.java)

    private fun scanVideos(): List<SharedVideoContent> {
        val root = File(animeDesktopSettings.downloadsDirectory.get())
        if (!root.exists()) return emptyList()
        return root
            .walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in VIDEO_EXTENSIONS }
            .map {
                SharedVideoContent(
                    videoName = it.nameWithoutExtension,
                    path = it.absolutePath,
                    duration = 0L,
                    lastPlayedPositionMs = resumePositions.getLong(it.absolutePath, 0L),
                )
            }
            .toList()
    }

    actual fun observeVideos(): Flow<List<SharedVideoContent>> =
        animeDesktopSettings.downloadsDirectory.asFlow().map { scanVideos() }

    actual fun getResumePosition(path: String): Long = resumePositions.getLong(path, 0L)

    actual fun setResumePosition(path: String, positionMs: Long) {
        resumePositions.putLong(path, positionMs)
    }

    actual fun delete(content: SharedVideoContent) {
        File(content.path).delete()
    }
}
```

- [ ] **Step 5: Build to verify**

Run: `./gradlew :animeworld:shared:compileKotlinJvm` — expect BUILD SUCCESSFUL (verifies the jvmMain actual and the commonMain expect/model). Then run `./gradlew :animeworld:shared:compileDebugKotlinAndroid` (or the equivalent Android-target compile task for this module — check the exact task name with `./gradlew :animeworld:shared:tasks --all | grep -i compile` if this exact name doesn't match) — expect BUILD SUCCESSFUL for the androidMain actual too.

- [ ] **Step 6: Commit**

```bash
git add animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos \
        animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videos \
        animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videos
git commit -m "feat(animeworld): add shared video-library model and expect/actual data source

SharedVideoContent replaces the Android-MediaStore-shaped VideoContent
for the video library screen. Android's actual wraps MediaStore
(unchanged behavior); the new JVM actual scans AnimeDesktopSettings'
downloads directory for video files (no resume-position tracking
existed there before, now backed by java.util.prefs)."
```

---

### Task 3: Video library screen — move `ViewVideoScreen` to shared, adapt to `SharedVideoContent`

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/ViewVideoScreen.kt`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt` (remove `VideoViewerRoute`/`ViewVideoScreen` nav entry wiring reference — replaced with the shared route in Task 6; remove the import)
- Delete: `animeworld/src/main/java/com/programmersbox/animeworld/videos/ViewVideosFragment.kt`
- Delete: `animeworld/src/main/java/com/programmersbox/animeworld/videos/ViewVideoViewModel.kt`

**Interfaces:**
- Consumes: `SharedVideoContent`, `VideoLibrarySource` (Task 2). `VideoScreen` (Task 1). `BottomSheetDeleteScaffold`, `ImageFlushListItem`, `BackButton`, `LocalNavHostPadding`, `PermissionRequest` — all existing shared `kmpuiviews` composables already used by the original.
- Produces: `data object VideoViewerRoute : NavKey` and `@Composable fun ViewVideoScreen(isCastActive: () -> Boolean = { false }, onCastLoad: (SharedVideoContent) -> Unit = {})` in commonMain.

- [ ] **Step 1: Create the shared `ViewVideoScreen.kt`**

Create `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/ViewVideoScreen.kt`. This is an adaptation of the existing `ViewVideosFragment.kt` (already read in full during design) with these exact changes:

- Package becomes `com.programmersbox.anime.shared.videos`.
- Drop the `@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")` line (Android-toolchain-specific suppression, not needed here).
- Replace the top-level `data object VideoViewerRoute : NavKey` declaration (currently in `ViewVideoViewModel.kt`) — bring it into this file instead.
- Replace `ViewVideoViewModel` construction (`viewModel { ViewVideoViewModel(context) }`) with direct use of `VideoLibrarySource` injected via Koin (`koinInject<VideoLibrarySource>()`) and `.observeVideos().collectAsStateWithLifecycle(emptyList())` for the video list (no separate ViewModel class needed — the old `ViewVideoViewModel` was a thin wrapper around `VideoGet`, which no longer exists here).
- Replace every reference to `VideoContent` with `SharedVideoContent`, and `item.videoDuration`/`item.assetFileStringUri`/`item.path`/`item.videoName` with `item.duration`/`item.path`/`item.path`/`item.videoName` respectively (there is no longer a separate `assetFileStringUri` vs `path` distinction — `SharedVideoContent.path` is used for both display and playback).
- Replace `context.getSharedPreferences("videos", ...).getLong(item.path, 0)` / `.putLong(...)` reads with `item.lastPlayedPositionMs` (already populated by `VideoLibrarySource`) for display, and drop the direct `SharedPreferences` writes (resume-position writing happens in the player, Task 5, via `VideoLibrarySource.setResumePosition`).
- Replace the `AndroidView { MediaRouteButton(...) }` cast button in the `TopAppBar` actions with: `if (isCastActive != null) { /* cast button slot, only rendered when a real check is supplied */ }` — concretely, add a new parameter `castButton: @Composable () -> Unit = {}` to `ViewVideoScreen`/`VideoLoad` and call it in place of the `AndroidView` block instead of trying to make the cast button itself portable. Android supplies a lambda rendering the real `MediaRouteButton` `AndroidView`; Desktop supplies `{}` (nothing).
- Replace `MainActivity.cast.isCastActive()` / `MainActivity.cast.loadMedia(...)` calls (in `VideoContentView`'s swipe-to-play and click actions) with the new `isCastActive: () -> Boolean` and `onCastLoad: (SharedVideoContent) -> Unit` parameters threaded down from `ViewVideoScreen` into `VideoContentView`.
- Replace `context.navigateToVideoPlayer(navController, item.assetFileStringUri, item.videoName, true, "")` calls with `navController.navigate(VideoScreen(showPath = item.path, showName = item.videoName, downloadOrStream = true, referer = ""))` directly (import `com.programmersbox.anime.shared.VideoScreen`).
- Replace the delete actions (`context.contentResolver.delete(...)` / `File(it.path!!).delete()`) with `koinInject<VideoLibrarySource>().delete(it)`.
- `SlideToDeleteDialog` — this composable is defined in the Android app (`animeworld/src/main/java/com/programmersbox/animeworld/SlideToDeleteDialog` referenced from `AnimeUtils.kt`'s `SlideTo`/dialog helpers seen during design). Check whether it has any Android-only dependency; if it's pure Compose (no `Context`/Android API usage beyond what's already portable), move it into this same file as a private composable taking `content: SharedVideoContent` and `onConfirm: () -> Unit` instead of `VideoContent`. If it does have real Android dependencies, leave it in the Android app and pass a `deleteDialog: @Composable (SharedVideoContent, (Boolean) -> Unit) -> Unit = { _, _ -> }`-shaped slot parameter instead, following the same pattern as `castButton` above.
- Everything else (the `BottomSheetDeleteScaffold` usage, `EmptyState`, thumbnail loading via Coil, swipe gestures, layout) carries over unchanged.

Add the function signature:

```kotlin
@Composable
fun ViewVideoScreen(
    isCastActive: () -> Boolean = { false },
    onCastLoad: (SharedVideoContent) -> Unit = {},
    castButton: @Composable () -> Unit = {},
)
```

- [ ] **Step 2: Delete the old Android files**

```bash
git rm animeworld/src/main/java/com/programmersbox/animeworld/videos/ViewVideosFragment.kt
git rm animeworld/src/main/java/com/programmersbox/animeworld/videos/ViewVideoViewModel.kt
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew :animeworld:shared:compileKotlinJvm` and (the Android-target compile task identified in Task 2 Step 5) — both must succeed for the new shared file. Do not attempt `:animeworld:assembleNoFirebaseDebug` yet (still broken pending Task 6).

- [ ] **Step 4: Commit**

```bash
git add animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videos/ViewVideoScreen.kt
git commit -m "refactor(animeworld): move video library screen to shared, adapt to SharedVideoContent

ViewVideoScreen moves to :animeworld:shared, sourcing its list from the
new VideoLibrarySource instead of a ViewModel wrapping Android's
MediaStore directly. Chromecast button and delete-confirmation dialog
(if Android-only) are passed in as composable slot parameters rather
than hardcoded, so Desktop can supply no-ops."
```

---

### Task 4: Video choice screen — move `VideoChoiceScreen` to shared

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videochoice/VideoChoiceScreen.kt`
- Delete: `animeworld/src/main/java/com/programmersbox/animeworld/videochoice/VideoChoiceScreen.kt`

**Interfaces:**
- Consumes: `VideoSourceModel` (Task 1), `KmpGenericInfo.downloadChapter` (existing interface method), `ListBottomScreen`/`ListBottomSheetItemModel` (existing shared `kmpuiviews` components).
- Produces: `@Composable fun VideoChoiceScreen(items: List<KmpStorage>, infoModel: KmpInfoModel, isStreaming: Boolean, model: KmpChapterModel, genericInfo: KmpGenericInfo, navController: NavigationActions, isCastActive: () -> Boolean = { false }, onCastLoad: (KmpStorage) -> Unit = {})`.

- [ ] **Step 1: Create the shared `VideoChoiceScreen.kt`**

Create `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videochoice/VideoChoiceScreen.kt`:

```kotlin
package com.programmersbox.anime.shared.videochoice

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled._360
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled._10mp
import androidx.compose.material.icons.filled._1k
import androidx.compose.material.icons.filled._4k
import androidx.compose.material.icons.filled._4mp
import androidx.compose.material.icons.filled._7mp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions

enum class Qualities(var value: Int) {
    Unknown(0),
    P360(-2), // 360p
    P480(-1), // 480p
    P720(1), // 720p
    P1080(2), // 1080p
    P1440(3), // 1440p
    P2160(4) // 4k or 2160p
}

fun getQualityFromName(qualityName: String): Qualities {
    return when (qualityName.replace("p", "").replace("P", "")) {
        "360" -> Qualities.P360
        "480" -> Qualities.P480
        "720" -> Qualities.P720
        "1080" -> Qualities.P1080
        "1440" -> Qualities.P1440
        "2160" -> Qualities.P2160
        "4k" -> Qualities.P2160
        "4K" -> Qualities.P2160
        else -> Qualities.Unknown
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoChoiceScreen(
    items: List<KmpStorage>,
    infoModel: KmpInfoModel,
    isStreaming: Boolean,
    model: KmpChapterModel,
    genericInfo: KmpGenericInfo,
    navController: NavigationActions,
    isCastActive: () -> Boolean = { false },
    onCastLoad: (KmpStorage) -> Unit = {},
) {
    val onAction: (KmpStorage) -> Unit = {
        VideoSourceModel.showVideoSources = null
        if (isStreaming) {
            if (isCastActive()) {
                onCastLoad(it)
            } else {
                navController.navigate(
                    VideoScreen(
                        showPath = it.link.orEmpty(),
                        showName = model.name,
                        downloadOrStream = false,
                        referer = it.headers["referer"] ?: it.source.orEmpty()
                    )
                )
            }
        } else {
            genericInfo.downloadChapter(model, listOf(model), infoModel, navController)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { VideoSourceModel.showVideoSources = null },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ListBottomScreen(
            includeInsetPadding = false,
            title = "Choose quality for ${model.name}",
            list = items,
            onClick = { onAction(it) }
        ) {
            ListBottomSheetItemModel(
                primaryText = it.quality.orEmpty(),
                icon = when (getQualityFromName(it.quality.orEmpty())) {
                    Qualities.Unknown -> Icons.Default.DeviceUnknown
                    Qualities.P360 -> Icons.AutoMirrored.Filled._360
                    Qualities.P480 -> Icons.Default._4mp
                    Qualities.P720 -> Icons.Default._7mp
                    Qualities.P1080 -> Icons.Default._10mp
                    Qualities.P1440 -> Icons.Default._1k
                    Qualities.P2160 -> Icons.Default._4k
                }
            )
        }
    }
}
```

Note the title string is now a plain literal (`"Choose quality for ${model.name}"`) instead of `stringResource(R.string.choose_quality_for, model.name)` — the original Android string resource lives in the `animeworld` app module's `strings.xml`, not reachable from `:animeworld:shared`'s commonMain. This is a deliberate, minimal behavior change (same text, no longer localized) accepted for this move; do not attempt to relocate the Android string resource system into the shared module.

`Qualities`/`getQualityFromName` above are copied verbatim from `animeworld/src/main/java/com/programmersbox/animeworld/AnimeUtils.kt:443-465` — same enum values, same string-matching logic, no changes.

- [ ] **Step 2: Delete the old Android files**

```bash
git rm animeworld/src/main/java/com/programmersbox/animeworld/videochoice/VideoChoiceScreen.kt
```

Also remove `Qualities`/`getQualityFromName` from `animeworld/src/main/java/com/programmersbox/animeworld/AnimeUtils.kt` (they moved to the new shared file) — but check first whether anything else in the Android app module still references `Qualities`/`getQualityFromName` besides the old `VideoChoiceScreen.kt` (grep for both names across `animeworld/src/main/java`); if something else uses them, update that call site to import from `com.programmersbox.anime.shared.videochoice` instead of also deleting them from `AnimeUtils.kt`.

- [ ] **Step 3: Build to verify**

Run: `./gradlew :animeworld:shared:compileKotlinJvm` — expect BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videochoice/VideoChoiceScreen.kt \
        animeworld/src/main/java/com/programmersbox/animeworld/AnimeUtils.kt
git commit -m "refactor(animeworld): move video quality-choice screen to shared

Casting is parameterized (isCastActive/onCastLoad) instead of reaching
into MainActivity.cast directly. Download action now goes through the
existing KmpGenericInfo.downloadChapter interface method instead of an
unsafe cast to the Android-only GenericAnime class."
```

---

### Task 5: Video player — `expect VideoPlayerUi`, Android actual (relocated ExoPlayer code), JVM stub actual

**Files:**
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerUi.kt`
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerCompose.android.kt`
- Create: `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoViewModel.kt`
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerUi.jvm.kt`
- Modify: `animeworld/shared/build.gradle.kts` (add `androidMain` dependencies for media3/ExoPlayer)
- Delete: `animeworld/src/main/java/com/programmersbox/animeworld/videoplayer/VideoPlayerCompose.kt`
- Delete: `animeworld/src/main/java/com/programmersbox/animeworld/videoplayer/VideoViewModel.kt`

**Interfaces:**
- Consumes: `VideoScreen`, `StorageHolder` (Task 1).
- Produces: `expect @Composable fun VideoPlayerUi(videoScreen: VideoScreen)` in commonMain.

- [ ] **Step 1: Add media3/ExoPlayer dependencies to `animeworld/shared/build.gradle.kts`**

In `animeworld/shared/build.gradle.kts`, add an `androidMain.dependencies { ... }` block (there isn't one yet — add it as a sibling to the existing `commonMain.dependencies`/`jvmMain.dependencies` blocks inside `sourceSets { }`):

```kotlin
        androidMain.dependencies {
            implementation(androidLibs.bundles.media3)
        }
```

Check `gradle/android.versions.toml` (or wherever `androidLibs.bundles.media3` is defined — it's already used by the existing `animeworld/build.gradle.kts`, confirm the exact accessor name matches) before assuming this exact name; use whatever the existing Android app module's `build.gradle.kts` uses for its media3 bundle.

- [ ] **Step 2: Declare the expect function**

Create `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerUi.kt`:

```kotlin
package com.programmersbox.anime.shared.videoplayer

import androidx.compose.runtime.Composable
import com.programmersbox.anime.shared.VideoScreen

@Composable
expect fun VideoPlayerUi(videoScreen: VideoScreen)
```

- [ ] **Step 3: Create the Android actual — relocate `VideoViewModel.kt`**

Create `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoViewModel.kt`. Read the current
`animeworld/src/main/java/com/programmersbox/animeworld/videoplayer/VideoViewModel.kt` (after Task 1 Step 5 already removed its `VideoScreen`/`navigateToVideoPlayer` content) and copy the remainder here verbatim, with these exact changes:
- Package becomes `com.programmersbox.anime.shared.videoplayer`.
- `import com.programmersbox.animeworld.StorageHolder` becomes `import com.programmersbox.anime.shared.StorageHolder`.
- `import com.programmersbox.animeworld.videoplayer.VideoScreen` (or wherever the compiler now resolves it from after Task 1) becomes `import com.programmersbox.anime.shared.VideoScreen`.
- Everything else (the `VideoViewModel` class itself with all its ExoPlayer/battery/state fields, `ExoPlayerAttributes`, `VideoPlayerVisibility`, `VideoInfo`, `SSLTrustManager`, `Context.getMediaSource`/`getDataSourceFactory`/`getHttpDataSourceFactory`) is unchanged.

- [ ] **Step 4: Create the Android actual — relocate `VideoPlayerCompose.kt`, wrap as `actual`**

Create `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerCompose.android.kt`. Read the current
`animeworld/src/main/java/com/programmersbox/animeworld/videoplayer/VideoPlayerCompose.kt` and copy it here verbatim, with these exact changes:
- Package becomes `com.programmersbox.anime.shared.videoplayer`.
- Change the top-level `@Composable fun VideoPlayerUi(screen: VideoScreen, ...)` function signature to match and implement the `expect` declared in Step 2: rename its `screen` parameter to `videoScreen` (matching the expect signature exactly) and add the `actual` modifier: `@Composable actual fun VideoPlayerUi(videoScreen: VideoScreen)`. Inside the function body, replace remaining uses of `screen` with `videoScreen`, and change the default-parameter-based dependency injection (`context: Context = LocalContext.current, genericInfo: GenericInfo = koinInject(), storageHolder: StorageHolder = koinInject(), viewModel: VideoViewModel = viewModel { VideoViewModel(screen, context, storageHolder) }`) to local `val` declarations inside the function body instead (since `actual` implementations of an `expect` function cannot add extra default-valued parameters not present on the `expect` signature):

```kotlin
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
actual fun VideoPlayerUi(videoScreen: VideoScreen) {
    val context = LocalContext.current
    val genericInfo = koinInject<GenericInfo>()
    val storageHolder = koinInject<StorageHolder>()
    val viewModel: VideoViewModel = viewModel { VideoViewModel(videoScreen, context, storageHolder) }
    val activity = LocalActivity.current
    // ...rest of the original body, unchanged except `screen` -> `videoScreen` where it was referenced
}
```

- `import com.programmersbox.animeworld.StorageHolder` becomes `import com.programmersbox.anime.shared.StorageHolder`.
- `import com.programmersbox.animeworld.composables.AirBar` — check if `AirBar.kt` (`animeworld/src/main/java/com/programmersbox/animeworld/composables/AirBar.kt`) has any Android-specific dependency; if it's pure Compose (no `Context`/Android API), also move it into
  `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videoplayer/AirBar.kt` (commonMain, since a brightness/volume slider widget is plausibly pure UI) and update the import to `com.programmersbox.anime.shared.videoplayer.AirBar`; if it has real Android dependencies, move it alongside this file into `animeworld/shared/src/androidMain` instead and keep the import path as `com.programmersbox.anime.shared.videoplayer.AirBar` either way (just androidMain instead of commonMain).
- `import com.programmersbox.animeworld.ignoreSsl` — this is `Context.ignoreSsl` from `AnimeUtils.kt`, backed by `otakuDataStore` (an Android-only DataStore instance). Leave this as an Android-only concept: keep the import as-is if `ignoreSsl`'s definition doesn't move (it's fine for this androidMain file to import from the Android app module's package, since `animeworld` (the app) already depends on `:animeworld:shared`, and Gradle allows a KMP module's androidMain to depend on the consuming Android app module *only if that dependency direction is declared* — check whether `animeworld/shared/build.gradle.kts`'s androidMain can see `com.programmersbox.animeworld.ignoreSsl` at all; if not (most likely, since `:animeworld:shared` does NOT depend on the `animeworld` app module — dependencies only flow the other direction), this will not compile. In that case: move `Context.ignoreSsl` (and the two-line `DataStoreHandler` definition of `IGNORE_SSL` it reads from, in `AnimeUtils.kt` around line 74-95) into `animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerCompose.android.kt` itself (as a private top-of-file declaration) or a small new file in the same package, and delete the original from `AnimeUtils.kt`, updating any other Android-app call sites that referenced it (grep `ignoreSsl` across `animeworld/src/main/java` to find them, e.g. `GenericAnime.kt`'s player settings toggle).
- `import com.programmersbox.uiviews.GenericInfo` — this Android-only interface (`UIViews` module) is fine to keep importing here since `:animeworld:shared`'s androidMain CAN depend on `UIViews`/Android-only modules if declared; check `animeworld/shared/build.gradle.kts`'s `androidMain.dependencies` block (added in Step 1) includes whatever module provides `com.programmersbox.uiviews.GenericInfo` (likely `projects.uiViews` — add `implementation(projects.uiViews)` to that block if not already present via a transitive dependency).
- All remaining imports/content (media3 ExoPlayer wiring, gesture handling, `VideoTopBar`/`VideoBottomBar`/`MediaControlGestures`/`GestureBox`, the two `@Preview` composables — drop the two `@Preview` functions, since Compose Preview annotations are an Android-Studio-tooling concept not meaningful in a shared module's build — everything else) carries over unchanged.

- [ ] **Step 5: Create the JVM stub actual**

Create `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videoplayer/VideoPlayerUi.jvm.kt`, reusing the existing `VideoNotSupportedScreen` content (from `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/VideoNotSupportedScreen.kt`, created in the earlier plan) as the stub body:

```kotlin
package com.programmersbox.anime.shared.videoplayer

import androidx.compose.runtime.Composable
import com.programmersbox.anime.shared.VideoNotSupportedScreen
import com.programmersbox.anime.shared.VideoScreen

@Composable
actual fun VideoPlayerUi(videoScreen: VideoScreen) {
    // TODO(user): replace with a real desktop video player implementation.
    // videoScreen.showPath is the stream URL or local file path to play;
    // videoScreen.referer carries any required request header for streaming.
    VideoNotSupportedScreen()
}
```

- [ ] **Step 6: Delete the old Android files**

```bash
git rm animeworld/src/main/java/com/programmersbox/animeworld/videoplayer/VideoPlayerCompose.kt
git rm animeworld/src/main/java/com/programmersbox/animeworld/videoplayer/VideoViewModel.kt
```

- [ ] **Step 7: Build to verify**

Run: `./gradlew :animeworld:shared:compileKotlinJvm` — expect BUILD SUCCESSFUL. Then run the Android-target compile task for `:animeworld:shared` (identified in Task 2 Step 5) — expect BUILD SUCCESSFUL, confirming the relocated ExoPlayer code compiles in its new location.

- [ ] **Step 8: Commit**

```bash
git add animeworld/shared/build.gradle.kts \
        animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/videoplayer \
        animeworld/shared/src/androidMain/kotlin/com/programmersbox/anime/shared/videoplayer \
        animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/videoplayer
git commit -m "feat(animeworld): expect/actual video player, relocate ExoPlayer implementation

VideoPlayerUi is now expect in commonMain. The existing ExoPlayer-based
implementation (VideoViewModel, VideoPlayerCompose, gesture handling)
moves into :animeworld:shared's androidMain essentially unchanged. The
jvmMain actual is a stub (today's VideoNotSupportedScreen content) for
a real desktop player to replace later."
```

---

### Task 6: Shared `chapterOnClick`, new `playOrCast` abstraction, nav wiring on both platforms

**Files:**
- Modify: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/GenericSharedAnime.kt`
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt`
- Modify: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericAnimeDesktop.kt`
- Modify: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt` (Koin: register `StorageHolder`, `VideoLibrarySource`)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt`'s `appModule` (Koin: register `StorageHolder` from the new shared package instead of the old Android-only one, register `VideoLibrarySource`)

**Interfaces:**
- Consumes: everything produced in Tasks 1-5.
- Produces: `GenericSharedAnime` now implements `chapterOnClick` concretely and declares `abstract fun playOrCast(navController: NavigationActions, storage: KmpStorage, model: KmpChapterModel, infoModel: KmpInfoModel)`.

- [ ] **Step 1: Add the shared `chapterOnClick` and `playOrCast` to `GenericSharedAnime`**

In `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/GenericSharedAnime.kt`, add:

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.programmersbox.anime.shared.videochoice.VideoSourceModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import kotlinx.coroutines.flow.firstOrNull
```

(merge with existing imports; `KmpChapterModel`/`KmpInfoModel` are likely already imported — don't duplicate)

Inside the `abstract class GenericSharedAnime`, add:

```kotlin
    var isLoadingChapter by mutableStateOf(false)
        private set

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        isLoadingChapter = true
        val storages = try {
            model.getChapterInfo().firstOrNull().orEmpty()
        } finally {
            isLoadingChapter = false
        }

        when {
            storages.size == 1 -> playOrCast(navController, storages.first(), model, infoModel)
            storages.isNotEmpty() -> VideoSourceModel.showVideoSources = VideoSourceModel(
                c = storages,
                infoModel = infoModel,
                isStreaming = true,
                model = model,
            )
            else -> Unit
        }
    }

    abstract fun playOrCast(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
    )
```

Remove `chapterOnClick` from the earlier abstract-members list if it was declared there separately (it's no longer abstract — it's now a concrete override, same as `downloadChapter` in `GenericSharedManga`'s pattern). `downloadChapter` and `ProfileIcon` remain the only abstract members besides the new `playOrCast`.

- [ ] **Step 2: Update Android's `GenericAnime` — implement `playOrCast`, remove old `chapterOnClick`/`getEpisodes`**

In `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt`:

1. Remove the entire existing `override suspend fun chapterOnClick(...)` block and the private `getEpisodes(...)` helper function (the `MaterialAlertDialogBuilder`-based loading dialog + `model.getChapterInfo()` fetch + single/multiple branching logic) — this logic is now in the shared `chapterOnClick`.
2. Add the `playOrCast` override:

```kotlin
    override fun playOrCast(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
    ) {
        if (MainActivity.cast.isCastActive()) {
            MainActivity.cast.loadUrl(
                storage.link,
                infoModel.title,
                model.name,
                infoModel.imageUrl,
                storage.headers
            )
        } else {
            storageHolder.storageModel = storage
            navController.navigate(
                VideoScreen(
                    showPath = storage.link.orEmpty(),
                    showName = model.name,
                    downloadOrStream = false,
                    referer = storage.headers["referer"] ?: storage.source.orEmpty()
                )
            )
        }
    }
```

3. Update the `globalNav3Setup()` override: replace the existing `navGraph.entry<VideoScreen> { VideoPlayerUi(it) }` entry with `navGraph.entry<VideoScreen> { VideoPlayerUi(it) }` using the now-shared `VideoScreen`/`VideoPlayerUi` (update the imports: `import com.programmersbox.anime.shared.VideoScreen`, `import com.programmersbox.anime.shared.videoplayer.VideoPlayerUi`, remove the old `com.programmersbox.animeworld.videoplayer.*` imports).
4. Update `settingsNav3Setup()`: replace `navGraph.entry<VideoViewerRoute> { ViewVideoScreen() }` with the shared versions (`import com.programmersbox.anime.shared.videos.VideoViewerRoute`, `import com.programmersbox.anime.shared.videos.ViewVideoScreen`), passing Android's real cast integration:

```kotlin
        navGraph.entry<VideoViewerRoute> {
            ViewVideoScreen(
                isCastActive = { MainActivity.cast.isCastActive() },
                onCastLoad = { content ->
                    MainActivity.cast.loadMedia(
                        File(content.path),
                        context.getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(content.path, 0L),
                        null, null
                    )
                },
                castButton = {
                    AndroidView(
                        factory = { ctx ->
                            MediaRouteButton(ctx).apply {
                                MainActivity.cast.showIntroductoryOverlay(this)
                                MainActivity.cast.setMediaRouteMenu(ctx, this)
                            }
                        }
                    )
                }
            )
        }
```

(Adjust the exact `onCastLoad`/`castButton` bodies to match whatever `ViewVideoScreen`'s final parameter list from Task 3 actually is — read the file you created in Task 3 Step 1 to confirm exact parameter names/types before writing this call site.)

5. Update `DialogSetups()`: it already renders `VideoChoiceScreen` from `VideoSourceModel.showVideoSources` — update the call to match the new shared `VideoChoiceScreen`'s signature (from Task 4): pass `genericInfo = this` (or however `GenericAnime` refers to itself as a `KmpGenericInfo`), `navController = LocalNavActions.current` (or whatever navigation-actions source `DialogSetups` already has access to), plus `isCastActive`/`onCastLoad` matching the same pattern as `settingsNav3Setup` above.
6. Update the `appModule` Koin block: remove `singleOf(::StorageHolder)` if it's currently registered from the old package (check first — it may not be explicitly registered if it was only ever constructor-injected as a class the app module owned; if `GenericAnime`'s constructor takes `storageHolder: StorageHolder` via Koin's automatic constructor resolution, no explicit `single { }` was needed before either way — just confirm the import now resolves to `com.programmersbox.anime.shared.StorageHolder` instead of the deleted local class). Add `singleOf(::VideoLibrarySource)` (new dependency, needs `Context` — Koin's automatic Android `Context` injection should resolve this the same way other Android-context-requiring classes in this module already do; check `GenericAnime`'s own `val context: Context` constructor param for the established pattern).

- [ ] **Step 3: Update Desktop's `GenericAnimeDesktop` — implement `playOrCast`, remove old stub `chapterOnClick`**

In `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericAnimeDesktop.kt`:

1. Remove the existing `override suspend fun chapterOnClick(...)` override entirely (it's now inherited, concrete, from `GenericSharedAnime`).
2. Add:

```kotlin
    override fun playOrCast(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
    ) {
        navController.navigate(
            VideoScreen(
                showPath = storage.link.orEmpty(),
                showName = model.name,
                downloadOrStream = false,
                referer = storage.headers["referer"] ?: storage.source.orEmpty()
            )
        )
    }
```

(Add the necessary imports: `com.programmersbox.anime.shared.VideoScreen`, `com.programmersbox.kmpmodels.KmpStorage` — check which are already present.)

3. Update `globalNav3Setup()`: replace the `navGraph.entry<VideoNotSupportedRoute> { VideoNotSupportedScreen() }` entry with `navGraph.entry<VideoScreen> { VideoPlayerUi(it) }` (import `com.programmersbox.anime.shared.VideoScreen`, `com.programmersbox.anime.shared.videoplayer.VideoPlayerUi`) — this now resolves to the JVM stub actual from Task 5, which still shows the "not supported" content, so behavior is unchanged until the user implements the real player; only the route type changes from the old `VideoNotSupportedRoute` to the shared `VideoScreen`.
4. Add a `settingsNav3Setup()` entry for the video library screen (there wasn't one before, since AnimeWorld desktop had no video list — this is new functionality, now real since `VideoLibrarySource`'s JVM actual works):

```kotlin
    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<PlatformSettings> { JvmSettingsScreen() }
        navGraph.entry<VideoViewerRoute> { ViewVideoScreen() }
    }
```

(imports: `com.programmersbox.anime.shared.videos.VideoViewerRoute`, `com.programmersbox.anime.shared.videos.ViewVideoScreen` — desktop uses the parameterless defaults, `isCastActive = { false }`, `onCastLoad = {}`, `castButton = {}`, since there's no casting on desktop.)

- [ ] **Step 4: Register the new Koin dependencies in `main.kt`**

In `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt`, add to the module block:

```kotlin
                            singleOf(::StorageHolder)
                            singleOf(::VideoLibrarySource)
```

(imports: `com.programmersbox.anime.shared.StorageHolder`, `com.programmersbox.anime.shared.videos.VideoLibrarySource` — `VideoLibrarySource`'s JVM constructor takes `AnimeDesktopSettings`, which is already registered via `singleOf(::AnimeDesktopSettings)` in this same file; Koin resolves it automatically via constructor injection, no extra wiring needed.)

- [ ] **Step 5: Remove the now-unused `VideoNotSupportedRoute`/`VideoNotSupportedScreen` route registration cleanup**

`VideoNotSupportedScreen` composable itself stays (it's now the JVM actual's stub body, per Task 5 Step 5) — only its standalone nav-route registration in `GenericAnimeDesktop.globalNav3Setup()` is removed (superseded by the `VideoScreen` entry in Step 3 above, which internally renders the same stub via `VideoPlayerUi`'s jvmMain actual).

- [ ] **Step 6: Build to verify — full app assembly this time**

Run: `./gradlew :animeworld:assembleNoFirebaseDebug` — expect BUILD SUCCESSFUL (this is the first point since Task 1 where the Android app module should compile cleanly again).

Run:
```bash
./gradlew :animeworld:desktop:run > /tmp/animeworld-video-refactor-run.log 2>&1 &
RUN_PID=$!
sleep 35
kill -0 $RUN_PID 2>/dev/null && echo "still alive after 35s" || echo "exited before 35s"
kill $RUN_PID 2>/dev/null
grep -ic "exception\|nosuchmethod\|caused by" /tmp/animeworld-video-refactor-run.log
```
Expect "still alive after 35s" and a `0` (or near-zero, double-check any hits aren't benign like `ExceptionDao`) exception count.

- [ ] **Step 7: Commit**

```bash
git add animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/GenericSharedAnime.kt \
        animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt \
        animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericAnimeDesktop.kt \
        animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt
git commit -m "refactor(animeworld): make chapterOnClick shared, add playOrCast abstraction

GenericSharedAnime now owns the fetch-then-decide flow (was duplicated
Android-only logic using a View-system loading dialog). Android and
Desktop each implement only playOrCast (cast-or-navigate vs. navigate)
and register the shared VideoScreen/VideoViewerRoute nav entries.
Desktop's stub player is now reached via the real VideoScreen route
instead of a separate VideoNotSupportedRoute, so wiring a real player
later is a one-file change (the jvmMain actual)."
```

---

## Self-Review Notes

- **Spec coverage:** all three named components (video list, player, choice screen) move to `:animeworld:shared` (Tasks 3, 5, 4 respectively); expect/actual used where the design calls for it (video data source in Task 2, player in Task 5); casting stays Android-only via parameterization (Tasks 3, 4, 6); `chapterOnClick` becomes shared per the design (Task 6); the JVM player stub is left for the user (Task 5 Step 5). No spec section is uncovered.
- **Placeholder scan:** Task 4's `Qualities`/`getQualityFromName` is copied verbatim from the real original (`AnimeUtils.kt:443-465`), verified by reading it directly — no placeholder guess left in the plan. Task 5's `TODO(user)` comment is intentional — it's the one deliberately-left stub the user asked to fill in themselves, not a plan gap.
- **Type consistency:** `VideoScreen(showPath, showName, downloadOrStream, referer)` used identically across Tasks 1, 3, 4, 5, 6. `playOrCast(navController, storage, model, infoModel)` signature matches between its declaration (Task 6 Step 1) and both overrides (Task 6 Steps 2-3). `VideoPlayerUi(videoScreen: VideoScreen)` matches between the expect (Task 5 Step 2) and both actuals (Task 5 Steps 4-5). `ViewVideoScreen(isCastActive, onCastLoad, castButton)` parameter names match between its definition (Task 3) and both call sites (Task 6 Steps 2, 3).
