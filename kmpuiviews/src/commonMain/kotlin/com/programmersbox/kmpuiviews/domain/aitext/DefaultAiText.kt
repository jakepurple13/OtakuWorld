package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineResolver
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource
import com.programmersbox.telemetry.events.AiTaskRun
import com.programmersbox.telemetry.Buckets
import com.programmersbox.telemetry.NoOpAnalytics
import com.programmersbox.telemetry.Analytics
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
    private val analytics: Analytics = NoOpAnalytics,
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
        val featureName = feature.telemetryName
        val started = TimeSource.Monotonic.markNow()
        fun log(engine: String, outcome: String) =
            analytics.log(AiTaskRun(featureName, engine, outcome, Buckets.durationMs(started.elapsedNow().inWholeMilliseconds)))

        val prepared = feature.prepare(input)
        val engine = resolve(feature, prepared.length)
            ?: run { log("none", AiTaskRun.UNAVAILABLE); throw noEngine() }
        val engineName = engine.id.name.lowercase()
        val tools = nanoTools
        val task = feature.nanoTask
        try {
            generationLock.withLock {
                if (engine.id == SummaryEngineId.Nano && tools != null && task != null && tools.isReady(task, prepared.length)) {
                    emitAll(tools.run(task, prepared))
                } else {
                    emitAll(engine.generate(feature.system, feature.userPrompt(prepared)))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(engineName, AiTaskRun.ERROR)
            throw e
        }
        log(engineName, AiTaskRun.SUCCESS)
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
