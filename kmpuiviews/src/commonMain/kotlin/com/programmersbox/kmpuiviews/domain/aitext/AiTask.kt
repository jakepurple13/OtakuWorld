package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.logFirebaseMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One AI generation slot for a screen: resolves an engine, runs a feature in [scope], and maps the
 * deltas to Loading → Streaming → Done (with [AiFeature.finish] applied), or exceptions to Error.
 */
class AiTask(
    private val aiText: AiText,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<AiTextState>(AiTextState.Idle)
    val state: StateFlow<AiTextState> = _state.asStateFlow()

    private var job: Job? = null

    /**
     * Resolves an engine for [input] in the caller's coroutine. Returns false, leaving the state as
     * it was, when none is ready. Otherwise cancels any previous run, launches this one in [scope],
     * and returns true. Cancelling the caller before the engine is resolved starts nothing.
     */
    suspend fun <I> start(feature: AiFeature<I>, input: I): Boolean {
        val engine = aiText.engineFor(feature, feature.prepare(input).length) ?: return false
        currentCoroutineContext().ensureActive()
        job?.cancel()
        _state.value = AiTextState.Loading
        job = scope.launch {
            try {
                val text = StringBuilder()
                aiText.run(feature, input).collect { delta ->
                    text.append(delta)
                    _state.value = AiTextState.Streaming(text.toString(), engine)
                }
                _state.value = AiTextState.Done(feature.finish(text.toString()), engine)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AiTextState.Error(errorMessage(e))
            }
        }
        return true
    }

    /** Shows [message] as an Error (with Retry) without running anything, e.g. when [start] found no engine. */
    fun fail(message: String) {
        job?.cancel()
        job = null
        _state.value = AiTextState.Error(message)
    }

    fun reset() {
        job?.cancel()
        job = null
        _state.value = AiTextState.Idle
    }
}

/**
 * Logs [e] and returns its text for [AiTextState.Error.message]. That message is for logs only;
 * the UI always shows the localized `ai_error` string, never raw engine text.
 */
internal fun errorMessage(e: Exception): String =
    (e.message ?: e::class.simpleName ?: "Unknown error").also { logFirebaseMessage("AI text failed: $it") }
