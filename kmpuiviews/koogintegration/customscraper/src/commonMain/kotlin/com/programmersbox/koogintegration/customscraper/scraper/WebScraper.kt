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
 * @param executor  Koog PromptExecutor wrapping the caller's chosen LLM client.
 *                  Nullable — only required when no custom [extractor] is provided.
 * @param model     LLModel to use for media URL extraction.
 *                  Nullable — only required when no custom [extractor] is provided.
 * @param httpClient Optional — inject a custom HttpClient (useful for testing with MockEngine).
 *                   Defaults to the platform-specific client via createHttpClient().
 * @param extractor  Internal seam for testing — allows stubbing the LLM step without a live
 *                   executor. Defaults to [LlmMediaExtractor] backed by [executor] and [model].
 */
class WebScraper(
    executor: PromptExecutor? = null,
    model: LLModel? = null,
    private val httpClient: HttpClient = createHttpClient(),
    // When extractor is not overridden, we build LlmMediaExtractor lazily using the defaults.
    // The error() calls inside the lambda are only evaluated at call time, so tests that
    // supply their own extractor are never affected.
    private val extractor: suspend (String) -> CustomScrapeKmpChapterModel =
        LlmMediaExtractor(
            executor ?: error("executor required when no custom extractor provided"),
            model ?: error("model required when no custom extractor provided"),
        )::extract,
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
