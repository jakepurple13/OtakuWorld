package com.programmersbox.kmpuiviews.presentation.shortcuts

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
class AppShortcutTest {
    @Test
    fun macShiftCmdF() {
        val e = KeyEvent(Key.F, KeyEventType.KeyDown, isMetaPressed = true, isShiftPressed = true)
        assertTrue(AppShortcut.GlobalSearch.matches(e, isMac = true))
    }

    @Test
    fun cmdFWithoutShiftIgnored() {
        val e = KeyEvent(Key.F, KeyEventType.KeyDown, isMetaPressed = true)
        assertFalse(AppShortcut.GlobalSearch.matches(e, isMac = true))
    }

    @Test
    fun windowsCtrlShiftF() {
        val e = KeyEvent(Key.F, KeyEventType.KeyDown, isCtrlPressed = true, isShiftPressed = true)
        assertTrue(AppShortcut.GlobalSearch.matches(e, isMac = false))
    }

    @Test
    fun ctrlSlash() {
        val e = KeyEvent(Key.Slash, KeyEventType.KeyDown, isCtrlPressed = true)
        assertTrue(AppShortcut.ShortcutsList.matches(e, isMac = false))
    }

    @Test
    fun ctrlShiftSlashIgnored() {
        val e = KeyEvent(Key.Slash, KeyEventType.KeyDown, isCtrlPressed = true, isShiftPressed = true)
        assertFalse(AppShortcut.ShortcutsList.matches(e, isMac = false))
    }

    @Test
    fun labels() {
        assertEquals("⇧⌘F", AppShortcut.GlobalSearch.label(true))
        assertEquals("Ctrl+Shift+F", AppShortcut.GlobalSearch.label(false))
        assertEquals("Ctrl+/", AppShortcut.ShortcutsList.label(false))
    }
}
