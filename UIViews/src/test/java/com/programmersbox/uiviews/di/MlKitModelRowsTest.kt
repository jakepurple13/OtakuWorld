package com.programmersbox.uiviews.di

import com.programmersbox.kmpuiviews.domain.ModelState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MlKitModelRowsTest {
    private fun rows(
        downloaded: Set<String> = emptySet(),
        downloading: Map<String, ModelState> = emptyMap(),
        sizes: (String) -> Long? = { null },
        lastUsed: Map<String, Long> = emptyMap(),
    ) = mlKitRows(downloaded, downloading, sizes, lastUsed).associateBy { it.language.code }

    @Test
    fun `english is required`() {
        val r = rows()
        assertTrue(r.getValue("en").required)
        assertFalse(r.getValue("fr").required)
    }

    @Test
    fun `unknown size uses 30MB estimate`() {
        val r = rows(downloaded = setOf("fr", "de"), sizes = { if (it == "de") 5L else null })
        assertEquals(30_000_000L, r.getValue("fr").sizeBytes)
        assertTrue(r.getValue("fr").sizeIsEstimate)
        assertEquals(5L, r.getValue("de").sizeBytes)
        assertFalse(r.getValue("de").sizeIsEstimate)
    }

    @Test
    fun `downloading state overrides downloaded`() {
        val r = rows(downloaded = setOf("fr"), downloading = mapOf("fr" to ModelState.Downloading(null)))
        assertEquals(ModelState.Downloading(null), r.getValue("fr").state)
        assertEquals(ModelState.Downloaded, rows(downloaded = setOf("de")).getValue("de").state)
        assertEquals(ModelState.NotDownloaded, r.getValue("de").state)
    }

    @Test
    fun `lastUsed is mapped`() {
        val r = rows(lastUsed = mapOf("fr" to 42L))
        assertEquals(42L, r.getValue("fr").lastUsedMillis)
        assertNull(r.getValue("de").lastUsedMillis)
    }
}
