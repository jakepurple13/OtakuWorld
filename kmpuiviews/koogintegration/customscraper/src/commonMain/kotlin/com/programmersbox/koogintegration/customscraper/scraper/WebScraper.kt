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
 * **Lifecycle:** When [httpClient] is not provided the instance owns the default client.
 * Call [close] when the scraper is no longer needed to release the underlying connection pool.
 * If you supply your own [httpClient] you are responsible for closing it.
 *
 * @param executor   Koog PromptExecutor wrapping the caller's chosen LLM client.
 * @param model      LLModel to use for media URL extraction.
 * @param httpClient Inject a custom HttpClient; defaults to the platform-specific client
 *                   via [createHttpClient].
 */
class WebScraper internal constructor(
    private val httpClient: HttpClient,
    private val extractor: suspend (String) -> CustomScrapeKmpChapterModel,
) : AutoCloseable {

    /**
     * Production constructor. Compile-time guarantees that [executor] and [model] are non-null —
     * there is no runtime `error()` fallback.
     */
    constructor(
        executor: PromptExecutor,
        model: LLModel,
        httpClient: HttpClient = createHttpClient(),
    ) : this(
        httpClient = httpClient,
        extractor = LlmMediaExtractor(executor, model)::extract,
    )

    suspend fun scrape(url: String): CustomScrapeKmpChapterModel = runCatching {
        val response = httpClient.get(url)

        // Non-2xx — return empty rather than crashing so callers can show a graceful UI.
        // Note: this is a non-local return from an `inline` runCatching lambda, which exits
        // scrape() directly and bypasses getOrElse — this is intentional and correct.
        if (!response.status.isSuccess()) return CustomScrapeKmpChapterModel(urls = emptyList())

        val rawHtml = response.bodyAsText()
        val sanitizedHtml = HtmlSanitizer.sanitize(rawHtml)

        extractor(sanitizedHtml)
    }.getOrElse {
        // Network error, LLM parse failure, or any unexpected exception → empty result.
        CustomScrapeKmpChapterModel(urls = emptyList())
    }

    /** Releases the underlying [httpClient] connection pool. */
    override fun close() {
        httpClient.close()
    }
}
