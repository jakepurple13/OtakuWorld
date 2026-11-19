package com.programmersbox.desktop.motion

import com.programmersbox.kmpuiviews.motion.MotionContract
import com.programmersbox.kmpuiviews.motion.MotionPhase
import kotlin.test.Test
import kotlin.test.assertEquals

class MangaMotionsTest {
    private val catalog = mangaMotionCatalog()

    @Test
    fun launchesObeyContract() = catalog.launch.forEach {
        assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Launch), it.key)
    }

    @Test
    fun quitsObeyContract() = catalog.quit.forEach {
        assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Quit), it.key)
    }

    /** Portrait and very tall hosts: vertical moves must still clear the window. */
    @Test
    fun tallWindowsObeyContract() = listOf(200 to 320, 200 to 500).forEach { (w, h) ->
        catalog.launch.forEach {
            assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Launch, w, h), "${it.key} ${w}x$h")
        }
        catalog.quit.forEach {
            assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Quit, w, h), "${it.key} ${w}x$h")
        }
    }

    @Test
    fun sushiIsDefault() {
        assertEquals("sushi-unroll", catalog.launch.first().key)
        assertEquals("sushi-roll-up", catalog.quit.first().key)
    }

    @Test
    fun durationsMatchMockup() {
        assertEquals(3200, SushiUnroll.durationMillis)
        assertEquals(3100, SushiRollUp.durationMillis)
    }
}
