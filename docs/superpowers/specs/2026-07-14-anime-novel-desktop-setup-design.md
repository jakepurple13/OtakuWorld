# AnimeWorld & NovelWorld Desktop + Shared Modules — Design

## Goal

Add `:animeworld:desktop` and `:novelworld:desktop` JVM modules, plus `:animeworld:shared`
(new) and `:novelworld:shared` (extend existing), following the exact pattern already
established by `:mangaworld:shared` / `:mangaworld:desktop`. MangaWorld's modules are the
canonical reference and are not modified.

## Current state (as of investigation)

- `:mangaworld:shared` / `:mangaworld:desktop` — fully working reference implementation.
- `:novelworld:shared` — **already exists and is already included** in `settings.gradle.kts`,
  but only contains the reader screen (`ChapterHolder`, `Platform.kt` expect, `reader/*`). No
  `jvmMain` actual, no `GenericSharedX`-style abstraction, no Koin module function. The Android
  `novelworld` app already depends on it for the reader only; `ItemListView`,
  `ComposeShimmerItem`, nav setup, etc. all still live in `novelworld`'s `GenericNovel.kt`.
- `:animeworld:shared` — **does not exist**. AnimeWorld's `GenericAnime.kt` is entirely
  Android-only: Chromecast/GMS cast, ExoPlayer-based video player, Android `DownloadManager`,
  `ChooserDialog` folder picker. None of this is desktop-portable as-is.
- `KmpGenericInfo` (commonMain interface) requires: `apkString`, `chapterOnClick`,
  `downloadChapter`, `ItemListView`, `ComposeShimmerItem`, `ProfileIcon`. Everything else has a
  default (no-op/empty) implementation.
- `PlatformGenericInfo` (expect/actual) adds nothing extra on JVM; adds `deepLinkUri` /
  `deepLinkDetails` / `deepLinkSettings` on Android only.
- `platformModule()` in `kmpuiviews/jvmMain/di/AppModule.jvm.kt` registers a `MangaDesktopSettings`
  single unconditionally for `SourceLoader`'s extension directory. This works fine for any app
  (not just manga) because `AppDirs` is already scoped per `AppConfig.appName` — no change needed
  here.

## Scope decisions (confirmed with user)

- **AnimeWorld desktop video playback**: stubbed in v1. Browsing/search/favorites/details work;
  clicking an episode shows a "not supported on desktop yet" placeholder screen instead of
  playing. No video player library is added.
- **AnimeWorld desktop downloads**: basic, desktop-only. A new JVM-only `AnimeDownloadManager`
  saves the episode stream to disk (fire-and-forget, no progress-tracked download screen/UI).
  Android's existing `DownloadManager`-based download stays completely untouched.
