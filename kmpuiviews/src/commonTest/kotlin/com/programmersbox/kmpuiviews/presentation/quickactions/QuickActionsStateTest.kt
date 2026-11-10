package com.programmersbox.kmpuiviews.presentation.quickactions

import com.programmersbox.kmpuiviews.domain.updatecheck.FailureKind
import com.programmersbox.kmpuiviews.domain.updatecheck.SourceFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickActionsStateTest {
    @Test
    fun groupsAndOrder() {
        assertEquals(
            listOf(QuickAction.CheckForUpdates, QuickAction.ScanQrCode),
            QuickAction.entries.filter { it.group == QuickActionGroup.Tools }
        )
        assertEquals(
            listOf(
                QuickAction.GlobalSearch,
                QuickAction.Lists,
                QuickAction.SavedNotifications,
                QuickAction.BackgroundTasks,
                QuickAction.UpdateErrors
            ),
            QuickAction.entries.filter { it.group == QuickActionGroup.GoTo }
        )
    }

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
        ManualCheckState.Running(null, null, waitingToStart = true), manualCheckState(listOf(work("ENQUEUED")), null, false)
    )

    @Test
    fun requeuedAfterRunningIsNotWaiting() = assertEquals(
        ManualCheckState.Running(null, null, waitingToStart = false),
        manualCheckState(listOf(work("ENQUEUED")), null, sawRunning = true, everRunning = true),
    )

    @Test
    fun runningIsNotWaiting() = assertEquals(
        ManualCheckState.Running(1, 2, waitingToStart = false),
        manualCheckState(listOf(work("RUNNING", 1, 2)), null, sawRunning = true),
    )

    @Test
    fun freshReportFinishesWithoutSeenActiveWork() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.Updates(3)),
        manualCheckState(emptyList(), report(3).copy(startedAt = 10), sawRunning = false, requestedAtMillis = 10),
    )

    @Test
    fun staleReportWithoutSeenActiveWorkStaysIdle() = assertEquals(
        ManualCheckState.Idle,
        manualCheckState(emptyList(), report(3).copy(startedAt = 9), sawRunning = false, requestedAtMillis = 10),
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
    fun noReportAfterRunIsCheckFailed() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.CheckFailed), manualCheckState(listOf(work("SUCCEEDED")), null, true)
    )

    @Test
    fun staleReportIsCheckFailed() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.CheckFailed),
        manualCheckState(emptyList(), report(3), true, requestedAtMillis = 10)
    )

    @Test
    fun staleFailedWorkEntryIgnored() = assertEquals(
        ManualCheckState.Finished(CheckOutcome.NoUpdates), manualCheckState(listOf(work("FAILED")), report(0), true)
    )

    @Test
    fun neverChecked() = assertEquals(LastChecked.Never, lastCheckedLabel(null, 1_000))

    @Test
    fun minutesAgo() = assertEquals(LastChecked.Ago(180), lastCheckedLabel(0, 180 * 60_000L))
}
