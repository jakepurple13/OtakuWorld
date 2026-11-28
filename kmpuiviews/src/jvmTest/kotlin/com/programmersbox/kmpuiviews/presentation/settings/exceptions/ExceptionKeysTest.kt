package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExceptionKeysTest {
    private fun key(key: Key, type: KeyEventType = KeyEventType.KeyDown, meta: Boolean = false, ctrl: Boolean = false) =
        KeyPress(key, type, meta, ctrl)

    @Test
    fun arrowsMoveSelection() {
        assertEquals(ExceptionKeyAction.Down, exceptionKeyAction(key(Key.DirectionDown)))
        assertEquals(ExceptionKeyAction.Up, exceptionKeyAction(key(Key.DirectionUp)))
    }

    @Test
    fun backspaceAndDeleteDelete() {
        assertEquals(ExceptionKeyAction.Delete, exceptionKeyAction(key(Key.Backspace)))
        assertEquals(ExceptionKeyAction.Delete, exceptionKeyAction(key(Key.Delete)))
    }

    @Test
    fun copyAndUndoNeedModifier() {
        assertEquals(ExceptionKeyAction.Copy, exceptionKeyAction(key(Key.C, meta = true)))
        assertEquals(ExceptionKeyAction.Copy, exceptionKeyAction(key(Key.C, ctrl = true)))
        assertEquals(ExceptionKeyAction.Undo, exceptionKeyAction(key(Key.Z, ctrl = true)))
        assertNull(exceptionKeyAction(key(Key.C)))
        assertNull(exceptionKeyAction(key(Key.Z)))
    }

    @Test
    fun keyUpIgnored() {
        assertNull(exceptionKeyAction(key(Key.DirectionDown, type = KeyEventType.KeyUp)))
    }
}
