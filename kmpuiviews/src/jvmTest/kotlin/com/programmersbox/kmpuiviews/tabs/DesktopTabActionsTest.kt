package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.input.pointer.PointerButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopTabActionsTest {

    private fun mode(
        isMac: Boolean,
        meta: Boolean = false,
        ctrl: Boolean = false,
        shift: Boolean = false,
        button: PointerButton = PointerButton.Primary,
    ) = modeFor(isMac = isMac, meta = meta, ctrl = ctrl, shift = shift, button = button)

    @Test
    fun commandClickOnMacOpensInBackground() {
        assertEquals(TabOpenMode.Background, mode(isMac = true, meta = true))
    }

    @Test
    fun ctrlClickOnMacIsPlain() {
        assertNull(mode(isMac = true, ctrl = true))
    }

    @Test
    fun ctrlClickOnWindowsOpensInBackground() {
        assertEquals(TabOpenMode.Background, mode(isMac = false, ctrl = true))
    }

    @Test
    fun commandShiftClickOpensInForeground() {
        assertEquals(TabOpenMode.Foreground, mode(isMac = true, meta = true, shift = true))
        assertEquals(TabOpenMode.Foreground, mode(isMac = false, ctrl = true, shift = true))
    }

    @Test
    fun middleClickOpensInBackground() {
        assertEquals(TabOpenMode.Background, mode(isMac = true, button = PointerButton.Tertiary))
        assertEquals(TabOpenMode.Background, mode(isMac = false, button = PointerButton.Tertiary))
    }

    @Test
    fun plainClickIsPlain() {
        assertNull(mode(isMac = true))
        assertNull(mode(isMac = false))
        assertNull(mode(isMac = true, shift = true))
        assertNull(mode(isMac = false, meta = true))
    }

    @Test
    fun rightClickIsNotAMode() {
        assertNull(mode(isMac = true, meta = true, button = PointerButton.Secondary))
    }

    private fun click(
        consumed: Boolean = false,
        inside: Boolean = true,
        movedPastSlop: Boolean = false,
        heldMillis: Long = 100,
    ) = isTabClick(consumed = consumed, inside = inside, movedPastSlop = movedPastSlop, heldMillis = heldMillis, longPressMillis = 500)

    @Test
    fun quickReleaseInsideIsAClick() {
        assertTrue(click())
    }

    @Test
    fun longPressIsNotAClick() {
        assertFalse(click(heldMillis = 500))
        assertFalse(click(heldMillis = 800))
    }

    @Test
    fun releaseOutsideIsNotAClick() {
        assertFalse(click(inside = false))
    }

    @Test
    fun dragOrSwipeIsNotAClick() {
        assertFalse(click(movedPastSlop = true))
    }

    @Test
    fun consumedReleaseIsNotAClick() {
        assertFalse(click(consumed = true))
    }
}
