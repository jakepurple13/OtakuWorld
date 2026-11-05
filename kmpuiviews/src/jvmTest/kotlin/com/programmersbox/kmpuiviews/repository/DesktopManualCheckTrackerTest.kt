package com.programmersbox.kmpuiviews.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopManualCheckTrackerTest {
    @Test
    fun startsEmpty() = assertEquals(emptyList(), DesktopManualCheckTracker().state.value)

    @Test
    fun secondStartRejectedWhileRunning() {
        val t = DesktopManualCheckTracker()
        assertTrue(t.tryStart())
        assertFalse(t.tryStart())
        assertEquals("RUNNING", t.state.value.single().state)
    }

    @Test
    fun progressIsReported() {
        val t = DesktopManualCheckTracker()
        t.tryStart()
        t.progress(42, 3, "MangaRead")
        val w = t.state.value.single()
        assertEquals(3, w.progress)
        assertEquals(42, w.max)
        assertEquals("MangaRead", w.source)
    }

    @Test
    fun failureEndsRunAndAllowsRestart() {
        val t = DesktopManualCheckTracker()
        t.tryStart()
        t.finish(success = false)
        assertEquals("FAILED", t.state.value.single().state)
        assertTrue(t.tryStart())
    }

    @Test
    fun successEndsRun() {
        val t = DesktopManualCheckTracker()
        t.tryStart()
        t.finish(true)
        assertEquals("SUCCEEDED", t.state.value.single().state)
    }
}
