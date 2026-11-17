package com.programmersbox.kmpuiviews.presentation.notifications

import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiTask
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Finished update summaries, kept in memory for the app session, keyed by [checkKey]. */
class UpdateSummaryCache {
    /** A summary and the notification lines it covered. */
    data class Entry(val lines: Set<String>, val done: AiTextState.Done)

    private val entries = MutableStateFlow<Map<Long, Entry>>(emptyMap())

    operator fun get(check: Long): Entry? = entries.value[check]

    operator fun set(check: Long, entry: Entry) {
        entries.update { it + (check to entry) }
    }
}

/** A check and its "retry failed" run share `finishedAt`, so the retry time keys the newer summary. */
internal fun UpdateCheckReport.checkKey(): Long = retriedAt ?: finishedAt

/**
 * The AI summary atop Notifications. It covers the notifications from the latest update check, needs
 * at least [MIN_UPDATES] of them, and starts on its own. A summary is kept per check and redone only
 * when a notification it didn't cover shows up (the update worker writes rows one at a time after
 * saving the report). Removals never redo it.
 *
 * Not thread-safe: [scope] must be single-threaded, e.g. viewModelScope on Main.
 */
@OptIn(FlowPreview::class)
class UpdateSummaries(
    aiText: AiText,
    store: UpdateCheckReportStore,
    private val cache: UpdateSummaryCache,
    items: Flow<List<NotificationItem>>,
    private val scope: CoroutineScope,
) {
    private sealed interface Mode {
        data object Hidden : Mode
        data class Cached(val done: AiTextState.Done) : Mode
        data object Live : Mode
    }

    private data class Run(val check: Long, val lines: List<String>)

    private val task = AiTask(aiText, scope)
    private val mode = MutableStateFlow<Mode>(Mode.Hidden)
    private var run: Run? = null
    private var runJob: Job? = null

    /** Null when the card is hidden; otherwise Loading, Streaming, Done or Error. */
    val state: StateFlow<AiTextState?> = combine(mode, task.state) { mode, live ->
        when (mode) {
            Mode.Hidden -> null
            is Mode.Cached -> mode.done
            Mode.Live -> live
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch {
            combine(
                // Lets the worker's burst of row writes settle before deciding
                items.debounce(ITEMS_SETTLE_MILLIS),
                store.report,
                aiText.availability(AiFeature.UpdateSummary),
            ) { items, report, engine -> Triple(items, report, engine) }
                .collect { (items, report, engine) -> update(items, report, engine != null) }
        }
    }

    private fun update(items: List<NotificationItem>, report: UpdateCheckReport?, ready: Boolean) {
        if (report == null || !ready) return hide()
        val lines = items
            .filter { it.createdAt >= report.startedAt }
            .sortedByDescending { it.createdAt }
            .map { it.summaryText }
        if (lines.size < MIN_UPDATES) return hide()

        val check = report.checkKey()
        val covered = { known: Collection<String> -> known.containsAll(lines) }
        cache[check]?.takeIf { covered(it.lines) }?.let {
            stopRun()
            mode.value = Mode.Cached(it.done)
            return
        }
        mode.value = Mode.Live
        val current = run
        if (current != null && current.check == check && covered(current.lines)) return
        startRun(Run(check, lines))
    }

    private fun hide() {
        // Also stops a running generation: it holds the shared AI lock and may be loading Gemma
        stopRun()
        mode.value = Mode.Hidden
    }

    private fun startRun(next: Run) {
        stopRun()
        run = next
        // Its own job, so a new emission can't cancel it halfway through starting
        runJob = scope.launch {
            if (!task.start(AiFeature.UpdateSummary, next.lines)) {
                task.fail("No on-device AI engine is ready")
                return@launch
            }
            val result = task.state.first { it is AiTextState.Done || it is AiTextState.Error }
            if (result is AiTextState.Done) cache[next.check] = UpdateSummaryCache.Entry(next.lines.toSet(), result)
        }
    }

    private fun stopRun() {
        runJob?.cancel()
        runJob = null
        run = null
        task.reset()
    }

    /** Runs the current summary again after an error. */
    fun retry() {
        val last = run ?: return
        startRun(last)
    }

    private companion object {
        const val MIN_UPDATES = 2
        const val ITEMS_SETTLE_MILLIS = 400L
    }
}
