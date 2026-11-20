package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TabMotionSpecsTest {
    @Test
    fun durationsMatchTheSpec() {
        assertEquals(520, tabMotionMs(TabMotion.Background, 1f, false))
        assertEquals(450, tabMotionMs(TabMotion.Grow, 1f, false))
        assertEquals(120, tabMotionMs(TabMotion.Switch, 1f, false))
        assertEquals(180, tabMotionMs(TabMotion.Close, 1f, false))
        assertEquals(300, tabMotionMs(TabMotion.Flash, 1f, false))
    }

    @Test
    fun scaleMultipliesEveryDuration() {
        assertEquals(1560, tabMotionMs(TabMotion.Background, 3f, false))
        assertEquals(60, tabMotionMs(TabMotion.Switch, 0.5f, false))
    }

    @Test
    fun reducedMotionIsAOneFiftyMsCrossfadeForEverything() {
        TabMotion.entries.forEach {
            assertEquals(150, tabMotionMs(it, 1f, true), "$it")
            assertEquals(LinearEasing, tabMotionEasing(it, true), "$it")
        }
        assertEquals(300, tabMotionMs(TabMotion.Flash, 2f, true))
    }

    @Test
    fun easingsMatchTheSpec() {
        assertEquals(CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f), tabMotionEasing(TabMotion.Background, false))
        assertEquals(CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f), tabMotionEasing(TabMotion.Grow, false))
        assertEquals(LinearEasing, tabMotionEasing(TabMotion.Switch, false))
        assertEquals(CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f), tabMotionEasing(TabMotion.Close, false))
        assertEquals(CubicBezierEasing(0.2f, 0f, 0f, 1f), tabMotionEasing(TabMotion.Flash, false))
    }

    @Test
    fun scaleParsingFallsBackToOne() {
        assertEquals(1f, parseMotionScale(null))
        assertEquals(1f, parseMotionScale("abc"))
        assertEquals(1f, parseMotionScale("0"))
        assertEquals(1f, parseMotionScale("-2"))
        assertEquals(4f, parseMotionScale("4"))
        assertEquals(2.5f, parseMotionScale(" 2.5 "))
    }

    @Test
    fun macReduceMotionReadsTheDefaultsFlag() {
        assertTrue(parseReduceMotion("Mac OS X", "1\n"))
        assertFalse(parseReduceMotion("Mac OS X", "0\n"))
        assertFalse(parseReduceMotion("Mac OS X", "The domain/default pair of (...) does not exist"))
    }

    @Test
    fun gnomeReducesWhenAnimationsAreDisabled() {
        assertTrue(parseReduceMotion("Linux", "false\n"))
        assertFalse(parseReduceMotion("Linux", "true\n"))
        assertFalse(parseReduceMotion("Linux", ""))
    }

    @Test
    fun windowsNeverReduces() {
        assertFalse(parseReduceMotion("Windows 11", "1"))
        assertFalse(parseReduceMotion("Windows 11", "false"))
    }
}
