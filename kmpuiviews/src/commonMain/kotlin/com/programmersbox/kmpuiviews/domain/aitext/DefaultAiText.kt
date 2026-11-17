package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineResolver
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineChoice
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.platformAutomaticOrder
import com.programmersbox.kmpuiviews.utils.dispatchIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Bound as a Koin single, so [generationLock] is process-wide: only one generation runs at a
 * time across all features (Gemma loads a multi-GB model per call).
 */
class DefaultAiText(
    engines: List<SummaryEngine>,
    private val nanoTools: NanoTools?,
    order: List<SummaryEngineId> = platformAutomaticOrder,
    private val generationLock: Mutex = Mutex(),
) : AiText {
    // AI text stays on device: cloud and the non-AI template are never used, whatever the order says.
    private val onDeviceOrder = order - SummaryEngineId.Provider - SummaryEngineId.Template
    private val resolver = EngineResolver(engines, onDeviceOrder)
    private val withoutNano = EngineResolver(engines, onDeviceOrder - SummaryEngineId.Nano)

    override suspend fun engineFor(feature: AiFeature<*>, inputLength: Int): AiEngineInfo? =
        withContext(Dispatchers.IO) {
            resolve(feature, inputLength)?.let { engine ->
                AiEngineInfo(engine.label(), engine.onDevice, engine.destination())
            }
        }

    override fun <I> run(feature: AiFeature<I>, input: I): Flow<String> = flow {
        val prepared = feature.prepare(input)
        val engine = resolve(feature, prepared.length) ?: throw noEngine()
        val tools = nanoTools
        val task = feature.nanoTask
        generationLock.withLock {
            if (engine.id == SummaryEngineId.Nano && tools != null && task != null && tools.isReady(task, prepared.length)) {
                emitAll(tools.run(task, prepared))
            } else {
                emitAll(engine.generate(feature.system, feature.userPrompt(prepared)))
            }
        }
    }.dispatchIo()

    /** Null when nothing on-device is ready. Template means the resolver found nothing. */
    private suspend fun resolve(feature: AiFeature<*>, inputLength: Int): SummaryEngine? {
        val tooLongForNano = feature.nanoMaxChars?.let { inputLength > it } == true
        val r = if (tooLongForNano) withoutNano else resolver
        return r.resolve(SummaryEngineChoice.Automatic).takeIf { it.id != SummaryEngineId.Template }
    }

    private fun noEngine() = IllegalStateException("No on-device AI engine is ready")
}
