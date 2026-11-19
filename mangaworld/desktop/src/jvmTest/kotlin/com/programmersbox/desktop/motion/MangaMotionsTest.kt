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
