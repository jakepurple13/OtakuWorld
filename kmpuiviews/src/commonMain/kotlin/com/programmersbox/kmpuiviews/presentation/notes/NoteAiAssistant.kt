package com.programmersbox.kmpuiviews.presentation.notes

import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.AiTextPrompts
import com.programmersbox.kmpuiviews.domain.aitext.AiTextRunner
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.aitext.DiffSpan
import com.programmersbox.kmpuiviews.domain.aitext.RewriteStyle
import com.programmersbox.kmpuiviews.domain.aitext.WordDiff
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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
    diffDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private class Pending(val original: String, val label: String, val diffed: Boolean)

    private val runner = AiTextRunner(scope)
    private val pending = MutableStateFlow<Pending?>(null)
    private val _available = MutableStateFlow(false)
    private val _toolsEnabled = MutableStateFlow(false)
    private val _disabledReason = MutableStateFlow<String?>(null)

    // Resolved once per screen open by resolveEngines, not per edit
    private var anyEngine = false
    private var engineForLongText = false
    private var lastText = ""

    /** False until an engine is known to exist; the controls stay hidden while it is false. */
    val available: StateFlow<Boolean> = _available
    val toolsEnabled: StateFlow<Boolean> = _toolsEnabled
    val disabledReason: StateFlow<String?> = _disabledReason

    val preview: StateFlow<NotePreview?> = combine(pending, runner.state) { pending, state ->
        pending?.let {
            val shown = when {
                state is AiTextState.Idle -> AiTextState.Loading
                // A blank result would wipe the note if accepted
                state is AiTextState.Done && state.text.isBlank() -> AiTextState.Error("Empty result")
                else -> state
            }
            val diff = if (it.diffed && shown is AiTextState.Done) WordDiff.diff(it.original, shown.text) else null
            NotePreview(it.original, shown, diff, it.label)
        }
    }.flowOn(diffDispatcher).stateIn(scope, SharingStarted.Eagerly, null)

    private var resolveJob: Job? = null
    private var runJob: Job? = null

    /** Checks which engines are ready. Call once when the note screen opens. */
    suspend fun resolveEngines(text: String) {
        lastText = text
        // Each check can query the on-device model's status, so run them side by side
        val (proofread, rewrite, proofreadLong, rewriteLong) = coroutineScope {
            listOf(
                async { aiText.engineFor(AiFeature.Proofread, 0) != null },
                async { aiText.engineFor(AiFeature.Rewrite, 0) != null },
                async { aiText.engineFor(AiFeature.Proofread, AiTextPrompts.NANO_TEXT_LIMIT + 1) != null },
                async { aiText.engineFor(AiFeature.Rewrite, AiTextPrompts.NANO_TEXT_LIMIT + 1) != null },
            ).awaitAll()
        }
        anyEngine = proofread || rewrite
        engineForLongText = anyEngine && (proofreadLong || rewriteLong)
        _available.value = anyEngine
        onTextChanged(lastText)
    }

    /** Cheap, per edit: only compares the length with what the resolved engines can take. */
    fun onTextChanged(text: String) {
        lastText = text
        val tooLong = text.length > AiTextPrompts.NANO_TEXT_LIMIT && !engineForLongText
        _toolsEnabled.value = anyEngine && text.isNotBlank() && !tooLong
        _disabledReason.value = if (anyEngine && text.isNotBlank() && tooLong) TOO_LONG else null
    }

    fun fixGrammar(text: String) =
        start(text, AiFeature.Proofread, Pending(text, FIX_GRAMMAR_LABEL, diffed = true)) { aiText.proofread(text) }

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
        const val FIX_GRAMMAR_LABEL = "Fix grammar"
        const val TOO_LONG = "Note is too long for on-device tools"
    }
}
