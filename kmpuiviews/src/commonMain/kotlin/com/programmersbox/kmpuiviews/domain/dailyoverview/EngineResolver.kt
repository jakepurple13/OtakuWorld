package com.programmersbox.kmpuiviews.domain.dailyoverview

/** Automatic order for this platform. Template is always the final fallback. */
expect val platformAutomaticOrder: List<SummaryEngineId>

class EngineResolver(
    engines: List<SummaryEngine>,
    val automaticOrder: List<SummaryEngineId>,
) {
    private val byId = engines.associateBy { it.id }
    private val template = byId[SummaryEngineId.Template] ?: TemplateEngine()

    suspend fun resolve(choice: SummaryEngineChoice): SummaryEngine = when (choice) {
        SummaryEngineChoice.Automatic -> automaticOrder.firstNotNullOfOrNull { ready(it) }
        else -> ready(SummaryEngineId.valueOf(choice.name))
    } ?: template

    /** The registered engine for [id], or Template when this platform does not have it. */
    fun engine(id: SummaryEngineId): SummaryEngine = byId[id] ?: template

    suspend fun available(): Map<SummaryEngineId, EngineStatus> =
        byId.mapValues { it.value.status() }

    private suspend fun ready(id: SummaryEngineId): SummaryEngine? =
        byId[id]?.takeIf { it.status() == EngineStatus.Ready }
}
