package com.programmersbox.koogintegration.customscraper.scraper

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpChapterModel
import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpInfoModel
import com.programmersbox.koogintegration.customscraper.platform.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/**
 * Public entry point for the custom web scraper.
 *
 * Production callers use the three-parameter public constructor:
 * ```kotlin
 * WebScraper(executor = myExecutor, model = myModel)
 * ```
 *
 * **Constructor convention note:** The internal primary constructor holds the fully resolved
 * fields `(httpClient, extractor)`. The public production constructor is secondary and delegates
 * to it. This is necessary in Kotlin because the public constructor needs to compute
 * `LlmMediaExtractor(executor, model)::extract` before handing it to the primary — a computation
 * that cannot be expressed in the primary position without exposing those parameters as fields.
 * The internal primary is only accessible directly by tests.
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
    private val llmMediaExtractor: LlmMediaExtractor,
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
        llmMediaExtractor = LlmMediaExtractor(executor, model),
    )

    suspend fun scrapeChapter(url: String): CustomScrapeKmpChapterModel {
        return scrapeUrl(url)
            .mapCatching { llmMediaExtractor.extractChapters(it) }
            .fold(
                onSuccess = {
                    when (it) {
                        is CustomScrapeKmpChapterModel -> it
                        is CustomScrapeKmpInfoModel -> error("Expected CustomScrapeKmpChapterModel, got CustomScrapeKmpInfoModel")
                    }
                },
                onFailure = { error(it) }
            )
    }

    suspend fun scrapeDetails(url: String): CustomScrapeKmpInfoModel {
        return scrapeUrl(url)
            .mapCatching { llmMediaExtractor.extractDetails(it) }
            .fold(
                onSuccess = {
                    when (it) {
                        is CustomScrapeKmpInfoModel -> it
                        is CustomScrapeKmpChapterModel -> error("Expected CustomScrapeKmpInfoModel, got CustomScrapeKmpChapterModel")
                    }
                },
                onFailure = { error(it) }
            )
    }

    private suspend fun scrapeUrl(url: String): Result<String> = runCatching {
        val response = httpClient.get(url)

        // Non-2xx — return empty rather than crashing so callers can show a graceful UI.
        // Note: this is a non-local return from an `inline` runCatching lambda, which exits
        // scrape() directly and bypasses getOrElse — this is intentional and correct.
        if (!response.status.isSuccess()) {
            error(response.status.description)
        }

        val rawHtml = response.bodyAsText()
        val sanitizedHtml = HtmlSanitizer.sanitize(rawHtml)

        sanitizedHtml
    }.onFailure { it.printStackTrace() }

    /** Releases the underlying [httpClient] connection pool. */
    override fun close() {
        httpClient.close()
    }
}
