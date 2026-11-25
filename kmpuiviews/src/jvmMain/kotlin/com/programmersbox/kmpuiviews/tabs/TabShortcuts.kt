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

    /** Switch to the [n]th tab of the window, 1-based, 1..8. */
    data class SwitchTo(val n: Int) : TabShortcut
    data object LastTab : TabShortcut
    data object Next : TabShortcut
    data object Previous : TabShortcut
    data object Back : TabShortcut
    data object Forward : TabShortcut

    /** The browser "address bar": open or switch to the Search tab and focus its field. */
    data object FocusSearch : TabShortcut
    data object NewWindow : TabShortcut
    data object CloseWindow : TabShortcut
}

private val numberKeys = listOf(
    Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight,
)

/**
 * Maps a key press to a tab shortcut. Every shortcut needs a modifier, so bare keys (the reader's
 * and player's shortcuts) always return null.
 *
 * - ⌘ (macOS) / Ctrl (others) + T, W, ⇧T, L, N, ⇧W, 1…8, 9 (last tab)
 * - ⌃Tab / ⌃⇧Tab and ⌃PageDown / ⌃PageUp on every OS, Ctrl+F4 closes on Windows/Linux
 * - ⌘[ / ⌘], ⌘← / ⌘→ on macOS, Alt+← / Alt+→ elsewhere; ⌘⇧] / ⌘⇧[ and ⌘⌥→ / ⌘⌥← switch tabs
 *
 * ⌘← / ⌘→ are skipped while [textFieldFocused], where they move the caret.
 *
 * Returns null while [chromeVisible] is false (onboarding and immersive screens hide the nav bar),
 * so tabs can't be opened, switched or closed from there.
 */
fun tabShortcut(
    event: KeyEvent,
    isMac: Boolean,
    chromeVisible: Boolean = true,
    textFieldFocused: Boolean = false,
): TabShortcut? {
    if (!chromeVisible || event.type != KeyEventType.KeyDown) return null
    val meta = event.isMetaPressed
    val ctrl = event.isCtrlPressed
    val shift = event.isShiftPressed
    val alt = event.isAltPressed
    val key = event.key

    // Every OS: Ctrl+Tab and Ctrl+PageDown/PageUp.
    if (ctrl && !meta && !alt) {
        when (key) {
            Key.Tab -> return if (shift) TabShortcut.Previous else TabShortcut.Next
            Key.PageDown -> if (!shift) return TabShortcut.Next
            Key.PageUp -> if (!shift) return TabShortcut.Previous
            Key.F4 -> if (!isMac && !shift) return TabShortcut.CloseTab
        }
    }

    if (!isMac && alt && !ctrl && !meta && !shift) {
        return when (key) {
            Key.DirectionLeft -> TabShortcut.Back
            Key.DirectionRight -> TabShortcut.Forward
            else -> null
        }
    }

    // macOS only: ⌘⌥← / ⌘⌥→ switch tabs.
    if (isMac && meta && alt && !ctrl && !shift) {
        return when (key) {
            Key.DirectionRight -> TabShortcut.Next
            Key.DirectionLeft -> TabShortcut.Previous
            else -> null
        }
    }

    val primary = if (isMac) meta && !ctrl else ctrl && !meta
    if (!primary || alt) return null

    if (shift) {
        return when (key) {
            Key.T -> TabShortcut.ReopenClosed
            Key.W -> TabShortcut.CloseWindow
            Key.RightBracket -> if (isMac) TabShortcut.Next else null
            Key.LeftBracket -> if (isMac) TabShortcut.Previous else null
            else -> null
        }
    }

    return when (key) {
        Key.T -> TabShortcut.NewTab
        Key.W -> TabShortcut.CloseTab
        Key.L -> TabShortcut.FocusSearch
        Key.N -> TabShortcut.NewWindow
        Key.Nine -> TabShortcut.LastTab
        Key.LeftBracket -> if (isMac) TabShortcut.Back else null
        Key.RightBracket -> if (isMac) TabShortcut.Forward else null
        Key.DirectionLeft -> if (isMac && !textFieldFocused) TabShortcut.Back else null
        Key.DirectionRight -> if (isMac && !textFieldFocused) TabShortcut.Forward else null
        in numberKeys -> TabShortcut.SwitchTo(numberKeys.indexOf(key) + 1)
        else -> null
    }
}

/** [index] moved by [delta] within [size] tabs, wrapping around both ends. */
fun wrappedTabIndex(index: Int, delta: Int, size: Int): Int = (index + delta).mod(size)

/** Label and key caps of every tab shortcut, for the shortcuts dialog. */
fun tabShortcutCaps(isMac: Boolean): List<Pair<String, List<String>>> {
    val mod = if (isMac) "⌘" else "Ctrl"
    val shift = if (isMac) "⇧" else "Shift"
    val ctrl = if (isMac) "⌃" else "Ctrl"
    val back = if (isMac) listOf(mod, "[") else listOf("Alt", "←")
    val forward = if (isMac) listOf(mod, "]") else listOf("Alt", "→")
    return listOf(
        "New tab" to listOf(mod, "T"),
        "Close tab" to listOf(mod, "W"),
        "Reopen closed tab" to listOf(mod, shift, "T"),
        "Next tab" to listOf(ctrl, "Tab"),
        "Previous tab" to listOf(ctrl, shift, "Tab"),
        "Go to tab 1–8" to listOf(mod, "1–8"),
        "Go to last tab" to listOf(mod, "9"),
        "Back" to back,
        "Forward" to forward,
        "Search" to listOf(mod, "L"),
        "New window" to listOf(mod, "N"),
        "Close window" to listOf(mod, shift, "W"),
    )
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
        TabShortcut.LastTab -> activate(window.tabs.last().id)
        TabShortcut.Next -> activate(window.tabs[wrappedTabIndex(index, 1, window.tabs.size)].id)
        TabShortcut.Previous -> activate(window.tabs[wrappedTabIndex(index, -1, window.tabs.size)].id)
        TabShortcut.Back -> back(active)
        TabShortcut.Forward -> forward(active)
        TabShortcut.FocusSearch -> {
            open(Screen.GlobalSearchScreen(), TabOpenMode.Foreground, active)
            searchFocus.request()
        }
        // wired in Task 13
        TabShortcut.NewWindow, TabShortcut.CloseWindow -> Unit
    }
}
