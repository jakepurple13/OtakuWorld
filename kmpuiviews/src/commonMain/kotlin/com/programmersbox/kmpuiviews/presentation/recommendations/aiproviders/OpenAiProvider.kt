package com.programmersbox.kmpuiviews.presentation.recommendations.aiproviders

import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.recommendations.AiRecommendationHandler

//TODO: Implement
class OpenAiProvider(
    private val newSettingsHandling: NewSettingsHandling,
) : AiRecommendationHandler {
    override suspend fun init() {
        newSettingsHandling.aiSettings.get().prompt
    }

    override suspend fun getResult(prompt: String): String? {
        return ""
    }
}