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
