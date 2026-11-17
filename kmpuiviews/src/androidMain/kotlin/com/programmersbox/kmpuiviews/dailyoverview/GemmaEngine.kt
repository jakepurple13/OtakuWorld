package com.programmersbox.kmpuiviews.dailyoverview

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.litert.LiteRTLLMProvider
import ai.koog.prompt.llm.LLModel
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.LocalModel
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.liteRtDisplayName
import com.programmersbox.koogintegration.AgentMaker
import com.programmersbox.koogintegration.PlatformAgents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.io.File
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_label_on_device
import otakuworld.kmpuiviews.generated.resources.do_setup_download_model

/** On-device Gemma through LiteRT, using a model the AI Helper already downloaded. */
class GemmaEngine(
    private val agentMaker: AgentMaker,
    private val platformAgents: PlatformAgents,
) : KoogSummaryEngine(), LocalModel {
    override val id = SummaryEngineId.Gemma
    override val onDevice = true

    // LiteRTLLMClient has no executeStreaming in this Koog version.
    override val streams = false

    override suspend fun label(): String =
        getString(Res.string.do_label_on_device, model()?.displayName() ?: "Gemma")

    override suspend fun shortName(): String = model()?.displayName() ?: "Gemma"

    override suspend fun status(): EngineStatus =
        if (model() != null) EngineStatus.Ready
        else EngineStatus.NeedsSetup(getString(Res.string.do_setup_download_model))

    override suspend fun client(): Pair<LLMClient, LLModel>? =
        model()?.let { platformAgents.liteRtClient() to it }

    override suspend fun currentFile(): File? =
        model()?.let { File(platformAgents.liteRtModelsDir, it.id) }?.takeIf { it.exists() }

    override suspend fun verify(file: File) = withContext(Dispatchers.IO) {
        val prompt = prompt("verify-model") {
            system("Reply with one word.")
            user("Say OK.")
        }
        // Koog loads modelsPath/model.id, so point it at the file's own folder
        platformAgents.liteRtClient(file.parentFile!!.absolutePath).use { client ->
            client.execute(prompt, platformAgents.textModel(file.name))
        }
        Unit
    }

    /** The AI Helper's LiteRT model when LiteRT is selected and downloaded, else the newest file. */
    private suspend fun model(): LLModel? {
        // Lists the model folder on disk.
        val downloaded = withContext(Dispatchers.IO) { platformAgents.downloadedLiteRtModels() }
        val selection = agentMaker.selection()
        val selected = selection
            ?.takeIf { it.company == LiteRTLLMProvider.display }
            ?.let { s -> downloaded.find { it.id == s.model } }
        return selected ?: downloaded.firstOrNull()
    }

    private fun LLModel.displayName(): String = liteRtDisplayName(id)
}
