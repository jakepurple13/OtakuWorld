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

These apps do not contain ANY sources themselves. In order to pull sources, the apps do contain some
default sources.

Right now, we do support cloud syncing via Firebase, however, that will be going away soon.
We are making an alternative way that requires some manual configuration.
Using [OtakuWorld](app/readme.md) will allow you to setup a manual configuration!
A server is required, fortunately, we have made a basic server that will only work on a local
network.
Help wanted to help set up a more solid solution!

For MangaWorld: Any sources that work with [Mihon](https://mihon.app/) will work with
MangaWorld...After the bridge
is installed.

For AnimeWorld: Any sources that work with [Aniyomi](https://aniyomi.org/) will work with
AnimeWorld...After the
bridge is installed.

## Contents
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
      * [AnimeWorldTV](#animeworldtv-1)
      * [NovelWorld](#novelworld-1)
      * [Otaku Manager](#otaku-manager-1)
   * [AnimeWorldTV Install/Update Instructions](#instructions-to-installupdate-animeworldtv)
   * [Issues](#issues)
   * [Contributing](#pull-requests)

## [Latest Release](https://github.com/jakepurple13/OtakuWorld/releases/latest)

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
<p align="center">
  <img src="/ss/otakumanager_ss_1.png" width="32%"/>
  <img src="/ss/otakumanager_ss_2.png" width="32%"/>
  <img src="/ss/otakumanager_ss_3.png" width="32%"/>
</p>

# Features

### Shared Features
- Log in to save your favorites and watched episodes from device to device
- Favorite to be alerted of any updates
- Share Anime/Manga and open in app!

### AnimeWorld
- Stream and download Anime from various different video sites
- Watch in a built-in video player
- Cast Videos to Chromecast Enabled Devices!

### AnimeWorldTV
- Stream Anime/TV/Movies from various different video sites
- Watch in a built-in video player
- Sync favorites with AnimeWorld
- Includes an opening skipper (just skips ahead 90 seconds)
- Available only on Android TV devices

### MangaWorld
- Read Manga from various different manga sites

### NovelWorld
- Read Novels from various different novel sites

### Otaku Manager
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
Be sure to change the build variant to a `noFirebase` variant. Other than that, nothing needs to change to run/build locally!

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

---

## Dynamic Translation

### Overview

The `:kmpuiviews:koogintegration:dynamictranslation` module provides a fully on-device pipeline that OCRs text from manga/comic images, translates it locally, inpaints the original text out of the image, and overlays the translated text — returning both the modified image and structured translation data with bounding boxes. No cloud APIs or Google Mobile Services required.

### Pipeline

    agent.translate(imageBytes, config)
      └─ OCR (Tesseract / Tess4J)
           └─ Local translation (Ollama/Koog on JVM | NLLB TFLite/LiteRT on Android)
                └─ Inpainting (OpenCV — removes original text regions)
                     └─ Text overlay (AWT on JVM | Canvas on Android)
                          └─ TranslationOutput(imageBytes, List<TranslatedBlock>)

### Supported Platforms

| Platform | OCR | Translation | Rendering |
|---|---|---|---|
| **JVM / Desktop** | Tess4J | Ollama via Koog | OpenCV + AWT |
| **Android** | Tesseract4Android | LiteRT / NLLB TFLite | OpenCV + Canvas |

### Data Model

```kotlin
data class TranslatedBlock(
    val originalText: String,
    val translatedText: String,
    val boundingBox: Rect,   // pixel coordinates in the output image
)

data class TranslationOutput(
    val imageBytes: ByteArray,          // translated image (same format as input)
    val translations: List<TranslatedBlock>,
)
```

### Usage

```kotlin
val config = DynamicTranslationConfig(
    sourceLanguage = "jpn",
    targetLanguage = "eng",
    tessDataPath = "/path/to/tessdata",
    ollamaModel = "llama3.2",               // JVM only
    nllbModelPath = "/path/to/nllb.tflite"  // Android only
)
val agent = buildDynamicTranslationAgent(config)
agent.use { a ->
    val output = a.translate(imageBytes, config)
    // output.imageBytes — translated image (same format as input)
    // output.translations — List<TranslatedBlock> with bounding boxes
}
```

### Technologies

| Technology | Role |
|---|---|
| **Kotlin Multiplatform** | Android + JVM Desktop targets |
| **Tess4J / Tesseract4Android** | OCR — extracts text and bounding boxes from images |
| **Ollama + Koog** | LLM-based translation on JVM/Desktop |
| **LiteRT (TFLite) + NLLB** | On-device neural translation on Android |
| **OpenCV** | Inpainting — erases original text regions from the image |
| **AWT / Canvas** | Renders translated text back onto the cleaned image |

[//]: # (Reference Links)
  [aa]: <https://www.androidauthority.com/sideloading-apps-on-android-tv-1189896/>
