package com.programmersbox.kmpuiviews.dailyoverview

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.litert.LiteRTLLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaLLMProvider
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.koogintegration.AgentMaker
import com.programmersbox.koogintegration.NoLLMProvider
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_engine_resolved
import otakuworld.kmpuiviews.generated.resources.do_label_via_koog
import otakuworld.kmpuiviews.generated.resources.do_setup_choose_provider

/** The AI Helper's cloud provider. Explicit opt-in only: Automatic never resolves to it. */
class ProviderEngine(
    private val agentMaker: AgentMaker,
) : KoogSummaryEngine() {
    override val id = SummaryEngineId.Provider
    override val onDevice = false

    private val notCloud = setOf(LiteRTLLMProvider.display, OllamaLLMProvider.display, NoLLMProvider.display)

    override suspend fun label(): String =
        getString(Res.string.do_label_via_koog, agentMaker.selection()?.company.orEmpty())

    /** "Google · gemini-2.5-flash". */
    override suspend fun shortName(): String =
        agentMaker.selection()?.let { getString(Res.string.do_engine_resolved, it.company, it.model) } ?: label()

    override suspend fun destination(): String? = agentMaker.selection()?.company

    override suspend fun status(): EngineStatus {
        val company = agentMaker.selection()?.company
        return if (company != null && company !in notCloud) EngineStatus.Ready
        else EngineStatus.NeedsSetup(getString(Res.string.do_setup_choose_provider))
    }

    override suspend fun client(): Pair<LLMClient, LLModel>? =
        agentMaker.getAgentInfo()?.let { it.llmClient to it.model }
}
