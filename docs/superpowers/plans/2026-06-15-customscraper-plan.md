# Custom Web Scraper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create `:kmpuiviews:koogintegration:customscraper`, a KMP submodule that fetches raw HTML from a URL, sanitizes it, and uses a Koog LLM agent to extract manga image or anime video URLs into a typed `CustomScrapeKmpChapterModel`.

**Architecture:** `WebScraper.scrape(url)` fetches HTML via a platform-specific Ktor client (OkHttp on Android, OkHttp on JVM), passes sanitized HTML through `HtmlSanitizer`, then hands it to `LlmMediaExtractor` which runs a single-shot Koog `AIAgent` with structured output. The caller owns the `PromptExecutor` and `LLModel` and passes them in — `customscraper` has zero knowledge of `AgentMaker` or `koogintegration`'s internals.

**Tech Stack:** Kotlin Multiplatform (Android + JVM), Ktor 3.3.3 (HTTP), Koog 1.0.0 (LLM), kotlinx.serialization 1.11.0 (JSON), kotlin-test (unit tests)

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `settings.gradle.kts` | Modify | Add `include(":kmpuiviews:koogintegration:customscraper")` |
| `kmpuiviews/koogintegration/customscraper/build.gradle.kts` | Create | Module definition, plugin, dependencies |
| `…/customscraper/model/CustomScrapeKmpChapterModel.kt` | Create | Serializable data class for extracted URLs |
| `…/customscraper/platform/HttpClientFactory.kt` (commonMain) | Create | `expect fun createHttpClient(): HttpClient` |
| `…/customscraper/platform/HttpClientFactory.kt` (androidMain) | Create | `actual` — Android engine |
| `…/customscraper/platform/HttpClientFactory.kt` (jvmMain) | Create | `actual` — OkHttp engine |
| `…/customscraper/scraper/HtmlSanitizer.kt` | Create | Pure regex/string HTML stripping, 8k char cap |
| `…/customscraper/scraper/LlmMediaExtractor.kt` | Create | Koog AIAgent, structured output, system prompt |
| `…/customscraper/scraper/WebScraper.kt` | Create | Public entry point orchestrating fetch → sanitize → extract |
| `README.md` | Modify | Add Custom Web Scraper section |

All source paths below are relative to:
`kmpuiviews/koogintegration/customscraper/src/`

Base package for all files: `com.programmersbox.koogintegration.customscraper`

---

## Task 1: Gradle Module Scaffold

**Files:**
- Modify: `settings.gradle.kts`
- Create: `kmpuiviews/koogintegration/customscraper/build.gradle.kts`

- [ ] **Step 1: Add include to settings**

Open `settings.gradle.kts`. Find the existing line `include(":kmpuiviews:koogintegration")` and add the new include directly after it:

```kotlin
include(":kmpuiviews:koogintegration")
include(":kmpuiviews:koogintegration:customscraper")  // add this line
```

- [ ] **Step 2: Create the module directory**

```bash
mkdir -p kmpuiviews/koogintegration/customscraper/src/commonMain/kotlin/com/programmersbox/koogintegration/customscraper/{model,scraper,platform}
mkdir -p kmpuiviews/koogintegration/customscraper/src/androidMain/kotlin/com/programmersbox/koogintegration/customscraper/platform
mkdir -p kmpuiviews/koogintegration/customscraper/src/jvmMain/kotlin/com/programmersbox/koogintegration/customscraper/platform
mkdir -p kmpuiviews/koogintegration/customscraper/src/commonTest/kotlin/com/programmersbox/koogintegration/customscraper
```

- [ ] **Step 3: Create build.gradle.kts**

Create `kmpuiviews/koogintegration/customscraper/build.gradle.kts`:

```kotlin
plugins {
    `otaku-multiplatform-no-ios`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.koogintegration.customscraper"
}

kotlin {
    android {
        namespace = "com.programmersbox.koogintegration.customscraper"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(libs.kotlinxSerialization)
                implementation(libs.ktorCore)
                implementation(libs.koog.agents)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.ktorMock)
            }
        }

        androidMain {
            dependencies {
                // Android Ktor engine (OkHttp-based, optimized for Android)
                implementation(libs.ktorAndroid)
            }
        }

        jvmMain {
            dependencies {
                // OkHttp engine — works cross-platform on JVM; no CIO in catalog
                implementation(libs.ktorOkHttp)
            }
        }
    }
}
```

