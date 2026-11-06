package com.programmersbox.kmpuiviews.translation.custom

import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.EngineKind
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.TranslationUnavailable
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotRuntime
import com.programmersbox.kmpuiviews.translation.bergamot.bergamotLanguageName
import com.programmersbox.kmpuiviews.translation.cleanLlmReply
import com.programmersbox.kmpuiviews.translation.planChain
import com.programmersbox.kmpuiviews.translation.translationSystemPrompt
import java.io.File

/** Translates with the user's own Bergamot model folders, or with an OpenAI-compatible server. */
class CustomEngine(
    private val settings: DesktopTranslationSettings,
    private val runtime: BergamotRuntime,
    private val client: OpenAiCompatClient,
) : TranslationEngine {
    override val kind = EngineKind.Custom

    private suspend fun validFolders(): List<FolderCheck.Valid> =
        settings.customFolders.get().sorted().mapNotNull { inspectFolder(File(it)) as? FolderCheck.Valid }

    override suspend fun targets(): List<String> =
        if (settings.customMode.get() == "server") {
            if (settings.customServerModel.get().isBlank()) emptyList() else TranslationLanguages.all.map { it.code }
        } else {
            validFolders().map { it.to }.distinct()
        }

    override suspend fun translate(text: String, source: String, target: String): String =
        if (settings.customMode.get() == "server") translateWithServer(text, source, target)
        else translateWithFolders(text, source, target)

    private suspend fun translateWithFolders(text: String, source: String, target: String): String {
        runtime.unavailableReason()?.let { throw TranslationUnavailable(it) }
        val s = bergamotLanguageName(source)
        val t = bergamotLanguageName(target)
        val steps = planChain(source, target, validFolders())
            ?: throw TranslationUnavailable("Add a folder for $s → $t, or $s → English and English → $t")
        return steps.fold(text) { current, folder -> runtime.translate(current, folder.files) }
    }

    private suspend fun translateWithServer(text: String, source: String, target: String): String {
        if (settings.customServerModel.get().isBlank()) {
            throw TranslationUnavailable("Pick a model in Settings → Translation")
        }
        val system = translationSystemPrompt(bergamotLanguageName(source), bergamotLanguageName(target))
        return cleanLlmReply(client.chat(system, text))
    }
}
