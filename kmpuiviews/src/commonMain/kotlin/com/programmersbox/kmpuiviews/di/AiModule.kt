package com.programmersbox.kmpuiviews.di

import com.programmersbox.datastore.AiService
import com.programmersbox.kmpuiviews.presentation.recommendations.AiRecommendationHandler
import com.programmersbox.kmpuiviews.presentation.recommendations.aiproviders.GeminiProvider
import com.programmersbox.kmpuiviews.presentation.recommendations.aiproviders.OpenAiProvider
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.named
import org.koin.dsl.module

val aiModule = module {
    factoryOf(::GeminiProvider) {
        named(AiService.Gemini.name)
        bind<AiRecommendationHandler>()
    }

    factoryOf(::OpenAiProvider) {
        named(AiService.OpenAi.name)
        bind<AiRecommendationHandler>()
    }
}