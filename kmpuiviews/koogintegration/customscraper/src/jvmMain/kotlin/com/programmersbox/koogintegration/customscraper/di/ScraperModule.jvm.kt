package com.programmersbox.koogintegration.customscraper.di

import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import com.programmersbox.koogintegration.customscraper.scraper.WebScraper
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single {
        WebScraper(
            executor = MultiLLMPromptExecutor(OllamaClient()),
            model = OllamaModels.Meta.LLAMA_3_2
        )
    }
}