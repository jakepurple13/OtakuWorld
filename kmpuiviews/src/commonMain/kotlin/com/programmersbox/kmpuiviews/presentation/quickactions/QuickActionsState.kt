package com.programmersbox.kmpuiviews.presentation.quickactions

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp

sealed interface ManualCheckState {
    data object Idle : ManualCheckState
    data class Running(val checked: Int?, val total: Int?) : ManualCheckState
    data class Finished(val outcome: CheckOutcome) : ManualCheckState
}

sealed interface CheckOutcome {
    data class Updates(val count: Int) : CheckOutcome
    data object NoUpdates : CheckOutcome
    data object SourcesFailed : CheckOutcome
}

sealed interface LastChecked {
    data object Never : LastChecked
    data class Ago(val minutes: Long) : LastChecked
}

private val activeStates = setOf("RUNNING", "ENQUEUED", "BLOCKED")

fun manualCheckState(
    work: List<WorkInfoKmp>,
    report: UpdateCheckReport?,
    sawRunning: Boolean,
    requestedAtMillis: Long = 0L,
): ManualCheckState {
    work.firstOrNull { it.state in activeStates }?.let {
        return ManualCheckState.Running(it.progress, it.max)
    }
    if (!sawRunning) return ManualCheckState.Idle
    // Finished work entries from earlier runs are ignored; only a report written by this run counts.
    // A run that ended without writing one failed.
    val outcome = when {
        report == null || report.startedAt < requestedAtMillis -> CheckOutcome.SourcesFailed
        report.hasProblems -> CheckOutcome.SourcesFailed
        report.updatedCount > 0 -> CheckOutcome.Updates(report.updatedCount)
        else -> CheckOutcome.NoUpdates
    }
    return ManualCheckState.Finished(outcome)
}

fun lastCheckedLabel(report: UpdateCheckReport?, nowMillis: Long): LastChecked =
    if (report == null) LastChecked.Never
    else LastChecked.Ago(((nowMillis - report.finishedAt) / 60_000L).coerceAtLeast(0))
