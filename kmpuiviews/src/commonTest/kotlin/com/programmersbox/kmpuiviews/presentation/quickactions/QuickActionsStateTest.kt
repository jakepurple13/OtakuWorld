package com.programmersbox.kmpuiviews.presentation.quickactions

import com.programmersbox.kmpuiviews.domain.updatecheck.FailureKind
import com.programmersbox.kmpuiviews.domain.updatecheck.SourceFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickActionsStateTest {
    private fun work(state: String, p: Int? = null, m: Int? = null) =
        WorkInfoKmp(state, "", p, m, LocalDateTime(2026, 1, 1, 0, 0))

    private fun report(updated: Int, failures: Boolean = false) = UpdateCheckReport(
        startedAt = 0, finishedAt = 1, favoriteCount = 42, checkedCount = 42, updatedCount = updated,
        sourceFailures = if (failures) listOf(SourceFailure("MangaRead", FailureKind.TimedOut, null, "timeout")) else emptyList(),
        itemFailures = emptyList(),
    )

    @Test
    fun runningWithProgress() = assertEquals(
        ManualCheckState.Running(3, 42), manualCheckState(listOf(work("RUNNING", 3, 42)), null, sawRunning = false)
    )

    @Test
    fun enqueuedCountsAsRunning() = assertEquals(
        ManualCheckState.Running(null, null), manualCheckState(listOf(work("ENQUEUED")), null, false)
    )

    @Test
    fun idleWhenNothingRanThisSession() = assertEquals(
        ManualCheckState.Idle, manualCheckState(listOf(work("SUCCEEDED")), report(3), sawRunning = false)
    )

    @Test
    fun finishedWithUpdates() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.Updates(3)), manualCheckState(listOf(work("SUCCEEDED")), report(3), true)
    )

    @Test
    fun finishedNoUpdates() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.NoUpdates), manualCheckState(emptyList(), report(0), true)
    )

    @Test
    fun failuresWinOverUpdates() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.SourcesFailed),
        manualCheckState(listOf(work("SUCCEEDED")), report(3, failures = true), true)
    )

    @Test
    fun noReportAfterRunIsSourcesFailed() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.SourcesFailed), manualCheckState(listOf(work("SUCCEEDED")), null, true)
    )

    @Test
    fun staleReportIsSourcesFailed() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.SourcesFailed),
        manualCheckState(emptyList(), report(3), true, requestedAtMillis = 10)
    )

    @Test
    fun staleFailedWorkEntryIgnored() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.NoUpdates), manualCheckState(listOf(work("FAILED")), report(0), true)
    )

    @Test
    fun neverChecked() = assertEquals(LastChecked.Never, lastCheckedLabel(null, 1_000))

    @Test
    fun minutesAgo() = assertEquals(LastChecked.Ago(180), lastCheckedLabel(report(0).copy(finishedAt = 0), 180 * 60_000L))
}
