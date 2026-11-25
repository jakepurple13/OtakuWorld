package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
class TabShortcutsTest {

    private fun key(
        key: Key,
        meta: Boolean = false,
        ctrl: Boolean = false,
        shift: Boolean = false,
        alt: Boolean = false,
        type: KeyEventType = KeyEventType.KeyDown,
    ) = KeyEvent(
        key,
        type,
        isMetaPressed = meta,
        isCtrlPressed = ctrl,
        isShiftPressed = shift,
        isAltPressed = alt,
    )

    @Test
    fun macShortcuts() {
        assertEquals(TabShortcut.NewTab, tabShortcut(key(Key.T, meta = true), isMac = true))
        assertEquals(TabShortcut.CloseTab, tabShortcut(key(Key.W, meta = true), isMac = true))
        assertEquals(TabShortcut.ReopenClosed, tabShortcut(key(Key.T, meta = true, shift = true), isMac = true))
        assertEquals(TabShortcut.SwitchTo(1), tabShortcut(key(Key.One, meta = true), isMac = true))
        assertEquals(TabShortcut.SwitchTo(8), tabShortcut(key(Key.Eight, meta = true), isMac = true))
        assertEquals(TabShortcut.Next, tabShortcut(key(Key.Tab, ctrl = true), isMac = true))
        assertEquals(TabShortcut.Previous, tabShortcut(key(Key.Tab, ctrl = true, shift = true), isMac = true))
        assertEquals(TabShortcut.Back, tabShortcut(key(Key.LeftBracket, meta = true), isMac = true))
        assertEquals(TabShortcut.Forward, tabShortcut(key(Key.RightBracket, meta = true), isMac = true))
    }

    @Test
    fun windowsLinuxShortcuts() {
        assertEquals(TabShortcut.NewTab, tabShortcut(key(Key.T, ctrl = true), isMac = false))
        assertEquals(TabShortcut.CloseTab, tabShortcut(key(Key.W, ctrl = true), isMac = false))
        assertEquals(TabShortcut.ReopenClosed, tabShortcut(key(Key.T, ctrl = true, shift = true), isMac = false))
        assertEquals(TabShortcut.SwitchTo(1), tabShortcut(key(Key.One, ctrl = true), isMac = false))
        assertEquals(TabShortcut.SwitchTo(5), tabShortcut(key(Key.Five, ctrl = true), isMac = false))
        assertEquals(TabShortcut.SwitchTo(8), tabShortcut(key(Key.Eight, ctrl = true), isMac = false))
        assertEquals(TabShortcut.Next, tabShortcut(key(Key.Tab, ctrl = true), isMac = false))
        assertEquals(TabShortcut.Previous, tabShortcut(key(Key.Tab, ctrl = true, shift = true), isMac = false))
        assertEquals(TabShortcut.Back, tabShortcut(key(Key.DirectionLeft, alt = true), isMac = false))
        assertEquals(TabShortcut.Forward, tabShortcut(key(Key.DirectionRight, alt = true), isMac = false))
    }

    @Test
    fun otherPlatformModifierIsIgnored() {
        assertNull(tabShortcut(key(Key.T, ctrl = true), isMac = true))
        assertNull(tabShortcut(key(Key.W, ctrl = true), isMac = true))
        assertNull(tabShortcut(key(Key.T, meta = true), isMac = false))
        assertNull(tabShortcut(key(Key.LeftBracket, ctrl = true), isMac = false))
        assertNull(tabShortcut(key(Key.DirectionLeft, alt = true), isMac = true))
    }

    @Test
    fun keyUpIsIgnored() {
        assertNull(tabShortcut(key(Key.T, meta = true, type = KeyEventType.KeyUp), isMac = true))
        assertNull(tabShortcut(key(Key.Tab, ctrl = true, type = KeyEventType.KeyUp), isMac = false))
    }

    @Test
    fun extraModifiersAreIgnored() {
        assertNull(tabShortcut(key(Key.W, meta = true, alt = true), isMac = true))
        assertNull(tabShortcut(key(Key.T, ctrl = true, alt = true), isMac = false))
        assertNull(tabShortcut(key(Key.Zero, ctrl = true), isMac = false))
    }

