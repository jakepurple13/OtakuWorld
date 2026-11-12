package com.programmersbox.kmpuiviews.domain.aitext

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Runs [block] in [scope], mapping deltas to Loading → Streaming → Done, exceptions to Error. Cancels the previous run. */
class AiTextRunner(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<AiTextState>(AiTextState.Idle)
    val state: StateFlow<AiTextState> = _state.asStateFlow()

    private var job: Job? = null

    fun run(engine: AiEngineInfo, block: () -> Flow<String>) {
        job?.cancel()
        _state.value = AiTextState.Loading
        job = scope.launch {
            try {
                val text = StringBuilder()
                block().collect { delta ->
                    text.append(delta)
                    _state.value = AiTextState.Streaming(text.toString(), engine)
                }
                _state.value = AiTextState.Done(text.toString(), engine)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AiTextState.Error(e.message ?: e::class.simpleName.orEmpty())
            }
        }
    }

    fun reset() {
        job?.cancel()
        job = null
        _state.value = AiTextState.Idle
    }
}
