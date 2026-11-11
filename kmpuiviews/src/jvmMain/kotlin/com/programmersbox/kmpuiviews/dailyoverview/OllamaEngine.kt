package com.programmersbox.kmpuiviews.dailyoverview

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.koogintegration.AgentMaker
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_label_ollama
import otakuworld.kmpuiviews.generated.resources.do_setup_choose_ollama
import otakuworld.kmpuiviews.generated.resources.do_setup_ollama_not_running
import otakuworld.kmpuiviews.generated.resources.do_setup_pick_ollama_model
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
        // LLMProvider.Ollama, not OllamaLLMProvider: touching the subclass first leaves Koog's LLMProvider.Ollama null.
        agentMaker.selection()?.company != LLMProvider.Ollama.display ->
            EngineStatus.NeedsSetup(getString(Res.string.do_setup_choose_ollama))

        else -> {
            val installed = installedModels()
            when {
                installed == null -> EngineStatus.NeedsSetup(getString(Res.string.do_setup_ollama_not_running))
                !installed.hasModel(agentMaker.selection()?.model.orEmpty()) ->
                    EngineStatus.NeedsSetup(getString(Res.string.do_setup_pick_ollama_model))

                else -> EngineStatus.Ready
            }
        }
    }

    public override suspend fun client(): Pair<LLMClient, LLModel>? =
        agentMaker.getAgentInfo()?.let { it.llmClient to it.model }

    // Real-time timeout on IO, so a test scheduler's virtual clock cannot cut the request short.
    /** Names of the models the local server has, or null when it does not answer. */
    private suspend fun installedModels(): List<String>? = try {
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(2.seconds) {
                val response = httpClient.get("${OllamaClient.DEFAULT_BASE_URL}/api/tags")
                if (!response.status.isSuccess()) return@withTimeoutOrNull null
                Json.parseToJsonElement(response.bodyAsText()).jsonObject["models"]
                    ?.jsonArray
                    ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }
                    .orEmpty()
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    // Ollama treats "name" as "name:latest".
    private fun List<String>.hasModel(selected: String): Boolean =
        selected in this || (":" !in selected && "$selected:latest" in this)
}
