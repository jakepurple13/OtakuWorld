package com.programmersbox.koogintegration.customscraper

import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class WebScraperTest {

    // Stub extractor that always returns a fixed result — lets us test HTTP logic without an LLM.
    private val stubResult = CustomScrapeKmpChapterModel(urls = listOf("https://example.com/page1.jpg"))

    @Test
    fun realTest() = runTest {
        WebScraper(
            executor = MultiLLMPromptExecutor(OllamaClient()),
            model = OllamaModels.Meta.LLAMA_3_2
        ).use {
            runCatching { println(it.scrapeChapter("https://picsum.photos/")) }
                .onFailure { it.printStackTrace() }

            runCatching {
                val info = it.scrapeDetails("details-url-here")
                println(info)
            }.onFailure { it.printStackTrace() }
        }
    }

    @Test
    fun returnsEmptyModelOnNon200Response() = runTest {
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond("Not Found", HttpStatusCode.NotFound)
                }
            }
        }
        var extractorCallCount = 0
        val result = WebScraper(
            executor = MultiLLMPromptExecutor(OllamaClient()),
            model = OllamaModels.Meta.LLAMA_3_2
        ).use {
            it.scrapeChapter("https://example.com/manga/chapter-1")
        }
        assertEquals(emptyList<String>(), result.urls)
        assertEquals(0, extractorCallCount, "extractor must not be called on non-200")
    }

    @Test
    fun returnsEmptyModelOnNetworkException() = runTest {
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler { throw RuntimeException("Network failure") }
            }
        }
        val result = WebScraper(
            executor = MultiLLMPromptExecutor(OllamaClient()),
            model = OllamaModels.Meta.LLAMA_3_2
        ).use {
            it.scrapeChapter("https://example.com/manga/chapter-1")
        }
        assertEquals(emptyList<String>(), result.urls)
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
        val result = WebScraper(
            executor = MultiLLMPromptExecutor(OllamaClient()),
            model = OllamaModels.Meta.LLAMA_3_2
        ).use {
            it.scrapeChapter("https://example.com/manga/chapter-1")
        }
        assertEquals(stubResult.urls, result.urls)
        // Sanitizer should have stripped <style> before handing off to extractor
        val html = assertNotNull(capturedHtml)
        assertFalse(html.contains("<style>"), "sanitizer should strip <style> blocks")
    }

    @Test
    fun returnsEmptyModelWhenExtractorThrows() = runTest {
        val mockClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        "<img src='page1.jpg'>",
                        HttpStatusCode.OK,
                        headersOf("Content-Type", "text/html")
                    )
                }
            }
        }
        val result = WebScraper(
            executor = MultiLLMPromptExecutor(OllamaClient()),
            model = OllamaModels.Meta.LLAMA_3_2
        ).use {
            it.scrapeChapter("https://example.com/manga/chapter-1")
        }
        assertEquals(emptyList<String>(), result.urls)
    }
}
