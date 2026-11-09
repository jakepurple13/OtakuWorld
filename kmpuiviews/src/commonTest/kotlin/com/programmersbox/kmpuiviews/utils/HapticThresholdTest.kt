package com.programmersbox.kmpuiviews.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HapticThresholdTest {

    @Test fun firesOnUpwardCrossing() = assertTrue(thresholdCrossed(0.8f, 1.0f, 1f))

    @Test fun notWhileStayingAbove() = assertFalse(thresholdCrossed(1.0f, 1.2f, 1f))

    @Test fun notOnDownwardCrossing() = assertFalse(thresholdCrossed(1.1f, 0.9f, 1f))

    @Test fun refiresAfterDroppingBelow() {
        val values = listOf(0.5f, 1.0f, 0.7f, 1.0f)
        val fires = values.zipWithNext().count { (a, b) -> thresholdCrossed(a, b, 1f) }
        assertEquals(2, fires)
    }

    // Starting a refresh from code animates the indicator to the threshold; that must not tick
    @Test fun programmaticRefreshDoesNotTick() {
        val frames = listOf(0f, 0.5f, 1f).map { pullHapticFraction(isRefreshing = true, distanceFraction = it) }
        assertEquals(0, frames.zipWithNext().count { (a, b) -> thresholdCrossed(a, b, 1f) })
    }

    @Test fun userPullStillTicks() {
        val frames = listOf(0f, 0.5f, 1f).map { pullHapticFraction(isRefreshing = false, distanceFraction = it) }
        assertEquals(1, frames.zipWithNext().count { (a, b) -> thresholdCrossed(a, b, 1f) })
    }
}
