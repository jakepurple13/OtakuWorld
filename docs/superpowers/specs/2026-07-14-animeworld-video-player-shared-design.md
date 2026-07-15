# AnimeWorld Video List/Player/Choice → Shared Module — Design

## Goal

Move the video list (library) screen, video player, and video-choice (quality picker) screen out
of the Android-only `animeworld` app module and into `:animeworld:shared`, using expect/actual
where genuine per-platform divergence exists, so `:animeworld:desktop` can get real playback
instead of the current `VideoNotSupportedScreen` stub. The user will implement the JVM `actual`
for the player itself; this effort's job is to design and build the seam (expect declarations,
shared logic, Android actual/relocation) and leave a clear stub for the JVM actual.

## Current state

Three components live entirely in the Android `animeworld` app module today, all with real
Android coupling:

- **Video list/library** (`videos/ViewVideosFragment.kt` + `ViewVideoViewModel.kt`) — browses
  videos already downloaded to the device via Android `MediaStore` (`VideoGet`), lets the user
  play/delete them, integrates Chromecast (`MainActivity.cast`) and Android permissions.
- **Video player** (`videoplayer/VideoViewModel.kt` + `VideoPlayerCompose.kt` +
  `VideoPlayerActivity.kt`) — ExoPlayer (`androidx.media3`)-based, reads battery state
  (`BatteryInformation2`), reads `StorageHolder` for the resolved stream link.
- **Video choice** (`videochoice/VideoChoiceScreen.kt` + `VideoSourceViewModel.kt`) — a quality
  picker bottom sheet shown when an episode has multiple `KmpStorage` options. Currently checks
  Chromecast active state, casts to `GenericAnime` directly to call Android's download method, and
  navigates via an Android `Context` extension (`navigateToVideoPlayer`).

`chapterOnClick` (the entry point — currently abstract in `GenericSharedAnime`, fully
Android-implemented in `GenericAnime.kt`) fetches `KmpStorage` options via
`model.getChapterInfo()`, shows a `MaterialAlertDialogBuilder` (Android View-system) loading
dialog, then either auto-plays/casts (single result) or pops `VideoChoiceScreen` via a
`VideoSourceModel.showVideoSources` static (single result vs. multiple).

## What moves where

### Fully shared (commonMain, `animeworld/shared/src/commonMain`)

- **`VideoScreen`** (the `@Serializable data class ... : NavKey` nav route — `showPath`,
  `showName`, `downloadOrStream`, `referer`) — plain data, no platform dependency.
- **`StorageHolder`** (currently a tiny Android-app class: `var storageModel: KmpStorage?`) — moves
  here so both `GenericAnime` (Android) and `GenericAnimeDesktop` can share one Koin-registered
  instance.
- **`VideoSourceModel`** (the quality-choice state holder + its `companion object`
  `showVideoSources` static) — plain data + Compose state, no platform dependency.
- **`VideoChoiceScreen`** — the quality-picker bottom sheet UI. Two Android-specific touch points
  get parameterized instead of hardcoded:
  - `isCastActive: () -> Boolean = { false }` (Android's `GenericAnime` passes the real
    `MainActivity.cast.isCastActive()` check; desktop always `false`)
  - `onCastLoad: (KmpStorage) -> Unit = {}` (Android wires to `MainActivity.cast.loadUrl(...)`;
    desktop passes a no-op — no casting exists there)
  - The direct `genericInfo as GenericAnime` cast to call `downloadVideo` is replaced with the
    existing `KmpGenericInfo.downloadChapter(...)` interface method — no cast needed.
