package com.programmersbox.kmpuiviews.testing

import com.programmersbox.kmpuiviews.domain.aitext.AiEngineInfo
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration

/**
 * The one test [AiText]. Every feature is ready with [engine] (or whatever [engineRule] says) and
 * replies with [output]. Pass `engine = null` for a screen with no AI.
 *
 * [availability] re-checks whenever [engine] is set or [changeSettings] is called, like the real
 * one does when an AI setting changes. It never polls on a timer, so `advanceUntilIdle` finishes.
 */
class FakeAiText(
    engine: AiEngineInfo? = ENGINE,
    /** Overrides [engine] when set; may return null. */
    var engineRule: (suspend (AiFeature<*>, Int) -> AiEngineInfo?)? = null,
    var output: (AiFeature<*>, Any?) -> Flow<String> = { _, _ -> flowOf("out") },
) : AiText {
    private val changes = MutableStateFlow(0)

    var engine: AiEngineInfo? = engine
        set(value) {
            field = value
            changeSettings()
        }

    // Thread-safe: some view model tests collect on real dispatchers and poll from the test thread.
    /** Every [run] call, in order. */
    val runs: MutableList<Pair<AiFeature<*>, Any?>> = CopyOnWriteArrayList()
    @Volatile var engineCalls = 0

    /** Makes every open [availability] flow re-check, as a settings change would. */
    fun changeSettings() = changes.update { it + 1 }

    override suspend fun engineFor(feature: AiFeature<*>, inputLength: Int): AiEngineInfo? {
        engineCalls++
        val rule = engineRule
        return if (rule != null) rule(feature, inputLength) else engine
    }

    override fun availability(feature: AiFeature<*>, inputLength: Int, retry: Duration): Flow<AiEngineInfo?> =
        changes.map { engineFor(feature, inputLength) }

    override fun <I> run(feature: AiFeature<I>, input: I): Flow<String> {
        runs += feature to input
        return output(feature, input)
    }

    /** Inputs passed to [run] for features of type [F]. */
    inline fun <reified F : AiFeature<*>> inputsFor(): List<Any?> = runs.filter { it.first is F }.map { it.second }

    companion object {
        val ENGINE = AiEngineInfo(label = "Fake", onDevice = true, destination = null)
    }
}
