package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.telemetry.events.UpdateCheckFinished
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateCheckEventTest {
    private fun report(checked: Int, updated: Int, durationMs: Long) = UpdateCheckReport(
        startedAt = 1_000,
        finishedAt = 1_000 + durationMs,
        favoriteCount = checked,
        checkedCount = checked,
        updatedCount = updated,
        sourceFailures = emptyList(),
        itemFailures = emptyList(),
    )

    @Test
    fun `report becomes a bucketed event`() = assertEquals(
        UpdateCheckFinished(UpdateCheckFinished.PERIODIC, itemsChecked = "21+", updatesFound = "1-5", sourcesFailed = 0, duration = "30s-2m"),
        updateCheckEvent(report(checked = 40, updated = 2, durationMs = 45_000), UpdateCheckFinished.PERIODIC),
    )

    @Test
    fun `no report means no event`() = assertNull(updateCheckEvent(null, UpdateCheckFinished.RETRY))

    @Test
    fun `trigger comes from the worker input`() {
        assertEquals(UpdateCheckFinished.RETRY, updateCheckTrigger(checkAll = true, retryFailed = true))
        assertEquals(UpdateCheckFinished.MANUAL, updateCheckTrigger(checkAll = true, retryFailed = false))
        assertEquals(UpdateCheckFinished.PERIODIC, updateCheckTrigger(checkAll = false, retryFailed = false))
    }
}
