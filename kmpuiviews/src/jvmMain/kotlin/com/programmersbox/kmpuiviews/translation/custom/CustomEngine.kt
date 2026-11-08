package com.programmersbox.kmpuiviews.translation.custom

import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.EngineKind
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.TranslationUnavailable
import com.programmersbox.kmpuiviews.translation.cleanLlmReply
import com.programmersbox.kmpuiviews.translation.translationSystemPrompt

/** Translates with any OpenAI-compatible server (LM Studio, llama.cpp, vLLM, ...). */
class CustomEngine(
    private val settings: DesktopTranslationSettings,
    private val client: OpenAiCompatClient,
) : TranslationEngine {
    override val kind = EngineKind.Custom

    override suspend fun targets(): List<String> =
        if (settings.customServerModel.get().isBlank()) emptyList() else TranslationLanguages.all.map { it.code }

    override suspend fun translate(text: String, source: String, target: String): String {
        if (settings.customServerModel.get().isBlank()) {
            throw TranslationUnavailable("Pick a model in Settings → Translation")
        }
        val system = translationSystemPrompt(languageName(source), languageName(target))
        return cleanLlmReply(client.chat(system, text))
    }

    private fun languageName(code: String) = TranslationLanguages.byCode(code)?.englishName ?: code
}
