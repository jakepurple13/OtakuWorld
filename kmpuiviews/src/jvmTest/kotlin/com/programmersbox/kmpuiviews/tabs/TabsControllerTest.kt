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
    fun homeIsPinnedAndCannotClose() = eventsTest { events ->
        assertEquals(1, controller.windows.size)
        assertTrue(main.isMain)
        val homeTab = main.tabs.single()
        assertTrue(homeTab.pinned)
        assertEquals(Screen.RecentScreen, homeTab.current)
        assertEquals(controller, home.interceptor)

        controller.close(homeId)

        assertEquals(listOf(homeId), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertEquals(emptyList(), events)
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
        val ids = listOf("A", "B", "C", "B", "D").map {
            controller.open(title(it), TabOpenMode.Background, homeId)
        }
        val (a, b, c, b2, d) = ids

        assertEquals(b, b2)
        assertEquals(listOf(homeId, a, b, c, d), main.tabs.map { it.id })
        assertEquals(
            listOf<NavKey>(Screen.RecentScreen, title("A"), title("B"), title("C"), title("D")),
            main.currents(),
        )
        assertEquals(homeId, main.activeId.value)
        assertEquals(
            listOf<TabEvent>(
                TabEvent.Opened(a, main.id, TabOpenMode.Background, null),
                TabEvent.Opened(b, main.id, TabOpenMode.Background, null),
                TabEvent.Opened(c, main.id, TabOpenMode.Background, null),
                TabEvent.Flash(b),
                TabEvent.Opened(d, main.id, TabOpenMode.Background, null),
            ),
            events,
        )
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
        assertEquals(listOf<TabEvent>(TabEvent.Flash(a), TabEvent.Flash(a)), events)
    }

    @Test
    fun navigatingWithinOwnTabIsNotDeduped() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val nav = tab(a).nav
        events.clear()

        nav.navigate(title("A"))
        nav.navigate(Screen.HistoryScreen)

        assertEquals(listOf<NavKey>(title("A"), title("A"), Screen.HistoryScreen), nav.backStack.toList())
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
        assertEquals(a, main.activeId.value)
        assertEquals(emptyList(), events)
    }

    @Test
    fun settingsFamilyPushesOntoExistingSettingsTab() = eventsTest { events ->
        val s = controller.open(Screen.Settings, TabOpenMode.Background, homeId)
        events.clear()

        home.navigate(Screen.Settings.Sources)

        assertEquals(listOf<NavKey>(Screen.Settings, Screen.Settings.Sources), tab(s).nav.backStack.toList())
        assertEquals(listOf(homeId, s), main.tabs.map { it.id })
        assertEquals(s, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen), home.backStack.toList())
        assertEquals(listOf<TabEvent>(TabEvent.Flash(s)), events)
    }

    @Test
    fun settingsFamilyOpensNewTabWhenNoneExists() = eventsTest { events ->
        home.navigate(Screen.Settings.Sources)

        assertEquals(listOf<NavKey>(Screen.RecentScreen, Screen.Settings.Sources), main.currents())
        val s = main.tabs[1].id
        assertEquals(s, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen), home.backStack.toList())
        assertEquals(listOf<TabEvent>(TabEvent.Opened(s, main.id, TabOpenMode.Foreground, null)), events)
    }

    @Test
    fun settingsSubpagesInsideSettingsTabPushNormally() = eventsTest { events ->
        val s = controller.open(Screen.Settings, TabOpenMode.Foreground, homeId)
        val nav = tab(s).nav
        events.clear()

        nav.navigate(Screen.Settings.Sources)
        nav.navigate(Screen.Settings.Colors)

        assertEquals(
            listOf<NavKey>(Screen.Settings, Screen.Settings.Sources, Screen.Settings.Colors),
            nav.backStack.toList(),
        )
        assertEquals(listOf(homeId, s), main.tabs.map { it.id })
        assertEquals(s, main.activeId.value)
        assertEquals(emptyList(), events)
    }

    @Test
    fun searchReusesSearchTabWithNewQuery() = eventsTest { events ->
        val s = controller.open(Screen.GlobalSearchScreen("a"), TabOpenMode.Background, homeId)
        events.clear()

        home.globalSearch("b")

        assertEquals(
            listOf<NavKey>(Screen.GlobalSearchScreen("a"), Screen.GlobalSearchScreen("b")),
            tab(s).nav.backStack.toList(),
        )
        assertEquals(listOf(homeId, s), main.tabs.map { it.id })
        assertEquals(s, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen), home.backStack.toList())
        assertEquals(listOf<TabEvent>(TabEvent.Flash(s)), events)
    }

    @Test
    fun clickOpensNewTabPrefOpensTitlesInForeground() = eventsTest { events ->
        controller.prefs = TabPrefsSnapshot(clickOpensNewTab = true)

        home.navigate(title("A"))

        assertEquals(listOf<NavKey>(Screen.RecentScreen, title("A")), main.currents())
        val a = main.tabs[1].id
        assertEquals(a, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen), home.backStack.toList())
        assertEquals(listOf<TabEvent>(TabEvent.Opened(a, main.id, TabOpenMode.Foreground, null)), events)

        home.navigate(Screen.HistoryScreen)

        // Pages without a Title key still navigate in place.
        assertEquals(listOf<NavKey>(Screen.RecentScreen, Screen.HistoryScreen), home.backStack.toList())
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertEquals(listOf<TabEvent>(TabEvent.Opened(a, main.id, TabOpenMode.Foreground, null)), events)
    }

    @Test
    fun armSendsNextNavigationToNewTab() = eventsTest { events ->
        val origin = Rect(0f, 0f, 10f, 10f)
        controller.arm(homeId, TabOpenMode.Background, origin)

        home.navigate(title("A"))
        home.navigate(title("B"))

        val a = main.tabs[1].id
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
        assertEquals(listOf<NavKey>(title("B"), title("A")), main.currents())
        assertEquals(listOf<NavKey>(Screen.RecentScreen, title("B")), home.backStack.toList())
        assertEquals(homeId, main.activeId.value)
        assertEquals(listOf<TabEvent>(TabEvent.Opened(a, main.id, TabOpenMode.Background, origin)), events)
    }

    @Test
    fun modClickArmThenTabNavigationOpensBackgroundTab() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        events.clear()
        // A window-level ⌘-click arms without an origin; an element's target re-arms the same mode with its bounds.
        controller.armClick(a, TabOpenMode.Background, null)
        val origin = Rect(0f, 0f, 10f, 10f)
        controller.armClick(a, TabOpenMode.Background, origin)

        // The element's own click navigates through the tab's nav a moment later (a chooser pick, a load).
        time += 3_000
        tab(a).nav.details(title = "B", description = "d", url = "url-B", imageUrl = "img-B", source = "src")

        val b = main.tabs.last().id
        assertEquals(listOf(homeId, a, b), main.tabs.map { it.id })
        assertEquals(a, main.activeId.value)
        assertEquals(title("A"), tab(a).current)
        assertEquals(listOf<TabEvent>(TabEvent.Opened(b, main.id, TabOpenMode.Background, origin)), events)

        // One arm, one tab: the next plain navigation stays in A.
        tab(a).nav.navigate(title("C"))
        assertEquals(title("C"), tab(a).current)
        assertEquals(3, main.tabs.size)
    }

    @Test
    fun armExpiresAfterTenSeconds() = eventsTest { events ->
        controller.arm(homeId, TabOpenMode.Background, null)
        time += 10_001

        home.navigate(title("A"))

        assertEquals(listOf(homeId), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen, title("A")), home.backStack.toList())
        assertEquals(emptyList(), events)
    }

    @Test
    fun navigationOnInactiveTabActivatesIt() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        events.clear()

        home.navigate(Screen.HistoryScreen)

        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertEquals(listOf<NavKey>(Screen.RecentScreen, Screen.HistoryScreen), home.backStack.toList())
        assertEquals(emptyList(), events)
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
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
        assertEquals(a, main.activeId.value)
        assertEquals(listOf<TabEvent>(TabEvent.Closed(b, main.id), TabEvent.Closed(c, main.id)), events)
    }

    @Test
    fun closedTabKeepsItsStoreUntilItsContentLeavesComposition() {
        val a = controller.open(title("A"), TabOpenMode.Background, homeId)
        val vm = object : ViewModel() {
            var cleared = false
            override fun onCleared() {
                cleared = true
            }
        }
        tab(a).viewModelStoreOwner.viewModelStore.put("vm", vm)
        val state = tab(a)
        controller.attachContent(a)

        controller.close(a)
        assertFalse(vm.cleared)
        controller.detachContent(a)
        assertTrue(vm.cleared)
        assertTrue(state.viewModelStoreOwner.viewModelStore.keys().isEmpty())
    }

    @Test
    fun closedStackReopensMostRecentFirst() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Background, homeId)
        val b = controller.open(title("B"), TabOpenMode.Background, homeId)
        tab(b).nav.navigate(Screen.HistoryScreen)
        controller.activate(homeId)
        events.clear()

        controller.close(a)
        controller.close(b)
        assertEquals(listOf(homeId), main.tabs.map { it.id })
        assertEquals(listOf<TabEvent>(TabEvent.Closed(a, main.id), TabEvent.Closed(b, main.id)), events)
        events.clear()

        val first = controller.reopenClosed(main.id)!!
        assertEquals(listOf<NavKey>(title("B"), Screen.HistoryScreen), tab(first).nav.backStack.toList())
        assertEquals(first, main.activeId.value)
        assertEquals(controller, tab(first).nav.interceptor)

        val second = controller.reopenClosed(main.id)!!
        assertEquals(title("A"), tab(second).current)
        assertNull(controller.reopenClosed(main.id))
        assertEquals(listOf(homeId, first, second), main.tabs.map { it.id })
        assertEquals(second, main.activeId.value)
        assertEquals(
            listOf<TabEvent>(
                TabEvent.Opened(first, main.id, TabOpenMode.Foreground, null),
                TabEvent.Opened(second, main.id, TabOpenMode.Foreground, null),
            ),
            events,
        )
    }

    @Test
    fun reopenClosedTitleOpenElsewhereSwitchesAndFlashes() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Background, homeId)
        controller.close(a)
        val again = controller.open(title("A"), TabOpenMode.Background, homeId)
        events.clear()

        assertEquals(again, controller.reopenClosed(main.id))

        assertEquals(listOf(homeId, again), main.tabs.map { it.id })
        assertEquals(again, main.activeId.value)
        assertEquals(listOf<TabEvent>(TabEvent.Flash(again)), events)
        assertNull(controller.reopenClosed(main.id))
    }

    @Test
    fun reopenClosedSettingsReusesSettingsTab() = eventsTest { events ->
        val s = controller.open(Screen.Settings, TabOpenMode.Foreground, homeId)
        tab(s).nav.navigate(Screen.Settings.Sources)
        controller.close(s)
        home.navigate(Screen.Settings)
        val again = main.tabs[1].id
        controller.activate(homeId)
        events.clear()

        assertEquals(again, controller.reopenClosed(main.id))

        assertEquals(listOf(homeId, again), main.tabs.map { it.id })
        assertEquals(again, main.activeId.value)
        assertEquals(listOf<TabEvent>(TabEvent.Flash(again)), events)
    }

    @Test
    fun backMovesToForwardAndForwardRestores() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val t = tab(a)
        t.nav.navigate(Screen.HistoryScreen)
        events.clear()

        controller.back(a)
        assertEquals(listOf<NavKey>(title("A")), t.nav.backStack.toList())
        assertEquals(listOf<NavKey>(Screen.HistoryScreen), t.forward.toList())

        controller.back(a)
        assertEquals(listOf<NavKey>(title("A")), t.nav.backStack.toList())

        controller.forward(a)
        assertEquals(listOf<NavKey>(title("A"), Screen.HistoryScreen), t.nav.backStack.toList())
        assertTrue(t.forward.isEmpty())
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
        assertEquals(a, main.activeId.value)
        assertEquals(emptyList(), events)
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
        assertEquals(emptyList(), events)
    }

    @Test
    fun newNavigationClearsForward() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val t = tab(a)
        t.nav.navigate(Screen.HistoryScreen)
        controller.back(a)
        assertEquals(1, t.forward.size)
        events.clear()

        t.nav.navigate(Screen.FavoriteScreen)

        assertTrue(t.forward.isEmpty())
        assertEquals(listOf<NavKey>(title("A"), Screen.FavoriteScreen), t.nav.backStack.toList())
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
        assertEquals(a, main.activeId.value)
        assertEquals(emptyList(), events)
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
    fun mergingATornTabBackLeavesItsWindowEmptyAndSafeToDraw() = eventsTest { _ ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val w2 = controller.tearOut(a, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))
        // The torn window's own composition still holds this state until the app composition drops the window.
        val torn = controller.windows.first { it.id == w2 }

        controller.move(a, main.id, 1)

        assertTrue(controller.windows.none { it.id == w2 })
        assertEquals(emptyList(), torn.tabs.toList())
        assertNull(activeTabOf(torn))
        assertEquals(a, activeTabOf(main)?.id)
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
    fun newWindowModeOpensInNewWindow() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.NewWindow, homeId)

        assertEquals(2, controller.windows.size)
        val second = controller.windowOf(a)
        assertFalse(second.isMain)
        assertEquals(listOf(a), second.tabs.map { it.id })
        assertEquals(a, second.activeId.value)
        assertEquals(second.id, controller.focusedWindow)
        assertEquals(listOf(homeId), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertEquals(listOf<TabEvent>(TabEvent.Opened(a, second.id, TabOpenMode.NewWindow, null)), events)
    }

    @Test
    fun closingLastTabOfTornWindowClosesWindow() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val w2 = controller.tearOut(a, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))
        events.clear()

        controller.close(a)

        assertEquals(listOf(main.id), controller.windows.map { it.id })
        assertEquals(main.id, controller.focusedWindow)
        assertEquals(listOf(homeId), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertEquals(listOf<TabEvent>(TabEvent.Closed(a, w2)), events)
    }

    @Test
    fun mainWindowCannotLoseHome() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val w2 = controller.tearOut(a, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))
        events.clear()

        controller.move(homeId, w2, 0)
        assertEquals(main.id, controller.tearOut(homeId, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp)))
        controller.close(homeId)

        assertEquals(listOf(main.id, w2), controller.windows.map { it.id })
        assertEquals(listOf(homeId), main.tabs.map { it.id })
        assertEquals(homeId, main.activeId.value)
        assertEquals(listOf(a), controller.windowOf(a).tabs.map { it.id })
        assertEquals(a, controller.windowOf(a).activeId.value)
        assertEquals(emptyList(), events)
    }

    @Test
    fun modClickForegroundSwapsArmModes() = eventsTest { events ->
        // Default: ⌘-click (Background) stays in the background.
        controller.armClick(homeId, TabOpenMode.Background, null)
        home.navigate(title("A"))
        assertEquals(homeId, main.activeId.value)

        controller.prefs = controller.prefs.copy(modClick = ModClick.Foreground)
        controller.armClick(homeId, TabOpenMode.Background, null)
        home.navigate(title("B"))
        assertEquals(title("B"), tab(main.activeId.value).current)

        controller.activate(homeId)
        controller.armClick(homeId, TabOpenMode.Foreground, null)
        home.navigate(title("C"))
        assertEquals(homeId, main.activeId.value)

        // The right-click menu's explicit choice is never swapped.
        controller.arm(homeId, TabOpenMode.Background, null)
        home.navigate(title("D"))
        assertEquals(homeId, main.activeId.value)
        assertEquals(
            listOf(TabOpenMode.Background, TabOpenMode.Foreground, TabOpenMode.Background, TabOpenMode.Background),
            events.filterIsInstance<TabEvent.Opened>().map { it.mode },
        )
    }

    @Test
    fun atEndAppendsNewTabs() = eventsTest {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val b = controller.open(title("B"), TabOpenMode.Foreground, a)
        controller.activate(homeId)
        controller.prefs = controller.prefs.copy(newPlace = NewTabPlace.AtEnd)

        val c = controller.open(title("C"), TabOpenMode.Background, homeId)
        val d = controller.open(title("D"), TabOpenMode.Foreground, homeId)

        assertEquals(listOf(homeId, a, b, c, d), main.tabs.map { it.id })
        assertEquals(d, main.activeId.value)
    }

    @Test
    fun afterCloseRightIsTheDefaultAndFallsBackLeft() = eventsTest {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val b = controller.open(title("B"), TabOpenMode.Foreground, a)
        val c = controller.open(title("C"), TabOpenMode.Foreground, b)

        controller.activate(b)
        controller.close(b)
        assertEquals(c, main.activeId.value)

        controller.close(c)
        assertEquals(a, main.activeId.value)
    }

    @Test
    fun afterCloseLeftPrefersLeft() = eventsTest {
        controller.prefs = controller.prefs.copy(afterClose = AfterClose.Left)
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val b = controller.open(title("B"), TabOpenMode.Foreground, a)
        val c = controller.open(title("C"), TabOpenMode.Foreground, b)

        controller.activate(b)
        controller.close(b)
        assertEquals(a, main.activeId.value)

        controller.close(a)
        assertEquals(homeId, main.activeId.value)

        // Nothing to the left: falls back to the right.
        val w2 = controller.tearOut(c, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))
        val d = controller.open(title("D"), TabOpenMode.Foreground, c)
        controller.move(d, w2, 0)
        assertEquals(listOf(d, c), controller.windowOf(c).tabs.map { it.id })
        controller.close(d)
        assertEquals(c, controller.windowOf(c).activeId.value)
    }

    @Test
    fun afterCloseLastUsedPicksMostRecentlyActive() = eventsTest {
        controller.prefs = controller.prefs.copy(afterClose = AfterClose.LastUsed)
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val b = controller.open(title("B"), TabOpenMode.Foreground, a)
        val c = controller.open(title("C"), TabOpenMode.Foreground, b)
        val d = controller.open(title("D"), TabOpenMode.Foreground, c)

        controller.activate(a)
        controller.activate(d)
        controller.activate(b)
        controller.close(b)
        assertEquals(d, main.activeId.value)

        // A tab closed in the background drops out of the order too.
        controller.close(a)
        controller.close(d)
        assertEquals(c, main.activeId.value)

        // Closing a background tab keeps the active one.
        val e = controller.open(title("E"), TabOpenMode.Background, c)
        controller.close(e)
        assertEquals(c, main.activeId.value)
    }

    @Test
    fun closedStackCappedAtSetting() = eventsTest {
        controller.prefs = controller.prefs.copy(closedKept = 10)
        val ids = (1..12).map { controller.open(title("T$it"), TabOpenMode.Background, homeId) }
        ids.forEach { controller.close(it) }

        val reopened = generateSequence { controller.reopenClosed(main.id) }.toList()

        assertEquals(10, reopened.size)
        assertEquals((3..12).reversed().map { title("T$it") }, reopened.map { tab(it).current })
    }

    @Test
    fun toolbarDownloadsHiddenWhenNoDestination() {
        assertNull(controller.downloadsKey)

        val withDownloads = TabsController(
            homeNav = Navigation3Actions(Screen.RecentScreen),
            keys = TabKeys(listOf(CoreTabKeyProvider)),
            prefs = TabPrefsSnapshot(clickOpensNewTab = false),
            downloadsKey = Screen.HistoryScreen,
        )
        assertEquals(Screen.HistoryScreen, withDownloads.downloadsKey)
    }

    @Test
    fun newTabRouteIsNeverDeduped() {
        val one = controller.open(NewTabRoute, TabOpenMode.Foreground, homeId)
        val two = controller.open(NewTabRoute, TabOpenMode.Foreground, one)

        assertTrue(one != two)
        assertEquals(listOf<NavKey>(Screen.RecentScreen, NewTabRoute, NewTabRoute), main.currents())
    }

    @Test
    fun recentlyClosedListsMostRecentFirstAndReopensTheChosenEntry() {
        val ids = listOf("A", "B", "C").map { controller.open(title(it), TabOpenMode.Background, homeId) }
        ids.forEach { controller.close(it) }

        assertEquals(listOf("C", "B", "A").map { title(it) }, controller.recentlyClosed.map { it.stack.last() })

        val b = controller.reopenClosed(main.id, controller.recentlyClosed[1])!!

        assertEquals(title("B"), tab(b).current)
        assertEquals(b, main.activeId.value)
        assertEquals(listOf("C", "A").map { title(it) }, controller.recentlyClosed.map { it.stack.last() })
    }

    @Test
    fun newTabPageGivesWayToAnExistingTab() = eventsTest { events ->
        val search = controller.open(Screen.GlobalSearchScreen(), TabOpenMode.Foreground, homeId)
        val newTab = controller.open(NewTabRoute, TabOpenMode.Foreground, search)
        assertEquals(listOf(homeId, search, newTab), main.tabs.map { it.id })
        events.clear()

        tab(newTab).nav.globalSearch("one piece")

        assertEquals(listOf(homeId, search), main.tabs.map { it.id })
        assertEquals(search, main.activeId.value)
        assertEquals(Screen.GlobalSearchScreen("one piece"), tab(search).current)
        assertEquals(listOf<TabEvent>(TabEvent.Flash(search), TabEvent.Closed(newTab, main.id)), events)
        assertTrue(controller.recentlyClosed.isEmpty())
    }

    @Test
    fun newTabPageBecomesASingleInstancePageWithNoTabYet() = eventsTest { events ->
        val newTab = controller.open(NewTabRoute, TabOpenMode.Foreground, homeId)
        events.clear()

        tab(newTab).nav.customList()

        assertEquals(listOf(homeId, newTab), main.tabs.map { it.id })
        assertEquals(newTab, main.activeId.value)
        assertEquals(listOf<NavKey>(NewTabRoute, Screen.CustomListScreen), tab(newTab).nav.backStack.toList())
        assertEquals(emptyList(), events)
    }

    @Test
    fun aNewTabPageThatNavigatedKeepsTheUsualRules() = eventsTest { events ->
        val newTab = controller.open(NewTabRoute, TabOpenMode.Foreground, homeId)
        tab(newTab).nav.navigate(Screen.HistoryScreen)
        events.clear()

        tab(newTab).nav.navigate(Screen.Settings)

        val settings = main.tabs.last().id
        assertEquals(listOf(homeId, newTab, settings), main.tabs.map { it.id })
        assertEquals(Screen.Settings, tab(settings).current)
        assertEquals(Screen.HistoryScreen, tab(newTab).current)
        assertEquals(listOf<TabEvent>(TabEvent.Opened(settings, main.id, TabOpenMode.Foreground, null)), events)
    }

    @Test
    fun reopeningFromTheNewTabPageTakesItsPlace() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        controller.close(a)
        val x = controller.open(title("X"), TabOpenMode.Foreground, homeId)
        val newTab = controller.open(NewTabRoute, TabOpenMode.Foreground, x)
        val y = controller.open(title("Y"), TabOpenMode.Background, newTab)
        assertEquals(listOf(homeId, x, newTab, y), main.tabs.map { it.id })
        events.clear()

        val reopened = controller.reopenClosed(main.id, controller.recentlyClosed.single(), replacing = newTab)!!

        assertEquals(listOf(homeId, x, reopened, y), main.tabs.map { it.id })
        assertEquals(title("A"), tab(reopened).current)
        assertEquals(reopened, main.activeId.value)
        assertEquals(
            listOf<TabEvent>(
                TabEvent.Opened(reopened, main.id, TabOpenMode.Foreground, null),
                TabEvent.Closed(newTab, main.id),
            ),
            events,
        )
        assertTrue(controller.recentlyClosed.isEmpty())
    }

    @Test
    fun reopeningAnOpenTitleFromTheNewTabPageSwitchesAndCloses() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        controller.close(a)
        val again = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val newTab = controller.open(NewTabRoute, TabOpenMode.Foreground, again)
        events.clear()

        assertEquals(again, controller.reopenClosed(main.id, controller.recentlyClosed.single(), replacing = newTab))

        assertEquals(listOf(homeId, again), main.tabs.map { it.id })
        assertEquals(again, main.activeId.value)
        assertEquals(listOf<TabEvent>(TabEvent.Flash(again), TabEvent.Closed(newTab, main.id)), events)
        assertTrue(controller.recentlyClosed.isEmpty())
    }

    @Test
    fun dropOnOwnStripReorders() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val b = controller.open(title("B"), TabOpenMode.Foreground, a)
        val c = controller.open(title("C"), TabOpenMode.Foreground, b)
        controller.activate(a)
        events.clear()

        // Insertion index 4 is after C (the slots count A's own).
        controller.drop(a, DropTarget.Reorder(main.id, 4))
        assertEquals(listOf(homeId, b, c, a), main.tabs.map { it.id })
        controller.drop(c, DropTarget.Reorder(main.id, 1))
        assertEquals(listOf(homeId, c, b, a), main.tabs.map { it.id })

        assertEquals(1, controller.windows.size)
        assertEquals(c, main.activeId.value)
        assertEquals(emptyList(), events)
    }

    @Test
    fun dropIndexNeverBeforeHome() = eventsTest {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val b = controller.open(title("B"), TabOpenMode.Foreground, a)
        val w2 = controller.tearOut(b, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))

        controller.drop(a, DropTarget.Reorder(main.id, 0))
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })

        controller.drop(b, DropTarget.MoveTo(main.id, 0))
        assertEquals(listOf(homeId, b, a), main.tabs.map { it.id })
        assertEquals(listOf(main.id), controller.windows.map { it.id })
        assertFalse(w2 in controller.windows.map { it.id })

        // Home itself never moves.
        controller.drop(homeId, DropTarget.Reorder(main.id, 3))
        assertEquals(listOf(homeId, b, a), main.tabs.map { it.id })
    }

    @Test
    fun tearOutFromSingleTabWindowJustMovesWindow() = eventsTest { events ->
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val w2 = controller.tearOut(a, WindowBounds(0.dp, 0.dp, 800.dp, 600.dp))
        controller.focus(main.id)
        events.clear()

        val moved = WindowBounds(400.dp, 300.dp, 800.dp, 600.dp)
        assertEquals(w2, controller.tearOut(a, moved))

        assertEquals(listOf(main.id, w2), controller.windows.map { it.id })
        val second = controller.windows.first { it.id == w2 }
        assertEquals(listOf(a), second.tabs.map { it.id })
        assertEquals(moved, second.bounds.value)
        assertEquals(w2, controller.focusedWindow)
        assertEquals(emptyList(), events)
    }
}
