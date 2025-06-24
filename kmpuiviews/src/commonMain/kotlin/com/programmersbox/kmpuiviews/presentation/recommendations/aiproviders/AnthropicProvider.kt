package com.programmersbox.kmpuiviews.presentation.recommendations.aiproviders

import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.recommendations.AiRecommendationHandler
import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.Model
import com.xemantic.ai.anthropic.message.Message

class AnthropicProvider(
    newSettingsHandling: NewSettingsHandling,
) : AiRecommendationHandler {

    private val aiSettings = newSettingsHandling.aiSettings

    private var anthropic: Anthropic? = null

    override suspend fun init() {
        val anthropicSettings = aiSettings.get().anthropicSettings
        anthropic = Anthropic {
            apiKey = anthropicSettings?.apiKey ?: ""
            defaultModel = anthropicSettings
                ?.modelName
                ?.let { Model.entries.find { model -> model.id == it } }
                ?: Model.DEFAULT
        }
    }

    override suspend fun getResult(prompt: String): String? {
        val systemInstructions = aiSettings.get().prompt
        return anthropic
            ?.messages
            ?.create {
                system(systemInstructions)
                messages(
                    Message {
                        +prompt
                    }
                )
            }
            ?.text
            ?.removePrefix("```json")
            ?.removeSuffix("```")
            ?.trim()
    }
}