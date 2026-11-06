package com.programmersbox.kmpuiviews.translation.ollama

import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.EngineKind
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.TranslationUnavailable
import com.programmersbox.kmpuiviews.translation.cleanLlmReply
import com.programmersbox.kmpuiviews.translation.translationSystemPrompt
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import java.io.IOException

class OllamaEngine(
    private val client: OllamaClient,
    private val settings: DesktopTranslationSettings,
) : TranslationEngine {
    override val kind = EngineKind.Ollama

    override suspend fun targets(): List<String> {
        if (settings.ollamaModel.get().isBlank()) return emptyList()
        return if (client.status() is OllamaStatus.Connected) TranslationLanguages.all.map { it.code } else emptyList()
    }

    override suspend fun translate(text: String, source: String, target: String): String {
        val model = settings.ollamaModel.get()
        if (model.isBlank()) throw TranslationUnavailable("Pick an Ollama model in Settings → Translation")
        val system = translationSystemPrompt(
            TranslationLanguages.byCode(source)?.englishName ?: source,
            TranslationLanguages.byCode(target)?.englishName ?: target,
        )
        return try {
            cleanLlmReply(client.chat(model, system, text))
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            throw TranslationUnavailable("Ollama took too long to answer at ${settings.ollamaUrl.get().trim().trimEnd('/')}.")
        } catch (e: ConnectTimeoutException) {
            throw TranslationUnavailable("Ollama took too long to answer at ${settings.ollamaUrl.get().trim().trimEnd('/')}.")
        } catch (e: IOException) {
            throw TranslationUnavailable("Can't reach Ollama at ${settings.ollamaUrl.get()}. Is it running?")
        } catch (e: Exception) {
            throw TranslationUnavailable(e.message ?: "Ollama translation failed")
        }
    }
}
