# Custom Web Scraper — Design Spec

**Date:** 2026-06-15  
**Module:** `:koogintegration:customscraper`  
**Status:** Approved

---

## Overview

A self-contained KMP submodule that accepts a URL, fetches raw HTML via Ktor, sanitizes the HTML,
then passes it to a Koog LLM agent that extracts all media URLs (manga images or anime video streams).
Results are returned as a typed `CustomScrapeKmpChapterModel`.

---

## Architecture Flow

```
WebScraper.scrape(url: String): CustomScrapeKmpChapterModel
  └─ HttpClientFactory.createHttpClient()   // platform expect/actual
       └─ Ktor GET → raw HTML string
  └─ HtmlSanitizer.sanitize(html)          // pure string/regex, commonMain
       └─ stripped HTML ≤ 8,000 chars
  └─ LlmMediaExtractor.extract(html)       // Koog agent, commonMain
       └─ AIAgent + structuredOutputWithToolsStrategy
            └─ CustomScrapeKmpChapterModel(urls=[...])
```

---

## Public API

```kotlin
// Entry point — caller owns executor + model setup
class WebScraper(
    private val executor: PromptExecutor,
    private val model: LLModel,
) {
    suspend fun scrape(url: String): CustomScrapeKmpChapterModel
}
```

Callers (e.g. a ViewModel in `kmpuiviews`) obtain `PromptExecutor` and `LLModel` from their
existing `AgentMaker` and pass them in. `customscraper` has no knowledge of `koogintegration`
— this avoids a circular dependency (`kmpmodels` ← `kmpuiviews`).

---

## Data Model

```kotlin
@Serializable
data class CustomScrapeKmpChapterModel(
    val urls: List<String>,
)
```

---

## Module Details

### Plugin
`otaku-multiplatform-no-ios` — targets Android + JVM (Desktop). No iOS.

### Package

`com.programmersbox.koogintegration.customscraper`

### Source Set Layout

```
koogintegration/customscraper/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/com/programmersbox/koogintegration/customscraper/
    │   ├── model/
    │   │   └── CustomScrapeKmpChapterModel.kt
    │   ├── scraper/
    │   │   ├── WebScraper.kt              ← public entry point
    │   │   ├── HtmlSanitizer.kt
    │   │   └── LlmMediaExtractor.kt
    │   └── platform/
    │       └── HttpClientFactory.kt       ← expect fun createHttpClient()
    ├── androidMain/kotlin/.../platform/
    │   └── HttpClientFactory.kt           ← actual: HttpClient(Android)
    └── jvmMain/kotlin/.../platform/
        └── HttpClientFactory.kt           ← actual: HttpClient(OkHttp)
```

> **Note:** Source set is `jvmMain` (not `desktopMain`). The `MultiplatformLibraryNoIosPlugin`
> declares `jvm()`, so `applyDefaultHierarchyTemplate()` produces `jvmMain`.

---

## Dependencies

| Dependency | Source set | Catalog alias |
|---|---|---|
| `koog-agents` | commonMain | `libs.koog.agents` |
| `ktor-client-core` | commonMain | `libs.ktorCore` |
| `kotlinx-serialization-json` | commonMain | `libs.kotlinxSerialization` |
| `ktor-client-android` | androidMain | `libs.ktorAndroid` |
| `ktor-client-okhttp` | jvmMain | `libs.ktorOkHttp` |
| `kotlin-test` | commonTest | `libs.kotlin.test` |

> No new entries added to `libs.versions.toml`. OkHttp is used for JVM (no CIO in catalog;
> OkHttp is cross-platform compatible).

---

## HTTP Client — expect/actual

```kotlin
// commonMain
expect fun createHttpClient(): HttpClient

// androidMain
actual fun createHttpClient(): HttpClient = HttpClient(Android)

// jvmMain
actual fun createHttpClient(): HttpClient = HttpClient(OkHttp)
```

---

## HTML Sanitizer

Pure Kotlin string/regex — no platform dependencies.

- **Strip:** `<style>`, `<script>` (unless containing media URL patterns), `<nav>`, `<footer>`,
  `<header>`, HTML comments, ad-related divs.
- **Preserve:** `<img>`, `<video>`, `<source>`, `<a>` tags and `src`, `href`, `data-src`,
  `data-lazy-src` attributes.
- **Max length:** 8,000 characters (configurable parameter, default 8000).
- **Overflow:** If sanitized HTML still exceeds limit, extract only the preserved media tags
  and concatenate them.

---

## Koog LLM Agent

Single-shot `AIAgent` — no tools, structured output only.

```kotlin
// Strategy
structuredOutputWithToolsStrategy<CustomScrapeKmpChapterModel>(
    config = StructuredRequestConfig(
        default = StructuredRequest.Manual(
            JsonStructure.create<CustomScrapeKmpChapterModel>(
                schemaGenerator = StandardJsonSchemaGenerator
            )
        )
    )
)

// Tool registry
ToolRegistry { /* empty — no tools needed */ }
```

### System Prompt

```
You are a web scraping assistant. You will be given raw HTML/JS content from a webpage.
Your task is to extract ALL direct media URLs from the content:
- For manga pages: extract all image URLs (from <img> src, data-src, data-lazy-src attributes,
  or JavaScript variables containing image URLs).
- For anime/video pages: extract all video or streaming URLs (from <video>, <source> tags,
  or JavaScript variables containing .mp4, .m3u8, or similar video URLs).
Return ONLY a JSON object in this exact format: {"urls": ["url1", "url2", ...]}
If no media URLs are found, return: {"urls": []}
Do not include thumbnails, icons, logos, or navigation images. Only include content media.
```

---

## Error Handling

| Failure | Behavior |
|---|---|
| Non-200 HTTP response | Return `CustomScrapeKmpChapterModel(urls = emptyList())` |
| Network exception | Catch, return empty model |
| Malformed / unparseable LLM output | Catch, return empty model |

---

## Out of Scope

- Login-protected / authenticated pages (no cookie/session handling)
- CAPTCHA bypassing
- JavaScript-rendered SPAs (no headless browser)
- Downloading or displaying media (URL extraction only)
- Caching or persistence of scraped results

---

## Settings Change

Add to `settings.gradle.kts`:
```kotlin
include(":koogintegration:customscraper")
```

---

## README

Add a section to the root `README.md` documenting:
- Feature description and purpose
- Architecture flow diagram
- Supported use cases (manga image extraction, anime video extraction)
- Limitations (out-of-scope items)
- `CustomScrapeKmpChapterModel` data class
- Technologies used (Ktor, Koog, KMP)
