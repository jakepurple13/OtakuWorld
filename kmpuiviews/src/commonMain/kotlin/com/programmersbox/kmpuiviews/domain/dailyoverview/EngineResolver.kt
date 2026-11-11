package com.programmersbox.kmpuiviews.domain.dailyoverview

import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_setup_error

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
        byId.mapValues { it.value.safeStatus() }

    private suspend fun ready(id: SummaryEngineId): SummaryEngine? =
        byId[id]?.takeIf { it.safeStatus() == EngineStatus.Ready }

    /** A status check that throws (e.g. an unreadable model folder) counts as needing setup. */
    private suspend fun SummaryEngine.safeStatus(): EngineStatus = try {
        status()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        EngineStatus.NeedsSetup(getString(Res.string.do_setup_error))
    }
}
