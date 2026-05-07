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
            defaultModel(
                anthropicSettings
                ?.modelName
                    ?.let { models.find { model -> model.id == it } }
                ?: Model.DEFAULT
            )
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

    companion object {
        val models = listOf(
            Model.DEFAULT,
            Model.CLAUDE_OPUS_4_7,
            Model.CLAUDE_SONNET_4_6,
            Model.CLAUDE_SONNET_4_5_20250929,
            Model.CLAUDE_OPUS_4_6,
            Model.CLAUDE_OPUS_4_5_20251101,
            Model.CLAUDE_OPUS_4_1_20250805,
            Model.CLAUDE_OPUS_4_20250514,
            Model.CLAUDE_SONNET_4_20250514,
            Model.CLAUDE_3_7_SONNET_20250219,
            Model.CLAUDE_HAIKU_4_5_20251001,
            Model.CLAUDE_3_5_HAIKU_20241022,
            Model.CLAUDE_3_5_SONNET_20241022,
            Model.CLAUDE_3_5_SONNET_20240620,
            Model.CLAUDE_3_OPUS_20240229,
            Model.CLAUDE_3_HAIKU_20240307,
        )
    }
}