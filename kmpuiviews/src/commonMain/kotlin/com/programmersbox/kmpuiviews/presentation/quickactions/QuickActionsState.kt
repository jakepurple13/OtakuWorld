package com.programmersbox.kmpuiviews.presentation.quickactions

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp

sealed interface ManualCheckState {
    data object Idle : ManualCheckState

    /** [waitingToStart]: the work is queued (for example, waiting for a network) and has not run yet. */
    data class Running(val checked: Int?, val total: Int?, val waitingToStart: Boolean = false) : ManualCheckState
    data class Finished(val outcome: CheckOutcome) : ManualCheckState
}

sealed interface CheckOutcome {
    data class Updates(val count: Int) : CheckOutcome
    data object NoUpdates : CheckOutcome
    data object SourcesFailed : CheckOutcome

    /** The run ended without writing a report, so there is nothing to view. */
    data object CheckFailed : CheckOutcome
}

sealed interface LastChecked {
    data object Never : LastChecked
    data class Ago(val minutes: Long) : LastChecked
}

/** Work states that mean a check is queued or running. Matches Android's `WorkInfo.State.toString()`. */
internal val activeStates = setOf("RUNNING", "ENQUEUED", "BLOCKED")

/**
 * @param sawRunning work for the requested run was observed active.
 * @param requestedAtMillis when the user requested a check; null when they haven't.
 * @param everRunning the requested run's work was observed RUNNING at least once.
 */
fun manualCheckState(
    work: List<WorkInfoKmp>,
    report: UpdateCheckReport?,
    sawRunning: Boolean,
    requestedAtMillis: Long? = null,
    everRunning: Boolean = false,
): ManualCheckState {
    work.firstOrNull { it.state in activeStates }?.let {
        return ManualCheckState.Running(
            checked = it.progress,
            total = it.max,
            waitingToStart = it.state == "ENQUEUED" && !everRunning,
        )
    }
    // A fast run can finish before any active entry is observed; a report written by it still counts.
    val freshReport = report != null && requestedAtMillis != null && report.startedAt >= requestedAtMillis
    if (!sawRunning && !freshReport) return ManualCheckState.Idle
    // Finished work entries from earlier runs are ignored; only a report written by this run counts.
    // A run that ended without writing one failed, and there is no report to view.
    val outcome = when {
        report == null || report.startedAt < (requestedAtMillis ?: 0L) -> CheckOutcome.CheckFailed
        report.hasProblems -> CheckOutcome.SourcesFailed
        report.updatedCount > 0 -> CheckOutcome.Updates(report.updatedCount)
        else -> CheckOutcome.NoUpdates
    }
    return ManualCheckState.Finished(outcome)
}

/** [finishedAtMillis] is the last report's finish time; null when no check has run. */
fun lastCheckedLabel(finishedAtMillis: Long?, nowMillis: Long): LastChecked =
    if (finishedAtMillis == null) LastChecked.Never
    else LastChecked.Ago(((nowMillis - finishedAtMillis) / 60_000L).coerceAtLeast(0))
