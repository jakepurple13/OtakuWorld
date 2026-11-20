package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TabsControllerTest {

    private var time = 1_000L
    private val home = Navigation3Actions(Screen.RecentScreen)
    private val controller = TabsController(
        homeNav = home,
        keys = TabKeys(listOf(CoreTabKeyProvider)),
        prefs = TabPrefsSnapshot(clickOpensNewTab = false),
        now = { time },
    )

    private fun title(name: String) = Screen.DetailsScreen.Details(
        title = name,
        description = "d",
        url = "url-$name",
        imageUrl = "img-$name",
        source = "src",
    )

    private val main get() = controller.windows.first()
    private val homeId get() = main.tabs.first().id

    private fun BrowserWindowState.currents(): List<NavKey> = tabs.map { it.current }
    private fun tab(id: TabId) = controller.windowOf(id).tabs.first { it.id == id }

    private fun eventsTest(block: suspend TestScope.(MutableList<TabEvent>) -> Unit) =
        runTest(UnconfinedTestDispatcher()) {
            val events = mutableListOf<TabEvent>()
            controller.events.onEach { events += it }.launchIn(backgroundScope)
            block(events)
        }

    @Test
    fun homeIsPinnedAndCannotClose() {
        assertEquals(1, controller.windows.size)
        assertTrue(main.isMain)
        val homeTab = main.tabs.single()
        assertTrue(homeTab.pinned)
        assertEquals(Screen.RecentScreen, homeTab.current)
        assertEquals(controller, home.interceptor)

        controller.close(homeId)

        assertEquals(listOf(homeId), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
    }

    @Test
    fun backgroundOpenInsertsRightOfActiveWithoutActivating() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        controller.activate(homeId)
        val b = controller.open(title("B"), TabOpenMode.Background, homeId)

        assertEquals(listOf(homeId, b, a), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertEquals(
            listOf<TabEvent>(
                TabEvent.Opened(a, main.id, TabOpenMode.Foreground, null),
                TabEvent.Opened(b, main.id, TabOpenMode.Background, null),
            ),
            events,
        )
    }

    @Test
    fun rapidBackgroundOpensKeepOrderAndDedupe() = eventsTest { events ->
        listOf("A", "B", "C", "B", "D").forEach {
            controller.open(title(it), TabOpenMode.Background, homeId)
        }

        assertEquals(
            listOf<NavKey>(Screen.RecentScreen, title("A"), title("B"), title("C"), title("D")),
            main.currents(),
        )
        assertEquals(homeId, main.activeId.value)
        assertEquals(4, events.count { it is TabEvent.Opened })
        val flash = events.filterIsInstance<TabEvent.Flash>().single()
        assertEquals(title("B"), tab(flash.tab).current)
    }

    @Test
    fun foregroundOpenActivates() = eventsTest { events ->
        val origin = Rect(1f, 2f, 3f, 4f)
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId, origin)

        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
        assertEquals(a, main.activeId.value)
        assertEquals(listOf<NavKey>(title("A")), tab(a).nav.backStack.toList())
        assertEquals(controller, tab(a).nav.interceptor)
        assertEquals(listOf<TabEvent>(TabEvent.Opened(a, main.id, TabOpenMode.Foreground, origin)), events)
    }

    @Test
    fun plainNavigateToTitleOpenElsewhereSwitchesAndFlashes() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Background, homeId)
        events.clear()

        val handled = controller.intercept(home, title("A"))

        assertTrue(handled)
        assertEquals(a, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen), home.backStack.toList())
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
        assertEquals(listOf<TabEvent>(TabEvent.Flash(a)), events)

        // Same through the real push path.
        controller.activate(homeId)
        home.navigate(title("A"))
        assertEquals(a, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen), home.backStack.toList())
    }

    @Test
    fun navigatingWithinOwnTabIsNotDeduped() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val nav = tab(a).nav

        nav.navigate(title("A"))
        nav.navigate(Screen.HistoryScreen)

        assertEquals(listOf<NavKey>(title("A"), title("A"), Screen.HistoryScreen), nav.backStack.toList())
        assertEquals(2, main.tabs.size)
    }

    @Test
    fun settingsFamilyPushesOntoExistingSettingsTab() = eventsTest { events ->
        val s = controller.open(Screen.Settings, TabOpenMode.Background, homeId)
        events.clear()

        home.navigate(Screen.Settings.Sources)

        assertEquals(listOf<NavKey>(Screen.Settings, Screen.Settings.Sources), tab(s).nav.backStack.toList())
        assertEquals(s, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen), home.backStack.toList())
        assertEquals(listOf<TabEvent>(TabEvent.Flash(s)), events)
    }

    @Test
    fun settingsFamilyOpensNewTabWhenNoneExists() {
        home.navigate(Screen.Settings.Sources)

        assertEquals(listOf<NavKey>(Screen.RecentScreen, Screen.Settings.Sources), main.currents())
        assertEquals(main.tabs[1].id, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen), home.backStack.toList())
    }

    @Test
    fun settingsSubpagesInsideSettingsTabPushNormally() {
        val s = controller.open(Screen.Settings, TabOpenMode.Foreground, homeId)
        val nav = tab(s).nav

        nav.navigate(Screen.Settings.Sources)
        nav.navigate(Screen.Settings.Colors)

        assertEquals(
            listOf<NavKey>(Screen.Settings, Screen.Settings.Sources, Screen.Settings.Colors),
            nav.backStack.toList(),
        )
        assertEquals(2, main.tabs.size)
    }

    @Test
    fun searchReusesSearchTabWithNewQuery() {
        val s = controller.open(Screen.GlobalSearchScreen("a"), TabOpenMode.Background, homeId)

        home.globalSearch("b")

        assertEquals(
            listOf<NavKey>(Screen.GlobalSearchScreen("a"), Screen.GlobalSearchScreen("b")),
            tab(s).nav.backStack.toList(),
        )
        assertEquals(s, main.activeId.value)
        assertEquals(2, main.tabs.size)
    }

    @Test
    fun clickOpensNewTabPrefOpensTitlesInForeground() {
        controller.prefs = TabPrefsSnapshot(clickOpensNewTab = true)

        home.navigate(title("A"))

        assertEquals(listOf<NavKey>(Screen.RecentScreen, title("A")), main.currents())
        assertEquals(main.tabs[1].id, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen), home.backStack.toList())

        home.navigate(Screen.HistoryScreen)
        // Pages without a Title key still navigate in place.
        assertEquals(listOf<NavKey>(Screen.RecentScreen, Screen.HistoryScreen), home.backStack.toList())
    }

    @Test
    fun armSendsNextNavigationToNewTab() = eventsTest { events ->
        val origin = Rect(0f, 0f, 10f, 10f)
        controller.arm(homeId, TabOpenMode.Background, origin)

        home.navigate(title("A"))
        home.navigate(title("B"))

        assertEquals(listOf<NavKey>(title("B"), title("A")), main.currents())
        assertEquals(listOf<NavKey>(Screen.RecentScreen, title("B")), home.backStack.toList())
        assertEquals(homeId, main.activeId.value)
        val opened = events.filterIsInstance<TabEvent.Opened>().single()
        assertEquals(TabOpenMode.Background, opened.mode)
        assertEquals(origin, opened.origin)
    }

    @Test
    fun armExpiresAfterTenSeconds() {
        controller.arm(homeId, TabOpenMode.Background, null)
        time += 10_001

        home.navigate(title("A"))

        assertEquals(1, main.tabs.size)
        assertEquals(listOf<NavKey>(Screen.RecentScreen, title("A")), home.backStack.toList())
    }

    @Test
    fun navigationOnInactiveTabActivatesIt() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)

        home.navigate(Screen.HistoryScreen)

        assertEquals(homeId, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen, Screen.HistoryScreen), home.backStack.toList())
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
    }

    @Test
    fun closeActivatesRightNeighborElseLeft() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Background, homeId)
        val b = controller.open(title("B"), TabOpenMode.Background, homeId)
        val c = controller.open(title("C"), TabOpenMode.Background, homeId)
        controller.activate(b)
        val vm = object : ViewModel() {
            var cleared = false
            override fun onCleared() {
                cleared = true
            }
        }
        tab(b).viewModelStoreOwner.viewModelStore.put("vm", vm)
        events.clear()

        controller.close(b)
        assertEquals(listOf(homeId, a, c), main.tabs.map { it.id })
        assertEquals(c, main.activeId.value)
        assertTrue(vm.cleared)
        assertEquals(listOf<TabEvent>(TabEvent.Closed(b, main.id)), events)

        controller.close(c)
        assertEquals(a, main.activeId.value)
    }

    @Test
    fun closedStackReopensMostRecentFirst() {
        val a = controller.open(title("A"), TabOpenMode.Background, homeId)
        val b = controller.open(title("B"), TabOpenMode.Background, homeId)
        tab(b).nav.navigate(Screen.HistoryScreen)
        controller.close(a)
        controller.close(b)

        val first = controller.reopenClosed(main.id)!!
        assertEquals(listOf<NavKey>(title("B"), Screen.HistoryScreen), tab(first).nav.backStack.toList())
        assertEquals(first, main.activeId.value)
        assertEquals(controller, tab(first).nav.interceptor)

        val second = controller.reopenClosed(main.id)!!
        assertEquals(title("A"), tab(second).current)
        assertNull(controller.reopenClosed(main.id))
        assertEquals(3, main.tabs.size)
    }

    @Test
    fun backMovesToForwardAndForwardRestores() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val t = tab(a)
        t.nav.navigate(Screen.HistoryScreen)

        controller.back(a)
        assertEquals(listOf<NavKey>(title("A")), t.nav.backStack.toList())
        assertEquals(listOf<NavKey>(Screen.HistoryScreen), t.forward.toList())

        controller.back(a)
        assertEquals(listOf<NavKey>(title("A")), t.nav.backStack.toList())

        controller.forward(a)
        assertEquals(listOf<NavKey>(title("A"), Screen.HistoryScreen), t.nav.backStack.toList())
        assertTrue(t.forward.isEmpty())
    }

    @Test
    fun backOntoTitleOpenElsewhereStays() = eventsTest { events ->
        val x = controller.open(title("A"), TabOpenMode.Background, homeId)
        tab(x).nav.navigate(title("B"))
        val y = controller.open(title("A"), TabOpenMode.Background, homeId)
        controller.activate(homeId)
        events.clear()

        controller.back(x)

        assertEquals(listOf<NavKey>(title("A")), tab(x).nav.backStack.toList())
        assertEquals(listOf(homeId, x, y), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertTrue(events.isEmpty())
    }

    @Test
    fun newNavigationClearsForward() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val t = tab(a)
        t.nav.navigate(Screen.HistoryScreen)
        controller.back(a)
        assertEquals(1, t.forward.size)

        t.nav.navigate(Screen.FavoriteScreen)

        assertTrue(t.forward.isEmpty())
    }

    @Test
    fun moveToOtherWindowActivatesThere() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val b = controller.open(title("B"), TabOpenMode.Background, homeId)
        val w2 = controller.tearOut(b, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))
        events.clear()

        controller.move(a, w2, 0)

        val second = controller.windows.first { it.id == w2 }
        assertEquals(listOf(a, b), second.tabs.map { it.id })
        assertEquals(a, second.activeId.value)
        assertEquals(w2, controller.focusedWindow)
        assertEquals(listOf(homeId), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertEquals(listOf<TabEvent>(TabEvent.Moved(a, main.id, w2)), events)
    }

    @Test
    fun tearOutCreatesWindowWithoutHome() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val bounds = WindowBounds(10.dp, 20.dp, 800.dp, 600.dp)
        events.clear()

        val w2 = controller.tearOut(a, bounds)

        val second = controller.windows.first { it.id == w2 }
        assertFalse(second.isMain)
        assertEquals(listOf(a), second.tabs.map { it.id })
        assertEquals(a, second.activeId.value)
        assertEquals(bounds, second.bounds.value)
        assertEquals(w2, controller.focusedWindow)
        assertEquals(listOf(homeId), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertEquals(listOf<TabEvent>(TabEvent.Moved(a, main.id, w2)), events)
    }

    @Test
    fun newWindowModeOpensInNewWindow() {
        val a = controller.open(title("A"), TabOpenMode.NewWindow, homeId)

        assertEquals(2, controller.windows.size)
        val second = controller.windowOf(a)
        assertFalse(second.isMain)
        assertEquals(listOf(a), second.tabs.map { it.id })
        assertEquals(second.id, controller.focusedWindow)
        assertEquals(listOf(homeId), main.tabs.map { it.id })
    }

    @Test
    fun closingLastTabOfTornWindowClosesWindow() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        controller.tearOut(a, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))

        controller.close(a)

        assertEquals(listOf(main.id), controller.windows.map { it.id })
        assertEquals(main.id, controller.focusedWindow)
    }

    @Test
    fun mainWindowCannotLoseHome() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val w2 = controller.tearOut(a, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))

        controller.move(homeId, w2, 0)
        controller.tearOut(homeId, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))
        controller.close(homeId)

        assertEquals(2, controller.windows.size)
        assertEquals(listOf(homeId), main.tabs.map { it.id })
        assertEquals(listOf(a), controller.windowOf(a).tabs.map { it.id })
    }
}
