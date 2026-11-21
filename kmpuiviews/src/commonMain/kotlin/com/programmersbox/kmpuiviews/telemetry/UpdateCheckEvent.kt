package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.telemetry.Buckets
import com.programmersbox.telemetry.events.UpdateCheckFinished

/** The update_check_finished event for a finished run; null when the run stored no report (e.g. a retry with nothing to do). */
fun updateCheckEvent(report: UpdateCheckReport?, trigger: String): UpdateCheckFinished? = report?.let {
    UpdateCheckFinished(
        trigger = trigger,
        itemsChecked = Buckets.count(it.checkedCount),
        updatesFound = Buckets.count(it.updatedCount),
        sourcesFailed = it.sourceFailures.size,
        duration = Buckets.durationMs(it.finishedAt - it.startedAt),
    )
}

/** What started an update check, from the worker's input flags. */
fun updateCheckTrigger(checkAll: Boolean, retryFailed: Boolean): String = when {
    retryFailed -> UpdateCheckFinished.RETRY
    checkAll -> UpdateCheckFinished.MANUAL
    else -> UpdateCheckFinished.PERIODIC
}
