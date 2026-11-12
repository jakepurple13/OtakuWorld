package com.programmersbox.kmpuiviews.domain.aitext

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Per-chapter "Previously" recaps. Nothing generates until [request]; a finished recap is cached
 * for the chapter. There is no cancel API for collapsing; only leaving the chapter cancels.
 */
class ChapterRecaps(
    private val aiText: AiText,
    private val scope: CoroutineScope,
) {
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val states = mutableMapOf<String, MutableStateFlow<AiTextState>>()
    private val jobs = mutableMapOf<String, Job>()

    init {
        scope.launch { _available.value = aiText.engineFor(AiFeature.Recap) != null }
    }

    private fun flowFor(chapterUrl: String) = states.getOrPut(chapterUrl) { MutableStateFlow(AiTextState.Idle) }

    fun stateFor(chapterUrl: String): StateFlow<AiTextState> = flowFor(chapterUrl).asStateFlow()

    fun request(chapterUrl: String, loadPreviousText: suspend () -> String?) {
        val state = flowFor(chapterUrl)
        when (state.value) {
            AiTextState.Idle, is AiTextState.Error -> Unit
            else -> return
        }
        state.value = AiTextState.Loading
        jobs[chapterUrl] = scope.launch {
            try {
                // Check for an engine before fetching the previous chapter over the network
                if (aiText.engineFor(AiFeature.Recap) == null) {
                    state.value = AiTextState.Error("No on-device engine is ready")
                    return@launch
                }
                val text = loadPreviousText()
                if (text == null) {
                    state.value = AiTextState.Error("No previous chapter")
                    return@launch
                }
                val engine = aiText.engineFor(AiFeature.Recap, text.length)
                if (engine == null) {
                    state.value = AiTextState.Error("No on-device engine is ready")
                    return@launch
                }
                val out = StringBuilder()
                aiText.recap(AiTextPrompts.recapTail(text)).collect { delta ->
                    out.append(delta)
                    state.value = AiTextState.Streaming(out.toString(), engine)
                }
                state.value = AiTextState.Done(out.toString(), engine)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.value = AiTextState.Error(errorMessage(e))
            } finally {
                // Only drop our own entry; a newer request for this chapter may have replaced it
                if (jobs[chapterUrl] === coroutineContext[Job]) jobs.remove(chapterUrl)
            }
        }
    }

    fun onChapterChanged(newChapterUrl: String) {
        jobs.keys.filter { it != newChapterUrl }.forEach { url ->
            jobs.remove(url)?.cancel()
            val state = states[url] ?: return@forEach
            if (state.value !is AiTextState.Done) state.value = AiTextState.Idle
        }
    }
}