- **`chapterOnClick`'s fetch-then-decide logic** — becomes a **concrete** implementation in
  `GenericSharedAnime` (no longer abstract): calls `model.getChapterInfo()`, tracks a simple
  Compose loading `MutableState<Boolean>` (replacing the Android `MaterialAlertDialogBuilder`,
  which doesn't exist on desktop), and on result either calls the one remaining abstract hook
  (single storage found) or sets `VideoSourceModel.showVideoSources` (multiple found, `DialogSetups`
  renders `VideoChoiceScreen`).

### Stays abstract per-platform (in `GenericSharedAnime`)

- **`playOrCast(navController, storage, model, infoModel)`** — new abstract method, the *only*
  genuinely platform-specific piece of the old `chapterOnClick`. Android's implementation checks
  `MainActivity.cast.isCastActive()` and either casts or navigates to `VideoScreen`; Desktop's
  implementation always navigates to `VideoScreen` (using the real player once implemented).
- **`downloadChapter`** — unchanged from the existing design (Task 3/4/5): Android keeps its
  `DownloadManager`-based flow; Desktop keeps `AnimeDownloadManager`. Desktop downloads stay
  "automatic best-available quality" — `VideoChoiceScreen`'s non-streaming (download) branch stays
  Android-only; not extended to desktop in this effort.
- **`ProfileIcon()`** — unchanged, already abstract.

### expect/actual (player)

- `expect @Composable fun VideoPlayerUi(videoScreen: VideoScreen)` in
  `animeworld/shared/src/commonMain`.
- `actual` in `animeworld/shared/src/androidMain` — the existing ExoPlayer-based `VideoViewModel` +
  `VideoPlayerCompose.kt` content, relocated essentially as-is (still depends on `androidx.media3`,
  battery info, etc. — androidMain of a KMP module can depend on Android-only libraries, same
  pattern already used by `MangaDownloadManager`'s Android/JVM split).
- `actual` in `animeworld/shared/src/jvmMain` — **left as a stub for the user to implement.** The
  stub keeps today's `VideoNotSupportedScreen` content so the app still compiles and runs
  meaningfully until the user replaces it.

### expect/actual (video library / list screen)

- `ViewVideoScreen` and its data source are the least portable piece: Android sources videos from
  `MediaStore` (`VideoGet`); there's no `MediaStore` on desktop.
- `expect class VideoLibrarySource` with a single method, e.g. `fun observeVideos():
  Flow<List<VideoContent>>` (or an equivalent shared model — see Open Question below for the exact
  shape of `VideoContent`, which is currently Android-app-specific).
- `actual` in androidMain wraps the existing `VideoGet`/`MediaStore` scan, unchanged behavior.
- `actual` in jvmMain scans the directory `AnimeDesktopSettings.downloadsDirectory` (from Task 4)
  for video files and maps them into the shared model — this is new, real (not stubbed) desktop
  functionality, since it doesn't depend on any video-playback library, only file listing.
- The screen's Chromecast button (`MediaRouteButton` in the top bar, swipe-to-cast row action) is
  Android-only UI — kept out of the shared composable via the same `isCastActive`/`onCastLoad`
  parameterization used in `VideoChoiceScreen`, or simply omitted entirely on desktop (no
  swipe-to-cast row action rendered when a `isCastActive`-style parameter is absent/false).

## Casting stays Android-only

No casting SDK code, UI, or dependency is added to `:animeworld:shared` or `:animeworld:desktop`.
Every place shared code needs to know "is casting active" or "send this to a cast device," it
takes a callback parameter that Android supplies with real logic and Desktop supplies as a
no-op/`false`. `CastingViewModel`, `DetailActions`, `ExpandedControlsActivity`,
`MyMiniControllerFragment`, `MediaRouteButton` usage in `composeCustomPreferences` — all of that
stays in the Android `animeworld` app module, untouched.

## Video-library item model

`VideoContent` (currently `com.programmersbox.animeworld.VideoContent`, Android-`MediaStore`-shaped:
`videoId: Long`, `assetFileStringUri: String`, etc.) is replaced with a new, genuinely
platform-neutral shared model in `animeworld/shared/src/commonMain`:

```kotlin
data class SharedVideoContent(
    val videoName: String,
    val path: String,
    val duration: Long,
    val lastPlayedPositionMs: Long,
)
```

- Android's `actual VideoLibrarySource` wraps the existing `VideoGet`/`MediaStore` scan and maps
  its results into `SharedVideoContent` (`assetFileStringUri` → `path`, `videoDuration` →
  `duration`, the existing `SharedPreferences`-backed resume-position lookup → `lastPlayedPositionMs`).
- Desktop's `actual VideoLibrarySource` scans `AnimeDesktopSettings.downloadsDirectory` for video
  files and produces `SharedVideoContent` directly (`lastPlayedPositionMs` defaults to `0` — no
  resume-position tracking exists yet on desktop; out of scope to add here).
- `ViewVideosFragment.kt`'s rendering (`ViewVideoScreen`, `VideoContentView`, thumbnail loading,
  swipe-to-delete/cast) moves to commonMain and is adapted to consume `SharedVideoContent` instead
  of the old `VideoContent`. Thumbnail generation (`coil3.video.VideoFrameDecoder`,
  `videoFramePercent`) is Android-specific (uses Android's video-frame extraction) — this one piece
  stays behind a small `expect fun VideoThumbnail(path: String): Painter?`-style seam (or is simply
  omitted from the desktop render — a generic file icon in its place — rather than expecting the
  user to also implement thumbnail extraction; the user only asked for list/player/choice, not
  thumbnailing). Desktop shows a plain icon placeholder instead of a real frame thumbnail for now.

## Out of scope

- Implementing the actual JVM video player (VLCJ or otherwise) — the user is doing this themselves.
- Adding desktop download quality-choice (stays automatic-best-quality, as already shipped).
- Any change to `:mangaworld:shared`/`:mangaworld:desktop`.
- Any casting functionality on desktop.