    @Test
    fun hiddenChromeDisablesEveryShortcut() {
        listOf(
            key(Key.T, meta = true),
            key(Key.W, meta = true),
            key(Key.T, meta = true, shift = true),
            key(Key.One, meta = true),
            key(Key.Tab, ctrl = true),
            key(Key.LeftBracket, meta = true),
        ).forEach { assertNull(tabShortcut(it, isMac = true, chromeVisible = false)) }
        assertNull(tabShortcut(key(Key.T, ctrl = true), isMac = false, chromeVisible = false))
        assertNull(tabShortcut(key(Key.DirectionLeft, alt = true), isMac = false, chromeVisible = false))
        assertEquals(TabShortcut.NewTab, tabShortcut(key(Key.T, meta = true), isMac = true, chromeVisible = true))
    }

    @Test
    fun bareKeysReturnNull() {
        listOf(Key.Spacebar, Key.DirectionLeft, Key.DirectionRight, Key.F, Key.C, Key.Tab, Key.T, Key.W, Key.One)
            .forEach { k ->
                assertNull(tabShortcut(key(k), isMac = true), "mac $k")
                assertNull(tabShortcut(key(k), isMac = false), "other $k")
            }
    }

    private val controller = TabsController(
        homeNav = Navigation3Actions(Screen.RecentScreen),
        keys = TabKeys(listOf(CoreTabKeyProvider)),
        prefs = TabPrefsSnapshot(clickOpensNewTab = false),
    )
    private val window = controller.windows.single()
    private val home = window.activeId.value

    @Test
    fun switchNextPreviousWrapAround() {
        val settings = controller.open(Screen.Settings, TabOpenMode.Foreground, home)
        val search = controller.open(Screen.GlobalSearchScreen(), TabOpenMode.Foreground, settings)

        controller.perform(TabShortcut.SwitchTo(1), window)
        assertEquals(home, window.activeId.value)
        controller.perform(TabShortcut.Previous, window)
        assertEquals(search, window.activeId.value)
        controller.perform(TabShortcut.Next, window)
        assertEquals(home, window.activeId.value)
        controller.perform(TabShortcut.SwitchTo(8), window)
        assertEquals(home, window.activeId.value)
        controller.perform(TabShortcut.LastTab, window)
        assertEquals(search, window.activeId.value)
    }

    @Test
    fun closeIgnoresHomeAndReopenRestores() {
        controller.perform(TabShortcut.CloseTab, window)
        assertEquals(listOf(home), window.tabs.map { it.id })

        controller.perform(TabShortcut.NewTab, window)
        assertEquals(2, window.tabs.size)
        controller.perform(TabShortcut.CloseTab, window)
        assertEquals(listOf(home), window.tabs.map { it.id })

        controller.perform(TabShortcut.ReopenClosed, window)
        assertEquals(2, window.tabs.size)
        assertEquals(window.tabs[1].id, window.activeId.value)
    }

    @Test
    fun macFullTable() {
        fun mac(k: Key, meta: Boolean = false, ctrl: Boolean = false, shift: Boolean = false, alt: Boolean = false) =
            tabShortcut(key(k, meta, ctrl, shift, alt), isMac = true)
        assertEquals(TabShortcut.Next, mac(Key.DirectionRight, meta = true, alt = true))
        assertEquals(TabShortcut.Previous, mac(Key.DirectionLeft, meta = true, alt = true))
        assertEquals(TabShortcut.Next, mac(Key.RightBracket, meta = true, shift = true))
        assertEquals(TabShortcut.Previous, mac(Key.LeftBracket, meta = true, shift = true))
        assertEquals(TabShortcut.Next, mac(Key.PageDown, ctrl = true))
        assertEquals(TabShortcut.Previous, mac(Key.PageUp, ctrl = true))
        assertEquals(TabShortcut.Back, mac(Key.DirectionLeft, meta = true))
        assertEquals(TabShortcut.Forward, mac(Key.DirectionRight, meta = true))
        assertEquals(TabShortcut.FocusSearch, mac(Key.L, meta = true))
        assertEquals(TabShortcut.NewWindow, mac(Key.N, meta = true))
        assertEquals(TabShortcut.CloseWindow, mac(Key.W, meta = true, shift = true))
        assertNull(mac(Key.F4, ctrl = true))
    }

