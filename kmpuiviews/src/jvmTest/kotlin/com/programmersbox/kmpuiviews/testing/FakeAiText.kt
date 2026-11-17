package com.programmersbox.kmpuiviews.testing

import com.programmersbox.kmpuiviews.domain.aitext.AiEngineInfo
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The one test [AiText]. Every feature is ready with [engine] (or whatever [engineRule] says) and
 * replies with [output]. Pass `engine = null` for a screen with no AI.
 */
class FakeAiText(
    var engine: AiEngineInfo? = ENGINE,
    /** Overrides [engine] when set; may return null. */
    var engineRule: (suspend (AiFeature<*>, Int) -> AiEngineInfo?)? = null,
    var output: (AiFeature<*>, Any?) -> Flow<String> = { _, _ -> flowOf("out") },
) : AiText {
    // Thread-safe: some view model tests collect on real dispatchers and poll from the test thread.
    /** Every [run] call, in order. */
    val runs: MutableList<Pair<AiFeature<*>, Any?>> = CopyOnWriteArrayList()
    @Volatile var engineCalls = 0

    override suspend fun engineFor(feature: AiFeature<*>, inputLength: Int): AiEngineInfo? {
        engineCalls++
        val rule = engineRule
        return if (rule != null) rule(feature, inputLength) else engine
    }

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