- **NovelWorld downloads**: out of scope entirely, on both platforms. `downloadChapter` remains a
  no-op (matches current Android behavior — it's a no-op today too).
- **Casting / Chromecast**: Android-only, stays entirely in the `animeworld` Android app module.
  Not moved to shared, not added to desktop.

## Module scaffolding

### `settings.gradle.kts`

Add, next to the existing `:mangaworld:desktop` / `:mangaworld:shared` / `:novelworld:shared`
lines:

```kotlin
include(":animeworld:desktop")
include(":animeworld:shared")
include(":novelworld:desktop")
```

### `:animeworld:shared` build.gradle.kts

New file, same shape as `novelworld/shared/build.gradle.kts`:

```kotlin
plugins {
    `otaku-multiplatform`
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("kotlinx-serialization")
}

kotlin {
    android {
        namespace = "com.programmersbox.anime.shared"
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(projects.kmpuiviews)
            implementation(commonLibs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.components.resources)
            implementation(commonLibs.material.kolor)
            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.bundles.koinKmp)
            implementation(projects.favoritesdatabase)
            implementation(projects.datastore)
            implementation(projects.kmpmodels)
            implementation(commonLibs.bundles.datastoreLibs)
            implementation(commonLibs.androidx.navigation3.runtime)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(desktopLibs.kotlinx.coroutines.swing)
        }
    }
}
```

No dedicated `datastore:animesettings` submodule — neither anime nor novel needs one (unlike
manga's `datastore:mangasettings`); plain `datastore` is sufficient.

### `:animeworld:desktop` / `:novelworld:desktop` build.gradle.kts

Copy of `mangaworld/desktop/build.gradle.kts`, with `projects.mangaworld.shared` swapped for
`projects.animeworld.shared` / `projects.novelworld.shared`, and `packageName` /
`mainClass` adjusted. The Koog-integration dependency (`projects.kmpuiviews.koogintegration`) is
a manga-desktop extra, not part of the core pattern — omitted here.

### Directory layout

```
animeworld/shared/src/{commonMain,androidMain,jvmMain}/kotlin/com/programmersbox/anime/shared/...
animeworld/desktop/src/{commonMain,jvmMain}/kotlin/com/programmersbox/desktop/...
novelworld/shared/src/jvmMain/kotlin/com/programmersbox/novel/shared/...   (new)
novelworld/desktop/src/{commonMain,jvmMain}/kotlin/com/programmersbox/desktop/...
```

Both desktop modules use package `com.programmersbox.desktop`, matching MangaWorld's desktop
module convention — safe since each is a separate Gradle module/app.

## Shared-code migration

### NovelWorld (low risk — mostly a straight move, no real platform divergence today)

- Add `novelworld/shared/src/jvmMain/kotlin/com/programmersbox/novel/shared/Platform.jvm.kt`:
  `actual fun platform() = "Desktop"` (the one missing actual for the existing expect).
- New `GenericSharedNovel : KmpGenericInfo` (commonMain, concrete — no real per-platform
  divergence exists for novel today) — move out of `GenericNovel.kt` verbatim: `ItemListView`,
  `ComposeShimmerItem`, `chapterOnClick`, `downloadChapter` (no-op), `globalNav3Setup` (the
  `NovelReader` entry).
- New `novelSharedModule()` Koin module function (commonMain) — move `singleOf(::ChapterHolder)`
  and `viewModelOf(::ReadViewModel)` out of the Android app's `appModule` into it. Mirrors
  `mangaSharedModule()`.
- Android's `GenericNovel` shrinks to: `class GenericNovel(...) : GenericSharedNovel(...),
  GenericInfo`, keeping only `apkString` (buildType branch) and `deepLinkDetails` /
  `deepLinkSettings` (Android `PendingIntent`-based). App's `appModule` calls
  `includes(novelSharedModule())`.
- New `GenericNovelDesktop` (`novelworld/desktop/src/jvmMain`) — same shape as
  `GenericMangaDesktop`: `class GenericNovelDesktop(...) : GenericSharedNovel(...),
  PlatformGenericInfo`, implementing `apkString` (empty string, matching manga desktop) and
  `ProfileIcon()`.

### AnimeWorld (bigger fork — real logic diverges per platform)

- New `GenericSharedAnime : KmpGenericInfo` (commonMain, **abstract**) — houses only
  `ItemListView`, `ComposeShimmerItem`, `apkString` (buildType branch, identical logic today, safe
  to share). `chapterOnClick` and `downloadChapter` stay **abstract** — not shared, since Android's
  real implementation (casting, quality-select dialog, ExoPlayer nav, `DownloadManager`) and
  Desktop's stub/download-manager implementation have nothing in common.
- Everything else in current `GenericAnime.kt` — `CastingViewModel`, `DetailActions`,
  `composeCustomPreferences`, `globalNav3Setup`/`settingsNav3Setup`, `DialogSetups`, folder-chooser
  settings, `getEpisodes`/`downloadVideo` — **stays in the Android app module, unchanged**. None of
  it is reusable for a stubbed desktop player; moving it would be pure churn.
- Android's `GenericAnime` now extends `GenericSharedAnime, GenericInfo`; only change is
  `ItemListView`/`ComposeShimmerItem` bodies are deleted (inherited from shared) — everything else
  identical, zero behavior change.
- New `GenericAnimeDesktop` (`animeworld/desktop/src/jvmMain`) — extends `GenericSharedAnime,
  PlatformGenericInfo`. `chapterOnClick` navigates to a stub screen ("Playback isn't supported on
  desktop yet"). `downloadChapter` calls the new `AnimeDownloadManager`. `ProfileIcon()` returns
  `""`.

## Desktop entry points

Per app (`animeworld/desktop`, `novelworld/desktop`), mirroring `mangaworld/desktop` exactly:

- `App.kt` (commonMain) — empty package-only stub, matching manga's, kept for structural parity.
- `main.kt` (jvmMain) — `AppDirs { appName = "AnimeWorld" / "NovelWorld" }`,
  `DataStoreSettings { ... }`, the `BackgroundWorkHandlerImpl.setupSyncCheckers(args)` early
  return, then `BaseDesktopUi(title = ...)` with a `moduleBlock` registering: `AppConfig`,
  `singleOf(::GenericAnimeDesktop / GenericNovelDesktop) { bindsGenericInfo() }`, the download
  manager single (anime only), and `includes(animeSharedModule() / novelSharedModule())`. No Koog
  integration.
- `PlatformSettings.kt` (jvmMain) — a `JvmSettingsScreen` composable in the shape of manga's:
  download-path picker (`rememberDirectoryPickerLauncher`) wired to a new
  `AnimeDesktopSettings` / `NovelDesktopSettings` class (jvmMain-only, living in the desktop
  module — **not** added to `kmpuiviews`, since the existing `MangaDesktopSettings` mechanism
  already works fine for other apps via per-app `AppDirs` scoping and touching the global module
  isn't necessary).

## Downloads

- **AnimeWorld**: new `AnimeDownloadManager` (jvm-only class, `animeworld/shared/src/jvmMain`),
  shaped like `MangaDownloadManager.jvm.kt` but simplified — fire-and-forget save-stream-to-disk
  on `downloadChapter`, writing to a directory configured via `AnimeDesktopSettings`. No
  progress-tracked download screen/UI; `observeChapterDownloadStates` / `deleteDownloadedChapter`
  / `batchDownloadChapters` stay on their `KmpGenericInfo` defaults (no-op/empty).
- **NovelWorld**: no download manager. `downloadChapter` stays the shared no-op from
  `GenericSharedNovel`.

## Video stub screen (AnimeWorld desktop only)

One small composable (e.g. `VideoNotSupportedScreen`) registered in
`GenericAnimeDesktop.globalNav3Setup()`, replacing the Android-only `VideoScreen` /
`VideoPlayerUi` nav entry. Message + back button only — no player dependency added.

## README

Append two new sections (not a rewrite) alongside the existing MangaWorld Desktop section:
AnimeWorld Desktop and NovelWorld Desktop — build command, what works (browse/search/favorites/
details, downloads where applicable), and what's stubbed (anime playback).

## Out of scope

- Modifying `:mangaworld:shared` / `:mangaworld:desktop`.
- Any iOS target changes.
- Unit tests.
- New backend/server-side components.
- Chromecast/casting on desktop.
- NovelWorld downloads on any platform.
- In-app video playback on AnimeWorld desktop (deferred to a future iteration).
