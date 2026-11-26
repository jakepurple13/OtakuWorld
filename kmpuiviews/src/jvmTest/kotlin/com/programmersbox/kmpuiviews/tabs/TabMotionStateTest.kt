package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TabMotionStateTest {
    private val tab = TabId(7)
    private val window = WindowId(1)
    private val origin = Rect(1f, 2f, 3f, 4f)

    private fun opened(mode: TabOpenMode, origin: Rect? = this.origin) = TabEvent.Opened(tab, window, mode, origin)

    @Test
    fun foregroundOpenWithOriginStartsAGrow() {
        val motion = TabMotionState(scale = 1f)
        motion.record(opened(TabOpenMode.Foreground))
        assertTrue(motion.hasGrow(tab))
        assertTrue(tab !in motion.flying)
    }

    @Test
    fun backgroundOpenWithOriginMarksTheTabFlying() {
        val motion = TabMotionState(scale = 1f)
        motion.record(opened(TabOpenMode.Background))
        assertTrue(tab in motion.flying)
        assertFalse(motion.hasGrow(tab))
    }

    @Test
    fun nullOriginDoesNothing() {
        val motion = TabMotionState(scale = 1f)
        motion.record(opened(TabOpenMode.Foreground, origin = null))
        motion.record(opened(TabOpenMode.Background, origin = null))
        assertFalse(motion.hasGrow(tab))
        assertTrue(motion.flying.isEmpty())
    }

    @Test
    fun reducedOrOffMotionDoesNothing() {
        listOf(TabMotionMode.Reduced, TabMotionMode.Off).forEach { mode ->
            val motion = TabMotionState(scale = 1f, mode = mode)
            motion.record(opened(TabOpenMode.Foreground))
            motion.record(opened(TabOpenMode.Background))
            assertFalse(motion.hasGrow(tab), "$mode")
            assertTrue(motion.flying.isEmpty(), "$mode")
        }
    }

    @Test
    fun systemModeFollowsTheOsSetting() {
        val motion = TabMotionState(scale = 1f)
        assertEquals(TabMotionLevel.Full, motion.level)
        motion.systemReduces = true
        assertEquals(TabMotionLevel.Reduced, motion.level)
        assertEquals(150, motion.ms(TabMotion.Grow))
        motion.mode = TabMotionMode.Full
        assertEquals(TabMotionLevel.Full, motion.level)
        motion.mode = TabMotionMode.Off
        assertEquals(0, motion.ms(TabMotion.Grow))
    }

    @Test
    fun newWindowOpenDoesNothing() {
        val motion = TabMotionState(scale = 1f)
        motion.record(opened(TabOpenMode.NewWindow))
        assertFalse(motion.hasGrow(tab))
        assertTrue(motion.flying.isEmpty())
    }

    @Test
    fun flashEventsCount() {
        val motion = TabMotionState(scale = 1f)
        motion.record(TabEvent.Flash(tab))
        assertEquals(1, motion.flashes[tab])
        motion.record(TabEvent.Flash(tab))
        assertEquals(2, motion.flashes[tab])
    }

    @Test
    fun takeGrowTakesOnlyOnce() {
        val motion = TabMotionState(scale = 1f)
        motion.record(opened(TabOpenMode.Foreground))
        assertNotNull(motion.takeGrow(tab))
        assertNull(motion.takeGrow(tab))
        assertFalse(motion.hasGrow(tab))
    }

    @Test
    fun markSeenIsTrueOnlyTheFirstTime() {
        val motion = TabMotionState(scale = 1f)
        assertTrue(motion.markSeen(tab))
        assertFalse(motion.markSeen(tab))
        assertTrue(motion.markSeen(TabId(8)))
    }

    @Test
    fun growAlphasFollowTheSpec() {
        val ghost = GrowGhost(tab, WindowId(0), origin)
        assertEquals(0f, ghost.contentAlpha)
        assertEquals(1f, ghost.coverAlpha)
    }

    @Test
    fun scaleAppliesOnlyWithTheDebugFlag() {
        assertEquals(1f, tabMotionScale(null, "4"))
        assertEquals(1f, tabMotionScale("false", "4"))
        assertEquals(4f, tabMotionScale("true", "4"))
    }
}