- [ ] **Step 4: Verify Gradle sync succeeds**

```bash
./gradlew :kmpuiviews:koogintegration:customscraper:tasks --quiet
```

Expected: task list prints without error. If Gradle can't resolve the module, check the `include(...)` line in settings.gradle.kts is spelled correctly.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts kmpuiviews/koogintegration/customscraper/build.gradle.kts
git commit -m "chore: scaffold :kmpuiviews:koogintegration:customscraper module"
```

---

## Task 2: Data Model

**Files:**
- Create: `commonMain/kotlin/…/model/CustomScrapeKmpChapterModel.kt`
- Create: `commonTest/kotlin/…/CustomScrapeKmpChapterModelTest.kt`

- [ ] **Step 1: Write the failing test**

Create `commonTest/kotlin/com/programmersbox/koogintegration/customscraper/CustomScrapeKmpChapterModelTest.kt`:

```kotlin
package com.programmersbox.koogintegration.customscraper

import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpChapterModel
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomScrapeKmpChapterModelTest {

    @Test
    fun serializesToJson() {
        val model = CustomScrapeKmpChapterModel(
            urls = listOf("https://example.com/page1.jpg", "https://example.com/page2.jpg")
        )
        val json = Json.encodeToString(CustomScrapeKmpChapterModel.serializer(), model)
        assertEquals("""{"urls":["https://example.com/page1.jpg","https://example.com/page2.jpg"]}""", json)
    }

    @Test
    fun deserializesFromJson() {
        val json = """{"urls":["https://example.com/video.mp4"]}"""
        val model = Json.decodeFromString(CustomScrapeKmpChapterModel.serializer(), json)
        assertEquals(listOf("https://example.com/video.mp4"), model.urls)
    }

    @Test
    fun emptyUrlsRoundTrips() {
        val model = CustomScrapeKmpChapterModel(urls = emptyList())
        val json = Json.encodeToString(CustomScrapeKmpChapterModel.serializer(), model)
        val decoded = Json.decodeFromString(CustomScrapeKmpChapterModel.serializer(), json)
        assertEquals(model, decoded)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :kmpuiviews:koogintegration:customscraper:jvmTest --tests "*.CustomScrapeKmpChapterModelTest" 2>&1 | tail -20
```

Expected: FAIL — `CustomScrapeKmpChapterModel` does not exist yet.

- [ ] **Step 3: Create the data model**

Create `commonMain/kotlin/com/programmersbox/koogintegration/customscraper/model/CustomScrapeKmpChapterModel.kt`:

```kotlin
package com.programmersbox.koogintegration.customscraper.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomScrapeKmpChapterModel(
    val urls: List<String>,
)
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :kmpuiviews:koogintegration:customscraper:jvmTest --tests "*.CustomScrapeKmpChapterModelTest" 2>&1 | tail -10
```

Expected: `3 tests completed, 0 failures`

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/koogintegration/customscraper/src/
git commit -m "feat(customscraper): add CustomScrapeKmpChapterModel data class"
```

---

## Task 3: Platform HTTP Client (expect/actual)

**Files:**
- Create: `commonMain/kotlin/…/platform/HttpClientFactory.kt`
- Create: `androidMain/kotlin/…/platform/HttpClientFactory.kt`
- Create: `jvmMain/kotlin/…/platform/HttpClientFactory.kt`

No unit tests for this task — it wires platform engines that are tested indirectly in Task 6.

- [ ] **Step 1: Create commonMain expect declaration**

Create `commonMain/kotlin/com/programmersbox/koogintegration/customscraper/platform/HttpClientFactory.kt`:

```kotlin
package com.programmersbox.koogintegration.customscraper.platform

import io.ktor.client.HttpClient

// Platform-specific engine selected at compile time via expect/actual.
// Android uses the Android (OkHttp) engine; JVM Desktop uses OkHttp directly.
expect fun createHttpClient(): HttpClient
```

- [ ] **Step 2: Create androidMain actual**

Create `androidMain/kotlin/com/programmersbox/koogintegration/customscraper/platform/HttpClientFactory.kt`:

```kotlin
package com.programmersbox.koogintegration.customscraper.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

// Android engine — wraps OkHttp with Android-specific socket/timeout defaults.
actual fun createHttpClient(): HttpClient = HttpClient(Android)
```

- [ ] **Step 3: Create jvmMain actual**

Create `jvmMain/kotlin/com/programmersbox/koogintegration/customscraper/platform/HttpClientFactory.kt`:

```kotlin
package com.programmersbox.koogintegration.customscraper.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

// OkHttp engine — cross-platform, works on Desktop JVM without Android runtime.
// No CIO engine is present in the project's version catalog, so OkHttp is used here.
actual fun createHttpClient(): HttpClient = HttpClient(OkHttp)
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :kmpuiviews:koogintegration:customscraper:compileKotlinJvm 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/koogintegration/customscraper/src/
git commit -m "feat(customscraper): add platform expect/actual HTTP client factory"
```

---

## Task 4: HTML Sanitizer

**Files:**
- Create: `commonMain/kotlin/…/scraper/HtmlSanitizer.kt`
- Create: `commonTest/kotlin/…/HtmlSanitizerTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `commonTest/kotlin/com/programmersbox/koogintegration/customscraper/HtmlSanitizerTest.kt`:

```kotlin
package com.programmersbox.koogintegration.customscraper

import com.programmersbox.koogintegration.customscraper.scraper.HtmlSanitizer
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlSanitizerTest {

    // --- Strip tests ---

    @Test
    fun stripsStyleBlocks() {
        val html = "<html><style>body { color: red; }</style><img src='a.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<style>"), "style block should be removed")
        assertFalse(result.contains("color: red"), "style content should be removed")
    }

    @Test
    fun stripsScriptBlocksWithoutMediaUrls() {
        val html = "<html><script>var x = 1; doSomething();</script><img src='a.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<script>"), "script block should be removed")
        assertFalse(result.contains("doSomething"), "script content should be removed")
    }

    @Test
    fun preservesScriptBlocksContainingMp4Urls() {
        val html = """<html><script>var video = "https://cdn.example.com/ep1.mp4";</script></html>"""
        val result = HtmlSanitizer.sanitize(html)
        assertContains(result, ".mp4", message = "script with .mp4 URL should be kept")
    }

    @Test
    fun preservesScriptBlocksContainingM3u8Urls() {
        val html = """<html><script>var src = "https://cdn.example.com/stream.m3u8";</script></html>"""
        val result = HtmlSanitizer.sanitize(html)
        assertContains(result, ".m3u8", message = "script with .m3u8 URL should be kept")
    }

    @Test
    fun stripsNavBlock() {
        val html = "<html><nav><a href='/home'>Home</a></nav><img src='page1.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<nav>"), "nav block should be removed")
        assertFalse(result.contains("Home</a>"), "nav content should be removed")
    }

    @Test
    fun stripsFooterBlock() {
        val html = "<html><footer>Copyright 2025</footer><img src='page1.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<footer>"), "footer should be removed")
        assertFalse(result.contains("Copyright"), "footer content should be removed")
    }

    @Test
    fun stripsHeaderBlock() {
        val html = "<html><header><h1>Site Title</h1></header><img src='page1.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<header>"), "header should be removed")
        assertFalse(result.contains("Site Title"), "header content should be removed")
    }

    @Test
    fun stripsHtmlComments() {
        val html = "<html><!-- this is a comment --><img src='page1.jpg'><!-- another --></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertFalse(result.contains("<!--"), "HTML comments should be removed")
        assertFalse(result.contains("this is a comment"), "comment content should be removed")
    }

    // --- Preserve tests ---

    @Test
    fun preservesImgTags() {
        val html = "<html><img src='https://example.com/page1.jpg' data-src='lazy.jpg'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertContains(result, "<img")
        assertContains(result, "page1.jpg")
    }

    @Test
    fun preservesVideoTags() {
        val html = "<html><video src='https://example.com/ep1.mp4'></video></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertContains(result, "<video")
        assertContains(result, "ep1.mp4")
    }

    @Test
    fun preservesSourceTags() {
        val html = "<html><source src='https://example.com/stream.m3u8'></html>"
        val result = HtmlSanitizer.sanitize(html)
        assertContains(result, "<source")
        assertContains(result, "stream.m3u8")
    }

    // --- Length cap tests ---

    @Test
    fun capsOutputAtMaxLength() {
        val html = "<html>" + "x".repeat(10_000) + "</html>"
        val result = HtmlSanitizer.sanitize(html, maxLength = 8_000)
        assertTrue(result.length <= 8_000, "Output must not exceed maxLength")
    }

    @Test
    fun overflowFallbackExtractsMediaTags() {
        // 5000 chars of noise + media tags at the end → overflow triggers media-only extraction
        val noise = "<p>" + "a".repeat(5_000) + "</p>"
        val mediaTag = """<img src="https://example.com/page1.jpg">"""
        val html = "<html>$noise$mediaTag</html>"
        val result = HtmlSanitizer.sanitize(html, maxLength = 100)
        assertContains(result, "page1.jpg", message = "media tag should survive overflow fallback")
    }

    @Test
    fun shortHtmlPassesThroughUnchanged() {
        val html = "<img src='a.jpg'>"
        val result = HtmlSanitizer.sanitize(html, maxLength = 8_000)
        assertContains(result, "a.jpg")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :kmpuiviews:koogintegration:customscraper:jvmTest --tests "*.HtmlSanitizerTest" 2>&1 | tail -15
```

Expected: FAIL — `HtmlSanitizer` does not exist yet.

- [ ] **Step 3: Implement HtmlSanitizer**

Create `commonMain/kotlin/com/programmersbox/koogintegration/customscraper/scraper/HtmlSanitizer.kt`:

```kotlin
package com.programmersbox.koogintegration.customscraper.scraper

object HtmlSanitizer {

    // Script blocks are kept only if they contain one of these media URL patterns.
    private val mediaUrlPatterns = listOf(".mp4", ".m3u8", ".jpg", ".jpeg", ".png", ".webp", ".gif")

    // Matches self-closing or open tags for the four media-relevant elements.
    private val mediaTagRegex = Regex(
        """<(?:img|video|source|a)\b[^>]*>""",
        RegexOption.IGNORE_CASE
    )

    fun sanitize(html: String, maxLength: Int = 8_000): String {
        var result = html

        // Strip HTML comments first so comment-wrapped tags don't confuse later patterns.
        result = result.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

        // Strip <style> blocks entirely.
        result = result.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")

        // Strip <script> blocks unless the content contains a media URL pattern.
        result = result.replace(Regex("<script[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)) { match ->
            val content = match.groupValues[1]
            if (mediaUrlPatterns.any { content.contains(it, ignoreCase = true) }) match.value else ""
        }

        // Strip structural navigation/layout blocks — these never contain content media.
        result = result.replace(Regex("<nav[^>]*>.*?</nav>", RegexOption.DOT_MATCHES_ALL), "")
        result = result.replace(Regex("<footer[^>]*>.*?</footer>", RegexOption.DOT_MATCHES_ALL), "")
        result = result.replace(Regex("<header[^>]*>.*?</header>", RegexOption.DOT_MATCHES_ALL), "")

        if (result.length <= maxLength) return result

        // Overflow: concatenate only the preserved media tags so the LLM gets
        // the highest-value content within the character budget.
        val mediaTags = mediaTagRegex.findAll(result)
            .map { it.value }
            .joinToString("\n")

        return mediaTags.take(maxLength)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :kmpuiviews:koogintegration:customscraper:jvmTest --tests "*.HtmlSanitizerTest" 2>&1 | tail -10
```

Expected: all tests pass, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/koogintegration/customscraper/src/
git commit -m "feat(customscraper): add HtmlSanitizer with media-tag preservation and 8k cap"
```

---

## Task 5: LLM Media Extractor

**Files:**
- Create: `commonMain/kotlin/…/scraper/LlmMediaExtractor.kt`

No isolated unit tests here — the Koog agent requires a live `PromptExecutor`. This is tested end-to-end via `WebScraper` in Task 6 (error-path tests) and by integration testing with a real executor.

- [ ] **Step 1: Create LlmMediaExtractor**

Create `commonMain/kotlin/com/programmersbox/koogintegration/customscraper/scraper/LlmMediaExtractor.kt`:

```kotlin
package com.programmersbox.koogintegration.customscraper.scraper

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.structuredOutputWithToolsStrategy
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.json.JsonStructure
import ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator
import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpChapterModel

internal class LlmMediaExtractor(
    private val executor: PromptExecutor,
    private val model: LLModel,
) {

    // Structured schema built once — JsonStructure is thread-safe and reusable.
    private val structure = JsonStructure.create<CustomScrapeKmpChapterModel>(
        schemaGenerator = StandardJsonSchemaGenerator
    )

    suspend fun extract(sanitizedHtml: String): CustomScrapeKmpChapterModel {
        // Single-shot agent: no tools, no history, structured JSON output only.
        val agentConfig = AIAgentConfig(
            prompt = prompt("customscraper") {
                system(SYSTEM_PROMPT)
            },
            model = model,
            // Low iteration cap — we only need one LLM call for extraction.
            maxAgentIterations = 5
        )

        val agent = AIAgent(
            promptExecutor = executor,
            agentConfig = agentConfig,
            strategy = structuredOutputWithToolsStrategy<CustomScrapeKmpChapterModel>(
                config = StructuredRequestConfig(
                    default = StructuredRequest.Manual(structure)
                )
            ),
            // No tools needed — the LLM parses HTML directly and returns JSON.
            toolRegistry = ToolRegistry { }
        )

        return agent.run(sanitizedHtml, "scraper-session")
    }

    companion object {
        // Exact system prompt from the design spec — do not paraphrase.
        private const val SYSTEM_PROMPT = """You are a web scraping assistant. You will be given raw HTML/JS content from a webpage.
Your task is to extract ALL direct media URLs from the content:
- For manga pages: extract all image URLs (from <img> src, data-src, data-lazy-src attributes, or JavaScript variables containing image URLs).
- For anime/video pages: extract all video or streaming URLs (from <video>, <source> tags, or JavaScript variables containing .mp4, .m3u8, or similar video URLs).
Return ONLY a JSON object in this exact format: {"urls": ["url1", "url2", ...]}
If no media URLs are found, return: {"urls": []}
Do not include thumbnails, icons, logos, or navigation images. Only include content media."""
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :kmpuiviews:koogintegration:customscraper:compileKotlinJvm 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/koogintegration/customscraper/src/commonMain/
git commit -m "feat(customscraper): add LlmMediaExtractor with Koog structured output agent"
```

---

## Task 6: WebScraper — Public Entry Point

**Files:**
- Create: `commonMain/kotlin/…/scraper/WebScraper.kt`
- Create: `commonTest/kotlin/…/WebScraperTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `commonTest/kotlin/com/programmersbox/koogintegration/customscraper/WebScraperTest.kt`:

```kotlin
package com.programmersbox.koogintegration.customscraper

import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpChapterModel
import com.programmersbox.koogintegration.customscraper.scraper.WebScraper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WebScraperTest {

    // Stub extractor that always returns a fixed result — lets us test HTTP logic without an LLM.
    private val stubResult = CustomScrapeKmpChapterModel(urls = listOf("https://example.com/page1.jpg"))

    @Test
    fun returnsEmptyModelOnNon200Response() = runTest {
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond("Not Found", HttpStatusCode.NotFound)
                }
            }
        }
        val scraper = WebScraper(
            httpClient = mockClient,
            extractor = { _ -> stubResult } // extractor should NOT be called
        )
        val result = scraper.scrape("https://example.com/manga/chapter-1")
        assertEquals(emptyList(), result.urls)
    }

    @Test
    fun returnsEmptyModelOnNetworkException() = runTest {
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { throw RuntimeException("Network failure") }
            }
        }
        val scraper = WebScraper(
            httpClient = mockClient,
            extractor = { _ -> stubResult }
        )
        val result = scraper.scrape("https://example.com/manga/chapter-1")
        assertEquals(emptyList(), result.urls)
    }

    @Test
    fun callsExtractorWithSanitizedHtmlOn200() = runTest {
        val rawHtml = "<html><style>body{}</style><img src='page1.jpg'></html>"
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(rawHtml, HttpStatusCode.OK, headersOf("Content-Type", "text/html"))
                }
            }
        }
        var capturedHtml: String? = null
        val scraper = WebScraper(
            httpClient = mockClient,
            extractor = { html ->
                capturedHtml = html
                stubResult
            }
        )
        val result = scraper.scrape("https://example.com/manga/chapter-1")
        assertEquals(stubResult.urls, result.urls)
        // Sanitizer should have stripped <style> before handing off to extractor
        assertEquals(false, capturedHtml?.contains("<style>"))
    }

    @Test
    fun returnsEmptyModelWhenExtractorThrows() = runTest {
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond("<img src='page1.jpg'>", HttpStatusCode.OK, headersOf("Content-Type", "text/html"))
                }
            }
        }
        val scraper = WebScraper(
            httpClient = mockClient,
            extractor = { _ -> throw RuntimeException("LLM failed") }
        )
        val result = scraper.scrape("https://example.com/manga/chapter-1")
        assertEquals(emptyList(), result.urls)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :kmpuiviews:koogintegration:customscraper:jvmTest --tests "*.WebScraperTest" 2>&1 | tail -15
