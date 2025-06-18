package com.programmersbox.kmpuiviews.di

import com.programmersbox.datastore.AiService
import com.programmersbox.kmpuiviews.presentation.recommendations.AiRecommendationHandler
import com.programmersbox.kmpuiviews.presentation.recommendations.aiproviders.AnthropicProvider
import com.programmersbox.kmpuiviews.presentation.recommendations.aiproviders.GeminiProvider
import com.programmersbox.kmpuiviews.presentation.recommendations.aiproviders.OpenAiProvider
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.named
import org.koin.dsl.module

val aiModule = module {
    aiOf(::GeminiProvider, AiService.Gemini.name)
    aiOf(::OpenAiProvider, AiService.OpenAi.name)
    aiOf(::AnthropicProvider, AiService.Anthropic.name)
}

private inline fun <reified T : AiRecommendationHandler, reified T1> Module.aiOf(
    crossinline provider: (T1) -> T,
    name: String,
) = factoryOf(provider) {
    named(name)
    bind<AiRecommendationHandler>()
}