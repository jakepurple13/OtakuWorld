package com.programmersbox.kmpuiviews.presentation.shortcuts

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/** Desktop app-level shortcuts: Cmd on macOS, Ctrl elsewhere. */
enum class AppShortcut(val key: Key, val shift: Boolean) {
    QuickActions(Key.K, false),
    GlobalSearch(Key.F, true),
    ShortcutsList(Key.Slash, false),
}

fun AppShortcut.matches(event: KeyEvent, isMac: Boolean): Boolean =
    event.type == KeyEventType.KeyDown &&
            event.key == key &&
            event.isShiftPressed == shift &&
            !event.isAltPressed &&
            if (isMac) event.isMetaPressed && !event.isCtrlPressed
            else event.isCtrlPressed && !event.isMetaPressed

fun AppShortcut.keyCaps(isMac: Boolean): List<String> {
    val letter = when (this) {
        AppShortcut.QuickActions -> "K"
        AppShortcut.GlobalSearch -> "F"
        AppShortcut.ShortcutsList -> "/"
    }
    return if (isMac) listOfNotNull(if (shift) "⇧" else null, "⌘", letter)
    else listOfNotNull("Ctrl", if (shift) "Shift" else null, letter)
}

fun AppShortcut.label(isMac: Boolean): String = keyCaps(isMac).joinToString(if (isMac) "" else "+")

/** Menu-bar accelerator; Compose maps `meta` to Cmd on macOS. */
fun AppShortcut.menuShortcut(): KeyShortcut = KeyShortcut(key, meta = true, shift = shift)
