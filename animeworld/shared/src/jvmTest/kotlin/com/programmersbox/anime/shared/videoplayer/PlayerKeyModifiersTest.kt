package com.programmersbox.anime.shared.videoplayer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
class PlayerKeyModifiersTest {
    @Test
    fun plainKeysArePlayerKeys() {
        assertFalse(KeyEvent(Key.F, KeyEventType.KeyDown).hasAppModifier())
        assertFalse(KeyEvent(Key.Spacebar, KeyEventType.KeyDown).hasAppModifier())
    }

    @Test
    fun shiftAloneIsAPlayerKey() {
        assertFalse(KeyEvent(Key.C, KeyEventType.KeyDown, isShiftPressed = true).hasAppModifier())
        assertFalse(KeyEvent(Key.Slash, KeyEventType.KeyDown, isShiftPressed = true).hasAppModifier())
    }

    @Test
    fun commandCtrlAltAreAppShortcuts() {
        assertTrue(KeyEvent(Key.K, KeyEventType.KeyDown, isMetaPressed = true).hasAppModifier())
        assertTrue(KeyEvent(Key.F, KeyEventType.KeyDown, isMetaPressed = true, isShiftPressed = true).hasAppModifier())
        assertTrue(KeyEvent(Key.K, KeyEventType.KeyDown, isCtrlPressed = true).hasAppModifier())
        assertTrue(KeyEvent(Key.F, KeyEventType.KeyDown, isAltPressed = true).hasAppModifier())
    }
}
