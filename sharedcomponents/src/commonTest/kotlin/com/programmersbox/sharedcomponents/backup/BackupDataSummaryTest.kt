package com.programmersbox.sharedcomponents.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackupDataSummaryTest {
    @Test
    fun `default summary has no fields set`() {
        val summary = BackupDataSummary()
        assertNull(summary.itemCount)
        assertNull(summary.sizeBytes)
        assertNull(summary.lastModified)
        assertEquals(emptyList(), summary.details)
    }

    @Test
    fun `item result defaults error to null`() {
        val result = ItemResult(key = "favorites.json", success = true)
        assertNull(result.error)
    }
}
