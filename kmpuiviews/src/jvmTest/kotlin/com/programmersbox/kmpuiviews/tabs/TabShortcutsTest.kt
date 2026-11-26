package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import kotlin.test.Test
import androidx.navigation3.runtime.NavKey
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

    /** Stands in for the manga/novel readers and the anime player, which live in the app modules. */
    private data object ImmersiveReaderKey : NavKey

    @Test
    fun onlyOnboardingHidesTabChrome() {
        // Onboarding hides the strip, toolbar and shortcuts.
        assertFalse(tabChromeVisible(Screen.OnboardingScreen))
        // Readers, players and other pages that hide the nav bar keep them.
        assertTrue(tabChromeVisible(ImmersiveReaderKey))
        assertTrue(tabChromeVisible(Screen.WebViewScreen("https://example.com")))
        // Home on any other page shows them.
        assertTrue(tabChromeVisible(Screen.RecentScreen))
        assertTrue(tabChromeVisible(Screen.Settings))
        assertTrue(tabChromeVisible(null))
    }

    @Test
    fun windowChromeFollowsItsActiveTab() {
        val home = Navigation3Actions(Screen.RecentScreen)
        val controller = TabsController(
            homeNav = home,
            keys = TabKeys(listOf(CoreTabKeyProvider)),
            prefs = TabPrefsSnapshot(clickOpensNewTab = false),
        )
        val main = controller.windows.first()
        assertTrue(main.chromeVisible)

        home.toOnboarding()
        assertFalse(main.chromeVisible)
        assertNull(tabShortcut(key(Key.T, meta = true), isMac = true, chromeVisible = main.chromeVisible))

        home.onboardingToRecent()
        assertTrue(main.chromeVisible)

        // A reader in the active tab keeps the chrome and the shortcuts.
        val reader = controller.open(ImmersiveReaderKey, TabOpenMode.Foreground, main.tabs.first().id)
        assertEquals(reader, main.activeId.value)
        assertTrue(main.chromeVisible)
        assertEquals(TabShortcut.NewTab, tabShortcut(key(Key.T, meta = true), isMac = true, chromeVisible = main.chromeVisible))
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
    fun newTabOpensTheNewTabPage() {
        controller.perform(TabShortcut.NewTab, window)
        controller.perform(TabShortcut.NewTab, window)

        assertEquals(listOf(Screen.RecentScreen, NewTabRoute, NewTabRoute), window.tabs.map { it.current })
        assertEquals(window.tabs.last().id, window.activeId.value)
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
    fun newWindowOpensAWindowAndCloseWindowSparesTheMainOne() {
        val before = window.tabs.map { it.id }
        controller.perform(TabShortcut.NewWindow, window)
        controller.perform(TabShortcut.CloseWindow, window)
        assertEquals(before, window.tabs.map { it.id })
        assertEquals(2, controller.windows.size)
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

    private val capKeys = mapOf(
        Key.T to "T", Key.W to "W", Key.L to "L", Key.N to "N", Key.Tab to "Tab",
        Key.PageUp to "PageUp", Key.PageDown to "PageDown", Key.F4 to "F4",
        Key.LeftBracket to "[", Key.RightBracket to "]", Key.DirectionLeft to "←", Key.DirectionRight to "→",
        Key.One to "1–8", Key.Two to "1–8", Key.Three to "1–8", Key.Four to "1–8", Key.Five to "1–8",
        Key.Six to "1–8", Key.Seven to "1–8", Key.Eight to "1–8", Key.Nine to "9",
    )

    private fun capSet(isMac: Boolean, k: Key, meta: Boolean, ctrl: Boolean, shift: Boolean, alt: Boolean): Set<String> =
        buildSet {
            if (meta) add("⌘")
            if (ctrl) add(if (isMac) "⌃" else "Ctrl")
            if (alt) add(if (isMac) "⌥" else "Alt")
            if (shift) add(if (isMac) "⇧" else "Shift")
            add(capKeys.getValue(k))
        }

    @Test
    fun capsAndParserAgreeBothWays() {
        for (isMac in listOf(true, false)) {
            val listed = tabShortcutCaps(isMac).map { it.second.toSet() }.toSet()
            val accepted = mutableSetOf<Set<String>>()
            for (k in capKeys.keys) for (bits in 0 until 16) {
                val meta = bits and 1 != 0
                val ctrl = bits and 2 != 0
                val shift = bits and 4 != 0
                val alt = bits and 8 != 0
                if (tabShortcut(key(k, meta, ctrl, shift, alt), isMac) != null) {
                    accepted += capSet(isMac, k, meta, ctrl, shift, alt)
                }
            }
            assertEquals(listed, accepted, "isMac=$isMac")
        }
    }

    @Test
    fun capsListsEveryBindingOfTheSpecTable() {
        val mac = tabShortcutCaps(isMac = true).groupBy({ it.first }, { it.second })
        assertEquals(listOf(listOf("⌘", "T")), mac["New tab"])
        assertEquals(listOf(listOf("⌘", "W")), mac["Close tab"])
        assertEquals(
            listOf(listOf("⌃", "Tab"), listOf("⌃", "PageDown"), listOf("⌘", "⌥", "→"), listOf("⌘", "⇧", "]")),
            mac["Next tab"],
        )
        assertEquals(
            listOf(listOf("⌃", "⇧", "Tab"), listOf("⌃", "PageUp"), listOf("⌘", "⌥", "←"), listOf("⌘", "⇧", "[")),
            mac["Previous tab"],
        )
        assertEquals(listOf(listOf("⌘", "["), listOf("⌘", "←")), mac["Back"])
        assertEquals(listOf(listOf("⌘", "]"), listOf("⌘", "→")), mac["Forward"])
        assertEquals(listOf(listOf("⌘", "9")), mac["Go to last tab"])
        assertEquals(listOf(listOf("⌘", "⇧", "W")), mac["Close window"])

        val win = tabShortcutCaps(isMac = false).groupBy({ it.first }, { it.second })
        assertEquals(listOf(listOf("Ctrl", "W"), listOf("Ctrl", "F4")), win["Close tab"])
        assertEquals(listOf(listOf("Ctrl", "Tab"), listOf("Ctrl", "PageDown")), win["Next tab"])
        assertEquals(listOf(listOf("Ctrl", "Shift", "Tab"), listOf("Ctrl", "PageUp")), win["Previous tab"])
        assertEquals(listOf(listOf("Alt", "←")), win["Back"])
        assertEquals(listOf(listOf("Alt", "→")), win["Forward"])
        assertEquals(listOf(listOf("Ctrl", "Shift", "W")), win["Close window"])
    }
}
