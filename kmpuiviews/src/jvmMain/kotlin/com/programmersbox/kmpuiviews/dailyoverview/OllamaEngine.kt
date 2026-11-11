package com.programmersbox.kmpuiviews.dailyoverview

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaLLMProvider
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.koogintegration.AgentMaker
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_label_ollama
import otakuworld.kmpuiviews.generated.resources.do_setup_choose_ollama
import otakuworld.kmpuiviews.generated.resources.do_setup_ollama_not_running
import kotlin.time.Duration.Companion.seconds

/** Desktop engine: the AI Helper's Ollama model, used only while the local server answers. */
class OllamaEngine(
    private val agentMaker: AgentMaker,
    private val httpClient: HttpClient,
) : KoogSummaryEngine() {
    override val id = SummaryEngineId.Ollama
    override val onDevice = true

    override suspend fun label(): String =
        getString(Res.string.do_label_ollama, agentMaker.selection()?.model.orEmpty())

    // NeedsSetup rather than Unavailable when the server is down: Unavailable carries no reason to show.
    override suspend fun status(): EngineStatus = when {
        agentMaker.selection()?.company != OllamaLLMProvider.display ->
            EngineStatus.NeedsSetup(getString(Res.string.do_setup_choose_ollama))

        !isRunning() -> EngineStatus.NeedsSetup(getString(Res.string.do_setup_ollama_not_running))
        else -> EngineStatus.Ready
    }

    override suspend fun client(): Pair<LLMClient, LLModel>? =
        agentMaker.getAgentInfo()?.let { it.llmClient to it.model }

    // Real-time timeout on IO, so a test scheduler's virtual clock cannot cut the request short.
    private suspend fun isRunning(): Boolean = try {
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(2.seconds) {
                httpClient.get("${OllamaClient.DEFAULT_BASE_URL}/api/tags").status.isSuccess()
            }
        } == true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }
}
