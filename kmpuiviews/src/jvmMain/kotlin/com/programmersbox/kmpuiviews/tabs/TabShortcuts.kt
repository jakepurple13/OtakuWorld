package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.programmersbox.kmpuiviews.presentation.Screen

sealed interface TabShortcut {
    data object NewTab : TabShortcut
    data object CloseTab : TabShortcut
    data object ReopenClosed : TabShortcut

    /** Switch to the [n]th tab of the window, 1-based. */
    data class SwitchTo(val n: Int) : TabShortcut
    data object Next : TabShortcut
    data object Previous : TabShortcut
    data object Back : TabShortcut
    data object Forward : TabShortcut
}

private val numberKeys = listOf(
    Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine,
)

/**
 * Maps a key press to a tab shortcut. Every shortcut needs a modifier, so bare keys (the reader's
 * and player's shortcuts) always return null.
 *
 * - ⌘ (macOS) / Ctrl (others) + T, W, ⇧T, 1…9
 * - ⌃Tab / ⌃⇧Tab on every OS
 * - ⌘[ / ⌘] on macOS, Alt+← / Alt+→ elsewhere
 */
fun tabShortcut(event: KeyEvent, isMac: Boolean): TabShortcut? {
    if (event.type != KeyEventType.KeyDown) return null
    val meta = event.isMetaPressed
    val ctrl = event.isCtrlPressed
    val shift = event.isShiftPressed
    val alt = event.isAltPressed

    if (event.key == Key.Tab && ctrl && !meta && !alt) {
        return if (shift) TabShortcut.Previous else TabShortcut.Next
    }

    if (!isMac && alt && !ctrl && !meta && !shift) {
        return when (event.key) {
            Key.DirectionLeft -> TabShortcut.Back
            Key.DirectionRight -> TabShortcut.Forward
            else -> null
        }
    }

    val primary = if (isMac) meta && !ctrl else ctrl && !meta
    if (!primary || alt) return null

    if (shift) {
        return if (event.key == Key.T) TabShortcut.ReopenClosed else null
    }

    return when (event.key) {
        Key.T -> TabShortcut.NewTab
        Key.W -> TabShortcut.CloseTab
        Key.LeftBracket -> if (isMac) TabShortcut.Back else null
        Key.RightBracket -> if (isMac) TabShortcut.Forward else null
        in numberKeys -> TabShortcut.SwitchTo(numberKeys.indexOf(event.key) + 1)
        else -> null
    }
}

/** Runs [shortcut] on [window]'s active tab. */
fun TabsController.perform(shortcut: TabShortcut, window: BrowserWindowState) {
    val active = window.activeId.value
    val index = window.tabs.indexOfFirst { it.id == active }
    when (shortcut) {
        // Placeholder new-tab page until the real one exists.
        TabShortcut.NewTab -> open(Screen.RecentScreen, TabOpenMode.Foreground, active)
        TabShortcut.CloseTab -> close(active)
        TabShortcut.ReopenClosed -> reopenClosed(window.id)
        is TabShortcut.SwitchTo -> window.tabs.getOrNull(shortcut.n - 1)?.let { activate(it.id) }
        TabShortcut.Next -> activate(window.tabs[(index + 1).mod(window.tabs.size)].id)
        TabShortcut.Previous -> activate(window.tabs[(index - 1).mod(window.tabs.size)].id)
        TabShortcut.Back -> back(active)
        TabShortcut.Forward -> forward(active)
    }
}
