package com.programmersbox.kmpuiviews.presentation.quickactions

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
class QuickActionsShortcutTest {
    @Test
    fun macCommandK() {
        assertTrue(isQuickActionsShortcut(KeyEvent(Key.K, KeyEventType.KeyDown, isMetaPressed = true), isMac = true))
    }

    @Test
    fun macCtrlKIsIgnored() {
        assertFalse(isQuickActionsShortcut(KeyEvent(Key.K, KeyEventType.KeyDown, isCtrlPressed = true), isMac = true))
    }

    @Test
    fun windowsCtrlK() {
        assertTrue(isQuickActionsShortcut(KeyEvent(Key.K, KeyEventType.KeyDown, isCtrlPressed = true), isMac = false))
    }

    @Test
    fun ctrlShiftKIsIgnored() {
        assertFalse(
            isQuickActionsShortcut(
                KeyEvent(Key.K, KeyEventType.KeyDown, isCtrlPressed = true, isShiftPressed = true),
                isMac = false,
            )
        )
    }

    @Test
    fun keyUpIsIgnored() {
        assertFalse(isQuickActionsShortcut(KeyEvent(Key.K, KeyEventType.KeyUp, isCtrlPressed = true), isMac = false))
    }
}
