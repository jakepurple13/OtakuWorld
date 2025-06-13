package com.programmersbox.kmpuiviews.presentation.recommendations.aiproviders

import com.bay.aiclient.AiClient
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.recommendations.AiRecommendationHandler

class OpenAiProvider(
    private val newSettingsHandling: NewSettingsHandling,
) : AiRecommendationHandler {

    private val aiSettings = newSettingsHandling.aiSettings

    private var client: AiClient? = null

    override suspend fun init() {
        val aiSettings = aiSettings.get()
        client = AiClient.get(AiClient.Type.OPEN_AI) {
            apiKey = aiSettings.openAiSettings?.apiKey ?: ""
            defaultModel = aiSettings.openAiSettings?.modelName ?: "gpt-3.5-turbo"
        }
    }

    override suspend fun getResult(prompt: String): String? {
        val systemInstructions = aiSettings.get().prompt
        return client
            ?.generateText {
                this.prompt = prompt
                this.systemInstructions = systemInstructions
            }
            ?.onFailure { it.printStackTrace() }
            ?.mapCatching { it.response }
            ?.recoverCatching { it.message }
            ?.getOrNull()
    }
}