    @Test
    fun windowsLinuxFullTable() {
        fun win(k: Key, ctrl: Boolean = false, shift: Boolean = false) =
            tabShortcut(key(k, ctrl = ctrl, shift = shift), isMac = false)
        assertEquals(TabShortcut.Next, win(Key.PageDown, ctrl = true))
        assertEquals(TabShortcut.Previous, win(Key.PageUp, ctrl = true))
        assertEquals(TabShortcut.FocusSearch, win(Key.L, ctrl = true))
        assertEquals(TabShortcut.NewWindow, win(Key.N, ctrl = true))
        assertEquals(TabShortcut.CloseWindow, win(Key.W, ctrl = true, shift = true))
        assertNull(win(Key.DirectionLeft, ctrl = true))
    }

    @Test
    fun ctrlF4ClosesOnWindows() {
        assertEquals(TabShortcut.CloseTab, tabShortcut(key(Key.F4, ctrl = true), isMac = false))
        assertNull(tabShortcut(key(Key.F4, ctrl = true), isMac = true))
    }

    @Test
    fun nineIsLastTab() {
        assertEquals(TabShortcut.LastTab, tabShortcut(key(Key.Nine, meta = true), isMac = true))
        assertEquals(TabShortcut.LastTab, tabShortcut(key(Key.Nine, ctrl = true), isMac = false))
    }

    @Test
    fun cmdArrowIgnoredWhileTyping() {
        assertNull(tabShortcut(key(Key.DirectionLeft, meta = true), isMac = true, textFieldFocused = true))
        assertNull(tabShortcut(key(Key.DirectionRight, meta = true), isMac = true, textFieldFocused = true))
        // Other back/forward shortcuts are unaffected.
        assertEquals(
            TabShortcut.Back,
            tabShortcut(key(Key.LeftBracket, meta = true), isMac = true, textFieldFocused = true),
        )
        assertEquals(
            TabShortcut.Back,
            tabShortcut(key(Key.DirectionLeft, alt = true), isMac = false, textFieldFocused = true),
        )
    }

    @Test
    fun nextWrapsAround() {
        assertEquals(0, wrappedTabIndex(index = 2, delta = 1, size = 3))
        assertEquals(2, wrappedTabIndex(index = 0, delta = -1, size = 3))
        assertEquals(1, wrappedTabIndex(index = 0, delta = 1, size = 3))
        assertEquals(0, wrappedTabIndex(index = 0, delta = 1, size = 1))
    }

    @Test
    fun windowActionsAreNoOpsUntilTask13() {
        val before = window.tabs.map { it.id }
        controller.perform(TabShortcut.NewWindow, window)
        controller.perform(TabShortcut.CloseWindow, window)
        assertEquals(before, window.tabs.map { it.id })
        assertEquals(1, controller.windows.size)
    }

    @Test
    fun focusSearchOpensSearchTabAndRequestsFocus() {
        controller.perform(TabShortcut.FocusSearch, window)
        assertEquals(2, window.tabs.size)
        assertEquals(window.tabs[1].id, window.activeId.value)
        assertEquals(true, controller.searchFocus.pending)
        // Again: switches to the same tab instead of opening a second one.
        controller.perform(TabShortcut.SwitchTo(1), window)
        controller.perform(TabShortcut.FocusSearch, window)
        assertEquals(2, window.tabs.size)
        assertEquals(window.tabs[1].id, window.activeId.value)
    }

    @Test
    fun capsListsEveryShortcutPerPlatform() {
        val mac = tabShortcutCaps(isMac = true).toMap()
        assertEquals(listOf("⌘", "T"), mac["New tab"])
        assertEquals(listOf("⌘", "9"), mac["Go to last tab"])
        assertEquals(listOf("⌘", "⇧", "W"), mac["Close window"])
        val win = tabShortcutCaps(isMac = false).toMap()
        assertEquals(listOf("Ctrl", "T"), win["New tab"])
        assertEquals(listOf("Ctrl", "Shift", "W"), win["Close window"])
        assertEquals(listOf("Alt", "←"), win["Back"])
    }
}
