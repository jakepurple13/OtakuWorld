package com.programmersbox.kmpuiviews.motion

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MotionRunTest {
    @Test
    fun skipFinishes() {
        val r = MotionRun(FadeInMotion)
        r.skip()
        assertEquals(450, r.timeMillis)
        assertTrue(r.finished)
    }

    @Test
    fun offIsFinishedAtOnce() = assertTrue(MotionRun(OffMotion).finished)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playEndsOnce() = runComposeUiTest {
        var ends = 0
        val r = MotionRun(FadeInMotion)
        mainClock.autoAdvance = false
        setContent { r.Play { ends++ } }
        mainClock.advanceTimeBy(1_000)
        assertTrue(r.finished)
        assertEquals(1, ends)
    }
}
