package com.programmersbox.kmpuiviews.presentation.notifications

import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiTask
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Finished update summaries keyed by the update check's `finishedAt`, kept in memory for the app session. */
class UpdateSummaryCache {
    private val entries = MutableStateFlow<Map<Long, AiTextState.Done>>(emptyMap())

    operator fun get(checkFinishedAt: Long): AiTextState.Done? = entries.value[checkFinishedAt]

    operator fun set(checkFinishedAt: Long, done: AiTextState.Done) {
        entries.update { it + (checkFinishedAt to done) }
    }
}

/**
 * The AI summary atop Notifications. It covers the notifications from the latest update check, needs
 * at least [MIN_UPDATES] of them, starts on its own, and is generated once per check.
 */
class UpdateSummaries(
    aiText: AiText,
    store: UpdateCheckReportStore,
    private val cache: UpdateSummaryCache,
    items: kotlinx.coroutines.flow.Flow<List<NotificationItem>>,
    private val scope: CoroutineScope,
) {
    private sealed interface Mode {
        data object Hidden : Mode
        data class Cached(val done: AiTextState.Done) : Mode
        data object Live : Mode
    }

    private val task = AiTask(aiText, scope)
    private val mode = MutableStateFlow<Mode>(Mode.Hidden)

    // The check the live task is summarizing, and its lines for a retry. Touched only inside [scope].
    private var liveCheck: Long? = null
    private var liveLines: List<String> = emptyList()

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
            combine(items, store.report, aiText.availability(AiFeature.UpdateSummary)) { items, report, engine ->
                Triple(items, report, engine)
            }.collectLatest { (items, report, engine) ->
                if (report == null || engine == null) {
                    mode.value = Mode.Hidden
                    return@collectLatest
                }
                val lines = items
                    .filter { it.createdAt >= report.startedAt }
                    .sortedByDescending { it.createdAt }
                    .map { it.summaryText }
                if (lines.size < MIN_UPDATES) {
                    mode.value = Mode.Hidden
                    return@collectLatest
                }
                val check = report.finishedAt
                cache[check]?.let {
                    mode.value = Mode.Cached(it)
                    return@collectLatest
                }
                mode.value = Mode.Live
                // Deleting a notification later doesn't restart the summary for the same check
                if (liveCheck != check) {
                    liveCheck = check
                    liveLines = lines
                    // The engine went away between the availability check and now: show Retry, not a shimmer
                    if (!task.start(AiFeature.UpdateSummary, lines)) task.fail("No on-device AI engine is ready")
                }
            }
        }
        scope.launch {
            task.state.collect { state ->
                val check = liveCheck
                if (state is AiTextState.Done && check != null) cache[check] = state
            }
        }
    }

    fun retry() {
        val lines = liveLines.takeIf { it.isNotEmpty() } ?: return
        scope.launch {
            if (!task.start(AiFeature.UpdateSummary, lines)) task.fail("No on-device AI engine is ready")
        }
    }

    private companion object {
        const val MIN_UPDATES = 2
    }
}