```

Expected: FAIL — `WebScraper` does not exist yet.

- [ ] **Step 3: Implement WebScraper**

Create `commonMain/kotlin/com/programmersbox/koogintegration/customscraper/scraper/WebScraper.kt`:

```kotlin
package com.programmersbox.koogintegration.customscraper.scraper

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpChapterModel
import com.programmersbox.koogintegration.customscraper.platform.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/**
 * Public entry point for the custom web scraper.
 *
 * @param executor Koog PromptExecutor wrapping the caller's chosen LLM client.
 * @param model    LLModel to use for media URL extraction.
 * @param httpClient Optional — inject a custom HttpClient (useful for testing with MockEngine).
 *                   Defaults to the platform-specific client via createHttpClient().
 */
class WebScraper(
    executor: PromptExecutor,
    model: LLModel,
    private val httpClient: HttpClient = createHttpClient(),
    // Internal seam for testing — allows stubbing the LLM step without a live executor.
    private val extractor: suspend (String) -> CustomScrapeKmpChapterModel =
        LlmMediaExtractor(executor, model)::extract,
) {

    suspend fun scrape(url: String): CustomScrapeKmpChapterModel = runCatching {
        val response = httpClient.get(url)

        // Non-2xx status — return empty rather than crashing so callers can show a graceful UI.
        if (!response.status.isSuccess()) return CustomScrapeKmpChapterModel(urls = emptyList())

        val rawHtml = response.bodyAsText()
        val sanitizedHtml = HtmlSanitizer.sanitize(rawHtml)

        extractor(sanitizedHtml)
    }.getOrElse {
        // Network error, LLM parse failure, or any unexpected exception → empty result.
        CustomScrapeKmpChapterModel(urls = emptyList())
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :kmpuiviews:koogintegration:customscraper:jvmTest --tests "*.WebScraperTest" 2>&1 | tail -10
```

Expected: `4 tests completed, 0 failures`

- [ ] **Step 5: Run all module tests**

```bash
./gradlew :kmpuiviews:koogintegration:customscraper:jvmTest 2>&1 | tail -10
```

Expected: all tests pass across `CustomScrapeKmpChapterModelTest`, `HtmlSanitizerTest`, `WebScraperTest`.

- [ ] **Step 6: Commit**

```bash
git add kmpuiviews/koogintegration/customscraper/src/
git commit -m "feat(customscraper): add WebScraper public entry point with error handling"
```

---

## Task 7: README Documentation

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add Custom Web Scraper section to README**

Open `README.md` and append the following section at the end of the file (before any trailing newlines):

```markdown
---

## Custom Web Scraper

### Overview

The `:kmpuiviews:koogintegration:customscraper` module provides an on-demand HTML scraping pipeline that extracts manga image URLs and anime video stream URLs from any public webpage using an on-device LLM agent.

### Architecture Flow

```
WebScraper.scrape(url)
  └─ Ktor HTTP GET → raw HTML
       └─ HtmlSanitizer → stripped HTML (≤ 8,000 chars)
            └─ LlmMediaExtractor (Koog AIAgent)
                 └─ CustomScrapeKmpChapterModel(urls=[...])
```

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
```

- [ ] **Step 2: Verify README renders cleanly (no broken code fences)**

Open the file and visually confirm all code fences are properly closed. There is a nested code fence in the "Architecture Flow" section — verify the outer markdown code fence uses backtick-triple and the inner uses triple-backtick for a different language tag or is indented.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: document Custom Web Scraper feature in README"
```

---

## Self-Review Checklist

- [x] **Spec coverage**: all 10 files from spec are covered across 7 tasks
- [x] **Settings path**: uses `:kmpuiviews:koogintegration:customscraper` (not `:koogintegration:customscraper` — that would be wrong given how the project includes koogintegration)
- [x] **No CIO**: `jvmMain` uses `ktorOkHttp`, not CIO (CIO is absent from the catalog)
- [x] **Circular deps**: `LlmMediaExtractor` uses Koog types directly; no import from the parent `koogintegration` module
- [x] **Type consistency**: `CustomScrapeKmpChapterModel` is referenced identically across all tasks
- [x] **WebScraper API**: `WebScraper(executor, model)` — `httpClient` and `extractor` are defaulted, so public API matches the approved design
- [x] **Error handling**: non-200, network exception, and LLM exception all tested and covered by `runCatching`
- [x] **No placeholders**: all code blocks contain complete, runnable code
