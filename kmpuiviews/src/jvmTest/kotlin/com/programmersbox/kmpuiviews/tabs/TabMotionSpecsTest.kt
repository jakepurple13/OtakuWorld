package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import kotlin.test.Test
import kotlin.test.assertEquals

class TabMotionSpecsTest {
    private val full = TabMotionLevel.Full

    @Test
    fun durationsMatchTheSpec() {
        assertEquals(520, tabMotionMs(TabMotion.Background, 1f, full))
        assertEquals(450, tabMotionMs(TabMotion.Grow, 1f, full))
        assertEquals(120, tabMotionMs(TabMotion.Switch, 1f, full))
        assertEquals(180, tabMotionMs(TabMotion.Close, 1f, full))
        assertEquals(300, tabMotionMs(TabMotion.Flash, 1f, full))
        assertEquals(450, tabMotionMs(TabMotion.Layout, 1f, full))
    }

    @Test
    fun scaleMultipliesEveryDuration() {
        assertEquals(1560, tabMotionMs(TabMotion.Background, 3f, full))
        assertEquals(60, tabMotionMs(TabMotion.Switch, 0.5f, full))
    }

    @Test
    fun motionReducedUsesCrossfade() {
        TabMotion.entries.forEach {
            assertEquals(150, tabMotionMs(it, 1f, TabMotionLevel.Reduced), "$it")
            assertEquals(LinearEasing, tabMotionEasing(it, TabMotionLevel.Reduced), "$it")
        }
        assertEquals(300, tabMotionMs(TabMotion.Flash, 2f, TabMotionLevel.Reduced))
    }

    @Test
    fun motionOffZeroesDurations() {
        TabMotion.entries.forEach {
            assertEquals(0, tabMotionMs(it, 1f, TabMotionLevel.Off), "$it")
            assertEquals(0, tabMotionMs(it, 4f, TabMotionLevel.Off), "$it")
        }
    }

    @Test
    fun motionModeResolvesAgainstTheOsSetting() {
        assertEquals(TabMotionLevel.Full, tabMotionLevel(TabMotionMode.System, systemReduces = false))
        assertEquals(TabMotionLevel.Reduced, tabMotionLevel(TabMotionMode.System, systemReduces = true))
        listOf(false, true).forEach { os ->
            assertEquals(TabMotionLevel.Full, tabMotionLevel(TabMotionMode.Full, os))
            assertEquals(TabMotionLevel.Reduced, tabMotionLevel(TabMotionMode.Reduced, os))
            assertEquals(TabMotionLevel.Off, tabMotionLevel(TabMotionMode.Off, os))
        }
    }

    @Test
    fun easingsMatchTheSpec() {
        assertEquals(CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f), tabMotionEasing(TabMotion.Background, full))
        assertEquals(CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f), tabMotionEasing(TabMotion.Grow, full))
        assertEquals(CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f), tabMotionEasing(TabMotion.Layout, full))
        assertEquals(LinearEasing, tabMotionEasing(TabMotion.Switch, full))
        assertEquals(CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f), tabMotionEasing(TabMotion.Close, full))
        assertEquals(CubicBezierEasing(0.2f, 0f, 0f, 1f), tabMotionEasing(TabMotion.Flash, full))
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
}
