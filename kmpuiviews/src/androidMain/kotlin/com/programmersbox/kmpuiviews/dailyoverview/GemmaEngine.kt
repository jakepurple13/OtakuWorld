package com.programmersbox.kmpuiviews.dailyoverview

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.litert.LiteRTLLMProvider
import ai.koog.prompt.llm.LLModel
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.koogintegration.AgentMaker
import com.programmersbox.koogintegration.PlatformAgents
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_label_on_device
import otakuworld.kmpuiviews.generated.resources.do_setup_download_model

/** On-device Gemma through LiteRT, using a model the AI Helper already downloaded. */
class GemmaEngine(
    private val agentMaker: AgentMaker,
    private val platformAgents: PlatformAgents,
) : KoogSummaryEngine() {
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

    /** The AI Helper's LiteRT model when LiteRT is selected and downloaded, else the first download. */
    private suspend fun model(): LLModel? {
        val downloaded = platformAgents.downloadedLiteRtModels()
        val selection = agentMaker.selection()
        val selected = selection
            ?.takeIf { it.company == LiteRTLLMProvider.display }
            ?.let { s -> downloaded.find { it.id == s.model } }
        return selected ?: downloaded.firstOrNull()
    }

    /** "gemma-4-E2B-it.litertlm" -> "Gemma 4 E2B". */
    private fun LLModel.displayName(): String = id
        .removeSuffix(".litertlm")
        .removeSuffix("-it")
        .split('-', '_')
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
}
