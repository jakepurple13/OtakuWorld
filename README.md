## 🌐  Supported Languages

[中文](README-zh.md)
- 🇺🇸 English
- 🇨🇳 Chinese
  
# English
# OtakuWorld

![Build Status](https://github.com/jakepurple13/OtakuWorld/actions/workflows/build_check.yaml/badge.svg)
![Build Status](https://github.com/jakepurple13/OtakuWorld/actions/workflows/nightly_release.yaml/badge.svg)
![Build Status](https://github.com/jakepurple13/OtakuWorld/actions/workflows/main_release.yml/badge.svg)
 
 [Discord Link](https://discord.gg/MhhHMWqryg) There's a discord now!!! This is something that will probably be changed and modified since it's the first time I've done this.

For the nightly release, go [here](https://github.com/jakepurple13/OtakuWorld/releases/tag/nightly)

A combined version of my [MangaWorld](https://github.com/jakepurple13/MangaWorld) and [AnimeWorld](https://github.com/jakepurple13/AnimeWorld) apps. This was created because there were a ton of shared elements between the two apps so I combined them, allowing two separate apps but a lot of the same base logic.

MangaWorld, AnimeWorld, and NovelWorld run on Android and on Desktop (Windows, macOS, and Linux).

These apps do not contain ANY sources themselves. Sources are extensions that you install from the
in-app Extensions screen, which shows catalog shelves, installed extensions, and per-extension
details with install progress.

## Extensions

- **MangaWorld:** installed [Mihon](https://mihon.app/)/Tachiyomi extensions load natively, with no
  bridge app. Add a Mihon repo URL in Extensions so its signing key marks that repo's extensions as
  trusted. In-app browsing and installing supports both repos that publish the legacy
  `index.min.json` and repos that publish Mihon's newer `index_v2` format. Desktop can install and
  use Mihon extensions too (non-Cloudflare sources; source settings are Android only).
- **AnimeWorld:** [Aniyomi](https://aniyomi.org/) extension APKs and
  [CloudStream](https://github.com/recloudstream/cloudstream) `.cs3` plugins load natively, with no
  bridge app. Repos must be trusted before their extensions load. Both work on Desktop as well
  (non-Cloudflare sources; plugins that need Android resources or a WebView, and extension
  settings, are Android only).
- **JavaScript/TypeScript extensions:** a sandboxed extension format (QuickJs) that runs on Android
  and Desktop in every app. See [Writing a JS/TS Extension](docs/js_ts_extension.md).

## Cloud Sync

Cloud sync uses your own [Supabase](https://supabase.com) project. Sign in from the app, then choose
which tables sync on each device. MangaWorld, AnimeWorld, and NovelWorld keep their data separate
even when signed into the same account. A Supabase config can be shared between devices by QR code.
See [docs/supabase/SETUP.md](docs/supabase/SETUP.md) for setup. Existing projects created before
per-app sync must run `docs/supabase/migration_add_app_column.sql` once.

Firebase sync and the custom server sync are no longer used.

## Contents
   * [Extensions](#extensions)
   * [Cloud Sync](#cloud-sync)
   * [Screenshots](#screenshots)
      * [MangaWorld](#mangaworld)
      * [AnimeWorld](#animeworld)
      * [AnimeWorldTV](#animeworldtv)
      * [NovelWorld](#novelworld)
      * [Otaku Manager](#otaku-manager)
   * [Features](#features)
        * [Shared](#shared-features)
      * [MangaWorld](#mangaworld-1)
      * [AnimeWorld](#animeworld-1)
      * [NovelWorld](#novelworld-1)
   * [AnimeWorldTV and Otaku Manager](#animeworldtv-and-otaku-manager)
   * [Building Locally](#building-locally)
   * [Desktop Apps](#desktop-apps)
   * [Issues](#issues)
   * [Contributing](#pull-requests)

## [Latest Release](https://github.com/jakepurple13/OtakuWorld/releases/latest)

Maintainers: see [docs/RELEASING.md](docs/RELEASING.md) for the release steps and checklist.

# Screenshots
<p align="center">
  <img src="/mangaworld/src/main/ic_launcher-playstore.png" width="32%"/>
  <img src="/animeworld/src/main/ic_launcher-playstore.png" width="32%"/>
  <img src="/novelworld/src/main/ic_launcher-playstore.png" width="32%"/>
  <img src="/otakumanager/src/main/ic_launcher-playstore.png" width="32%"/>
</p>

### MangaWorld
<p align="center">
  <img src="/ss/mw_recent.png" width="32%"/>
  <img src="/ss/mw_all.png" width="32%"/>
  <img src="/ss/mw_details.png" width="32%"/>
  <img src="/ss/mw_notifications.png" width="32%"/>
  <img src="/ss/mw_top_settings.png" width="32%"/>
  <img src="/ss/mw_more_settings.png" width="32%"/>
</p>

### AnimeWorld
<p align="center">
  <img src="/ss/aw_recent.png" width="32%"/>
  <img src="/ss/aw_all.png" width="32%"/>
  <img src="/ss/aw_details.png" width="32%"/>
  <img src="/ss/aw_history.png" width="32%"/>
  <img src="/ss/aw_top_settings.png" width="32%"/>
  <img src="/ss/aw_more_settings.png" width="32%"/>
</p>

### AnimeWorldTV
<p align="center">
  <img src="/ss/tv_ss_homescreen.png" width="32%"/>
  <img src="/ss/tv_ss_details.png" width="32%"/>
  <img src="/ss/tv_ss_episodes.png" width="32%"/>
  <img src="/ss/tv_ss_settings.png" width="32%"/>
  <img src="/ss/tv_ss_search.png" width="32%"/>
  <img src="/ss/tv_ss_favorites.png" width="32%"/>
</p>

### NovelWorld
<p align="center">
  <img src="/ss/nw_recent.png" width="32%"/>
  <img src="/ss/nw_all.png" width="32%"/>
  <img src="/ss/nw_details.png" width="32%"/>
  <img src="/ss/nw_global_search.png" width="32%"/>
  <img src="/ss/nw_favorites.png" width="32%"/>
  <img src="/ss/nw_settings.png" width="32%"/>
</p>

### Otaku Manager
_Not currently built. See [AnimeWorldTV and Otaku Manager](#animeworldtv-and-otaku-manager)._
<p align="center">
  <img src="/ss/otakumanager_ss_1.png" width="32%"/>
  <img src="/ss/otakumanager_ss_2.png" width="32%"/>
  <img src="/ss/otakumanager_ss_3.png" width="32%"/>
</p>

# Features

### Shared Features
- Sign in to sync favorites, history, lists, and watched chapters/episodes between devices through
  your own Supabase project
- Favorite a title to be alerted of updates. Update notifications list each title's new chapters or
  episodes, and an Update check errors screen shows what failed, with Retry and Share. Choose which
  sources the update checker skips
- Favorites with Overview and Library tabs, filters, an A–Z jump rail, and full-text search over
  titles, descriptions, and your notes
- Custom lists with covers, sort, grid/list view, filters, multi-select, and moving items between
  lists
- Notes on any title
- Notifications timeline with multi-select and a swipeable Catch-up deck
- History grouped by day, with search, source filter, and Undo
- Global search with per-source results pages that keep loading as you scroll
- Details screen chapter multi-select: mark read/unread, bookmark, download, or delete in bulk
- Quick actions (bolt button on Recent and Settings; Cmd/Ctrl+K on Desktop): check for updates, scan
  a QR code, background tasks, update check errors, daily overview, global search, lists, and saved
  notifications
- Daily overview: a recap of yesterday's reading or watching, with a by-hour graph. An on-device or
  local AI model can write the summary (Gemini Nano or Gemma on Android, Ollama on Desktop)
- Backup and restore wizard: pick exactly what to back up or restore, down to individual lists
- Personal Dictionary/Glossary of terms: manage entries from Settings, or quick-add a term while
  reading in MangaWorld
- Share titles by QR code with a share card, and open shared links in the app
- Account statistics, activity timer, and an About screen with build details
- Translation of descriptions: downloadable offline languages on Android; Ollama or any
  OpenAI-compatible server on Desktop
- AI chat with local model management
- In-app banners with Undo/Retry actions, color schemes, blur styles, biometric lock, and an
  onboarding flow with a Tips & Gestures page
- Android only: home screen widget, screensaver (Daydream), and Credential Manager sign-in
- Desktop only: native title bar, keyboard shortcuts (Cmd/Ctrl+/ lists them), and in-app updates
  on a Nightly or Stable track

### MangaWorld
- Read manga from Mihon/Tachiyomi extensions and other installed extensions
- Built-in reader with list and pager modes, chapter bookmarks, and quick-add to the Dictionary
- Download chapters (Desktop lets you choose the download folder)

### AnimeWorld
- Stream and download anime from Aniyomi extensions, CloudStream plugins, and other installed
  extensions
- Built-in video player with Classic, Floating, and Theater styles, a +1:30 skip button, and
  gesture brightness/volume
- Full-screen episode loading screen with a source picker and Retry / Try another source
- Subtitles: track picker, styling settings with a live preview, and an in-player quick sheet
- Episode downloads, including HLS (AES-128 and fMP4) streams, with subtitles kept for offline
  playback
- Downloads screen with live progress, retryable failed downloads, and a Free up space screen
- Cast to Chromecast and Google TV, with a remote screen, mini player, and subtitles on the TV
  (Android only)
- Desktop: libVLC player bundled in the app, progressive/HLS/DASH downloads, local video library
  folders, and subtitle generation with whisper.cpp

### NovelWorld
- Read novels from installed extensions in the built-in reader, on Android and Desktop

### AnimeWorldTV and Otaku Manager

AnimeWorldTV, Otaku Manager, and the `app` sync manager are commented out of `settings.gradle.kts`
and are not currently built. The screenshots and instructions below are kept for reference.

AnimeWorldTV:
- Stream Anime/TV/Movies from various different video sites
- Watch in a built-in video player
- Sync favorites with AnimeWorld
- Includes an opening skipper (just skips ahead 90 seconds)
- Available only on Android TV devices

Otaku Manager:
- View all favorites across all OtakuWorld applications. Requires you to login.
- Made using pure Jetpack Compose. No Xml for any views.

#### Instructions to Install/Update AnimeWorldTV
1. Download animeworldtv-release.apk

If using `adb`:

2.
```sh
adb install animeworldtv-release.apk
```

If not using `adb`:

2. Follow [Android Authority][aa]'s steps

# Building Locally
Be sure to change the build variant to a `noFirebase` variant (it is the default). Other than that,
nothing needs to change to run/build locally!

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug
./gradlew :animeworld:assembleNoFirebaseDebug
./gradlew :novelworld:assembleNoFirebaseDebug
./gradlew test
```

The build uses Kotlin 2.4.20, Gradle 9.7.1, and Java 21.

# Desktop Apps

All three apps have a JVM/Desktop Compose build. Packaged builds are in the
[nightly release](https://github.com/jakepurple13/OtakuWorld/releases/tag/nightly) and stable
releases: a ZIP for macOS, an NSIS installer for Windows, and an AppImage for Linux. Desktop builds
are unsigned for now. The apps check for updates on launch; pick the Nightly or Stable track under
Settings › Updates.

## MangaWorld Desktop

A JVM/Desktop Compose build of MangaWorld, sharing UI with the Android app via `:mangaworld:shared`.

```bash
./gradlew :mangaworld:desktop:run
```

**Works:** browsing, search, favorites, manga lists/details, reading chapters in the built-in reader, downloads
(with configurable download directory in Platform Settings), custom lists, and Mihon extensions from
Mihon repos (non-Cloudflare sources).

**Not yet supported:** Mihon source settings.

## AnimeWorld Desktop

A JVM/Desktop Compose build of AnimeWorld, sharing UI with the Android app via `:animeworld:shared`.

```bash
./gradlew :animeworld:desktop:run
```

**Works:** browsing, search, favorites, episode lists/details, Aniyomi extensions and CloudStream
plugins, an in-app video player (libVLC is bundled, no VLC install needed) with keyboard shortcuts
(press `?` in the player), subtitles, episode downloads for progressive, HLS, and DASH streams,
local video folders on the Downloads screen, a Free up space screen, and subtitle generation for
downloaded episodes with whisper.cpp (Settings › Subtitles).

**Not yet supported:** Chromecast/casting, Aniyomi/CloudStream extension settings, and plugins that
need Android resources or a WebView.

## NovelWorld Desktop

A JVM/Desktop Compose build of NovelWorld, sharing UI with the Android app via `:novelworld:shared`.

```bash
./gradlew :novelworld:desktop:run
```

**Works:** browsing, search, favorites, reading chapters in the built-in reader, and installed
extensions.

**Not yet supported:** downloads (NovelWorld has no download support on any platform yet).

# Issues

If you run into any issues, please create an issue request with the following details:

- Small description
- Steps taken
- Device
- Version of Android
- Expected behavior
- Actual behavior
- If the issue is a breaking issue or not
- Any other additional information you might think will help

# Pull Requests

If you want to add a new source or have a change that might make the app better

- Make a new branch
- Make changes
- Push and create a new PR
- Add me (@jakepurple13) as a reviewer

Disclaimer: I am not affiliated with Tachiyomi app or any fork hosted on GitHub.

---

## Custom Web Scraper

### Overview

The `:kmpuiviews:koogintegration:customscraper` module provides an on-demand HTML scraping pipeline that extracts manga image URLs and anime video stream URLs from any public webpage using an on-device LLM agent.

### Architecture Flow

    WebScraper.scrape(url)
      └─ Ktor HTTP GET → raw HTML
           └─ HtmlSanitizer → stripped HTML (≤ 8,000 chars)
                └─ LlmMediaExtractor (Koog AIAgent)
                     └─ CustomScrapeKmpChapterModel(urls=[...])

### Supported Use Cases

| Use Case | What it extracts |
|---|---|
| Manga chapter page | All `<img>` `src`, `data-src`, `data-lazy-src` URLs (page images) |
| Anime episode page | All `<video>`, `<source>` URLs and `.mp4` / `.m3u8` JS variables |

### Limitations

- **No authentication** — login-gated pages are not supported.
- **No CAPTCHA bypassing** — CAPTCHA-protected pages return an empty result.
- **No JS rendering** — client-side SPA content (rendered after page load) is not visible to the scraper.
- **No caching** — results are not persisted; each `scrape()` call is a fresh fetch.
- **URL extraction only** — downloading or displaying media is handled elsewhere in the app.

### Data Model

```kotlin
@Serializable
data class CustomScrapeKmpChapterModel(
    val urls: List<String>,  // extracted media URLs; empty on failure
)
```

### Usage

```kotlin
// Obtain executor + model from your AgentMaker (or any Koog LLM setup):
val executor = MultiLLMPromptExecutor(agentInfo.llmClient)
val model = agentInfo.model

val scraper = WebScraper(executor = executor, model = model)
val result = scraper.scrape("https://example.com/manga/chapter-1")
result.urls.forEach { println(it) }
```

### Technologies

| Technology | Role |
|---|---|
| **Kotlin Multiplatform** | Android + JVM Desktop targets |
| **Ktor** | HTTP client (OkHttp engine on both platforms) |
| **Koog** | LLM agent framework for structured JSON extraction |
| **kotlinx.serialization** | JSON deserialization of `CustomScrapeKmpChapterModel` |

[//]: # (Reference Links)
  [aa]: <https://www.androidauthority.com/sideloading-apps-on-android-tv-1189896/>
