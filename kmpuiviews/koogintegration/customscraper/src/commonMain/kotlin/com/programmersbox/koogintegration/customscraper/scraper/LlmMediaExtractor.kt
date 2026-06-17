package com.programmersbox.koogintegration.customscraper.scraper

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.ext.agent.structuredOutputWithToolsStrategy
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.json.JsonStructure
import ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator
import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpChapterModel
import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpInfoModel
import com.programmersbox.koogintegration.customscraper.model.ScraperResult

internal class LlmMediaExtractor(
    private val executor: PromptExecutor,
    private val model: LLModel,
) {
    // Agent specifically for chapters
    private val chapterAgent by lazy {
        AIAgent(
            promptExecutor = executor,
            agentConfig = AIAgentConfig(
                prompt = prompt("chapterScraper") { system(SYSTEM_PROMPT) },
                model = model,
                maxAgentIterations = 50
            ),
            strategy = structuredOutputWithToolsStrategy<CustomScrapeKmpChapterModel>(
                config = StructuredRequestConfig(
                    default = StructuredRequest.Manual(
                        JsonStructure.create<CustomScrapeKmpChapterModel>(
                            schemaGenerator = StandardJsonSchemaGenerator
                        )
                    )
                )
            )
        )
    }

    // Agent specifically for info
    private val infoAgent by lazy {
        AIAgent(
            promptExecutor = executor,
            agentConfig = AIAgentConfig(
                prompt = prompt("infoScraper") { system(SYSTEM_PROMPT) },
                model = model,
                maxAgentIterations = 100
            ),
            strategy = structuredOutputWithToolsStrategy<CustomScrapeKmpInfoModel>(
                config = StructuredRequestConfig(
                    default = StructuredRequest.Manual(
                        JsonStructure.create<CustomScrapeKmpInfoModel>(
                            schemaGenerator = StandardJsonSchemaGenerator
                        )
                    )
                )
            )
        )
    }

    suspend fun extractChapters(sanitizedHtml: String): ScraperResult {
        val instructions = """
            Your task is to extract ALL direct media URLs from the content:
            - For manga pages: extract all image URLs (from <img> src, data-src, data-lazy-src attributes, or JavaScript variables containing image URLs). And make sure they are in order as much as possible.
            - For anime/video pages: extract all video or streaming URLs (from <video>, <source> tags, or JavaScript variables containing .mp4, .m3u8, or similar video URLs).
            - For light novels: extract the main content (text) from the page.
            Do not include thumbnails, icons, logos, or navigation images. Only include content media.
            
            Content: $sanitizedHtml
        """.trimIndent()

        return chapterAgent.run(instructions, "scraper-chap-${sanitizedHtml.hashCode().toString(36)}")
    }

    //TODO: Maybe database or new source of dynamically webscraped sources. We save the url, cover image, and name.
    // If we go to see it, we must include refresh options since it could fail.
    // Also refresh for cover image, chapter list, etc.
    // MAYBE also look into a fine tuned model?
    // Maybe a pure local model?
    // Maybe give the user a choice on local or cloud.
    // Might be a thing to not do the chapter extract? Maybe give the option?
    suspend fun extractDetails(sanitizedHtml: String): ScraperResult {
        val instructions = """
            Extract the title, description, cover image URL, chapters, and genres from the content below.
            
            The description should be a description or a summary.
            
            For the chapters, they need both the urls and the names of the chapters.
            If the name for the chapter is unable to be found, it can be obtained by parsing the url.
            
            Content: $sanitizedHtml
        """.trimIndent()

        return infoAgent.run(instructions, "scraper-info-${sanitizedHtml.hashCode().toString(36)}")
    }

    companion object {
        private const val SYSTEM_PROMPT = """
            You are a web scraping assistant. You will be given raw HTML/JS content from a webpage.
            Your task is to extract the requested media URLs and information from the content accurately into the provided JSON schema.
        """
    }
}
