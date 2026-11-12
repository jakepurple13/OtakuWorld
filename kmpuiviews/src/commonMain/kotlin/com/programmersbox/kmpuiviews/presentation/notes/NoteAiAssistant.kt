package com.programmersbox.kmpuiviews.presentation.notes

import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.AiTextPrompts
import com.programmersbox.kmpuiviews.domain.aitext.AiTextRunner
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.aitext.DiffSpan
import com.programmersbox.kmpuiviews.domain.aitext.RewriteStyle
import com.programmersbox.kmpuiviews.domain.aitext.WordDiff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotePreview(
    val original: String,
    val state: AiTextState,
    val diff: List<DiffSpan>?,
    val label: String,
)

/** On-device writing tools for a note: fix grammar and rewrite, each shown as a preview to accept or discard. */
class NoteAiAssistant(
    private val aiText: AiText,
    private val scope: CoroutineScope,
) {
    private class Pending(val original: String, val label: String, val diffed: Boolean)

    private val runner = AiTextRunner(scope)
    private val pending = MutableStateFlow<Pending?>(null)
    private val _toolsEnabled = MutableStateFlow(false)
    private val _disabledReason = MutableStateFlow<String?>(null)

    val toolsEnabled: StateFlow<Boolean> = _toolsEnabled
    val disabledReason: StateFlow<String?> = _disabledReason

    val preview: StateFlow<NotePreview?> = combine(pending, runner.state) { pending, state ->
        pending?.let {
            val shown = if (state is AiTextState.Idle) AiTextState.Loading else state
            val diff = if (it.diffed && shown is AiTextState.Done) WordDiff.diff(it.original, shown.text) else null
            NotePreview(it.original, shown, diff, it.label)
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private var availabilityJob: Job? = null
    private var runJob: Job? = null

    fun onTextChanged(text: String) {
        availabilityJob?.cancel()
        if (text.isBlank()) {
            _toolsEnabled.value = false
            _disabledReason.value = null
            return
        }
        availabilityJob = scope.launch {
            val available = aiText.engineFor(AiFeature.Proofread, text.length) != null ||
                    aiText.engineFor(AiFeature.Rewrite, text.length) != null
            _toolsEnabled.value = available
            _disabledReason.value = if (!available && text.length > AiTextPrompts.NANO_TEXT_LIMIT) TOO_LONG else null
        }
    }

    fun fixGrammar(text: String) = start(text, AiFeature.Proofread, Pending(text, "Fix grammar", diffed = true)) {
        aiText.proofread(text)
    }

    fun rewrite(text: String, style: RewriteStyle) =
        start(text, AiFeature.Rewrite, Pending(text, style.name, diffed = false)) { aiText.rewrite(text, style) }

    /** The generated text once it is done, or null when there is nothing to accept. Clears the preview. */
    fun accept(): String? {
        val done = preview.value?.state as? AiTextState.Done ?: return null
        discard()
        return done.text
    }

    fun discard() {
        runJob?.cancel()
        runJob = null
        runner.reset()
        pending.value = null
    }

    private fun start(
        text: String,
        feature: AiFeature,
        entry: Pending,
        block: () -> Flow<String>,
    ) {
        runJob?.cancel()
        runner.reset()
        pending.value = entry
        runJob = scope.launch {
            val engine = aiText.engineFor(feature, text.length)
            ensureActive()
            if (engine == null) {
                pending.value = null
                return@launch
            }
            runner.run(engine, block)
        }
    }

    companion object {
        const val TOO_LONG = "Note is too long for on-device tools"
    }
}
