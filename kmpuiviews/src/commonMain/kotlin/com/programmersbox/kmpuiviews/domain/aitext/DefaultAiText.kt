package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineResolver
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineChoice
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.platformAutomaticOrder
import com.programmersbox.kmpuiviews.utils.dispatchIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration

/**
 * Bound as a Koin single, so [generationLock] is process-wide: only one generation runs at a
 * time across all features (Gemma loads a multi-GB model per call).
 */
class DefaultAiText(
    private val engines: List<SummaryEngine>,
    private val nanoTools: NanoTools?,
    private val settings: AiSettings = AlwaysOnAiSettings,
    order: List<SummaryEngineId> = platformAutomaticOrder,
    private val generationLock: Mutex = Mutex(),
) : AiText {
    // AI text stays on device: cloud and the non-AI template are never used, whatever the order says.
    private val onDeviceOrder = order - SummaryEngineId.Provider - SummaryEngineId.Template

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

    /** Like the default, but starts over whenever an AI setting changes, so open screens follow the switches. */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun availability(feature: AiFeature<*>, inputLength: Int, retry: Duration): Flow<AiEngineInfo?> =
        combine(settings.enabled, settings.disabled, settings.preference) { _, _, _ -> }
            .flatMapLatest { pollAvailability(feature, inputLength, retry) }

    /**
     * Null when the AI settings turn [feature] off or nothing on-device is ready. Template means the
     * resolver found nothing. Settings are read on every call, so a switch flip applies right away.
     */
    private suspend fun resolve(feature: AiFeature<*>, inputLength: Int): SummaryEngine? {
        if (!settings.enabled.first()) return null
        val toggle = feature.toggle
        if (toggle != null && toggle in settings.disabled.first()) return null
        val tooLongForNano = feature.nanoMaxChars?.let { inputLength > it } == true
        var order = settings.preference.first().order(onDeviceOrder)
        if (tooLongForNano) order = order - SummaryEngineId.Nano
        return EngineResolver(engines, order).resolve(SummaryEngineChoice.Automatic)
            .takeIf { it.id != SummaryEngineId.Template }
    }

    private fun noEngine() = IllegalStateException("No on-device AI engine is ready")
}
