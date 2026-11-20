package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.navactions.NavInterceptor
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Owns every desktop window and tab. Each tab's [Navigation3Actions] routes its pushes through
 * [intercept], which keeps one tab per title and sends single-instance pages to their own tab.
 */
class TabsController(
    homeNav: Navigation3Actions,
    private val keys: TabKeys,
    prefs: TabPrefsSnapshot,
    private val now: () -> Long = System::currentTimeMillis,
) : NavInterceptor {

    var prefs: TabPrefsSnapshot by mutableStateOf(prefs)

    private var nextId = 0L

    val windows: SnapshotStateList<BrowserWindowState> = mutableStateListOf(
        BrowserWindowState(
            id = WindowId(nextId++),
            isMain = true,
            first = TabState(TabId(nextId++), homeNav, pinned = true),
        )
    )

    var focusedWindow: WindowId by mutableStateOf(windows.first().id)
        private set

    private val _events = MutableSharedFlow<TabEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<TabEvent> = _events.asSharedFlow()

    private class Arm(val mode: TabOpenMode, val origin: Rect?, val at: Long)

    private val arms = mutableMapOf<TabId, Arm>()

    /** Per window, the last background tab opened in a row from the active tab. */
    private val backgroundRun = mutableMapOf<WindowId, TabId>()

    /** Back stacks of closed tabs, most recent last. Session only. */
    private val closed = ArrayDeque<List<NavKey>>()

    init {
        homeNav.interceptor = this
    }

    override fun intercept(from: Navigation3Actions, key: NavKey): Boolean {
        val fromTab = allTabs().firstOrNull { it.nav === from } ?: return false

        arms.remove(fromTab.id)?.let { arm ->
            if (now() - arm.at <= ARM_TIMEOUT_MS) {
                open(key, arm.mode, fromTab.id, arm.origin)
                return true
            }
        }

        val tk = keys.keyOf(key)
        if (tk != null) {
            val existing = allTabs().firstOrNull { it !== fromTab && keys.keyOf(it.current) == tk }
            if (existing != null) {
                reuse(existing, key, activate = true)
                return true
            }
            if (tk.isSingleInstanceFamily && keys.keyOf(fromTab.current) != tk) {
                open(key, TabOpenMode.Foreground, fromTab.id)
                return true
            }
        }

        if (prefs.clickOpensNewTab && tk is TabKey.Title) {
            open(key, TabOpenMode.Foreground, fromTab.id)
            return true
        }

        val focused = window(focusedWindow)
        if (focused?.activeId?.value != fromTab.id) activate(fromTab.id)
        fromTab.forward.clear()
        return false
    }

    fun open(key: NavKey, mode: TabOpenMode, from: TabId, origin: Rect? = null): TabId {
        val tk = keys.keyOf(key)
        val existing = tk?.let { allTabs().firstOrNull { tab -> keys.keyOf(tab.current) == tk } }
        if (existing != null) {
            reuse(existing, key, activate = mode != TabOpenMode.Background)
            return existing.id
        }

        val tab = newTab(key)
        if (mode == TabOpenMode.NewWindow) {
            val window = newWindow(tab, DefaultWindowBounds)
            emit(TabEvent.Opened(tab.id, window.id, mode, origin))
            return tab.id
        }

        val window = findTab(from)?.let { windowOf(it.id) } ?: window(focusedWindow)!!
        val after = backgroundRun[window.id]?.takeIf { id -> window.tabs.any { it.id == id } }
            ?: window.activeId.value
        window.tabs.add(window.tabs.indexOfFirst { it.id == after } + 1, tab)

        if (mode == TabOpenMode.Foreground) {
            activate(tab.id)
            backgroundRun.remove(window.id)
        } else {
            backgroundRun[window.id] = tab.id
        }
        emit(TabEvent.Opened(tab.id, window.id, mode, origin))
        return tab.id
    }

    fun activate(tab: TabId) {
        val window = findTab(tab)?.let { windowOf(it.id) } ?: return
        if (window.activeId.value != tab) {
            window.activeId.value = tab
            backgroundRun.remove(window.id)
        }
        focusedWindow = window.id
    }

    fun close(tab: TabId) {
        val state = findTab(tab) ?: return
        if (state.pinned) return
        val window = windowOf(tab)

        detach(state, window)
        arms.remove(tab)
        closed.addLast(state.nav.backStack.toList())
        state.viewModelStoreOwner.viewModelStore.clear()
        emit(TabEvent.Closed(tab, window.id))
    }

    fun reopenClosed(window: WindowId): TabId? {
        val target = window(window) ?: return null
        val stack = closed.removeLastOrNull() ?: return null
        val tk = keys.keyOf(stack.last())
        val existing = tk?.let { allTabs().firstOrNull { tab -> keys.keyOf(tab.current) == tk } }
        if (existing != null) {
            activate(existing.id)
            emit(TabEvent.Flash(existing.id))
            return existing.id
        }

        val tab = newTab(stack.first())
        stack.drop(1).forEach { tab.nav.navBackStack.add(it) }

        target.tabs.add(target.tabs.indexOfFirst { it.id == target.activeId.value } + 1, tab)
        activate(tab.id)
        emit(TabEvent.Opened(tab.id, target.id, TabOpenMode.Foreground, null))
        return tab.id
    }

    fun back(tab: TabId) {
        val state = findTab(tab) ?: return
        if (state.nav.backStack.size <= 1) return
        state.forward.add(state.current)
        state.nav.popBackStack()
    }

    fun forward(tab: TabId) {
        val state = findTab(tab) ?: return
        val key = state.forward.removeLastOrNull() ?: return
        state.nav.navBackStack.add(key)
    }

    /** Sends the next navigation from [tab], within [ARM_TIMEOUT_MS], to a new tab instead. */
    fun arm(tab: TabId, mode: TabOpenMode, origin: Rect?) {
        arms[tab] = Arm(mode, origin, now())
    }

    fun move(tab: TabId, to: WindowId, index: Int) {
        val state = findTab(tab) ?: return
        if (state.pinned) return
        val target = window(to) ?: return
        val source = windowOf(tab)

        detach(state, source)
        target.tabs.add(index.coerceIn(0, target.tabs.size), state)
        activate(tab)
        emit(TabEvent.Moved(tab, source.id, target.id))
    }

    fun tearOut(tab: TabId, bounds: WindowBounds): WindowId {
        val state = findTab(tab) ?: error("No tab $tab")
        val source = windowOf(tab)
        if (state.pinned) return source.id

        detach(state, source)
        val window = newWindow(state, bounds)
        emit(TabEvent.Moved(tab, source.id, window.id))
        return window.id
    }

    fun focus(window: WindowId) {
        if (window(window) != null) focusedWindow = window
    }

    fun windowOf(tab: TabId): BrowserWindowState =
        windows.first { window -> window.tabs.any { it.id == tab } }

    private fun allTabs() = windows.flatMap { it.tabs }

    private fun findTab(tab: TabId) = allTabs().firstOrNull { it.id == tab }

    private fun window(id: WindowId) = windows.firstOrNull { it.id == id }

    private fun newTab(key: NavKey) = TabState(
        id = TabId(nextId++),
        nav = Navigation3Actions(startKey = key).also { it.interceptor = this },
        pinned = false,
    )

    private fun newWindow(tab: TabState, bounds: WindowBounds): BrowserWindowState {
        val window = BrowserWindowState(WindowId(nextId++), isMain = false, first = tab, bounds = bounds)
        windows.add(window)
        focusedWindow = window.id
        return window
    }

    /** Switches to [tab], which already shows [key]'s identity. Single-instance tabs also navigate to [key]. */
    private fun reuse(tab: TabState, key: NavKey, activate: Boolean) {
        if (activate) activate(tab.id)
        if (keys.keyOf(key)?.isSingleInstanceFamily == true && tab.current != key) {
            tab.forward.clear()
            tab.nav.navBackStack.add(key)
        }
        emit(TabEvent.Flash(tab.id))
    }

    /** Removes [tab] from [window], activating its right neighbor (else left) and closing an emptied torn-out window. */
    private fun detach(tab: TabState, window: BrowserWindowState) {
        val index = window.tabs.indexOf(tab)
        window.tabs.removeAt(index)
        if (backgroundRun[window.id] == tab.id) backgroundRun.remove(window.id)

        if (window.tabs.isEmpty()) {
            windows.remove(window)
            backgroundRun.remove(window.id)
            if (focusedWindow == window.id) focusedWindow = windows.first { it.isMain }.id
        } else if (window.activeId.value == tab.id) {
            activate(window.tabs[index.coerceAtMost(window.tabs.lastIndex)].id)
        }
    }

    private fun emit(event: TabEvent) {
        _events.tryEmit(event)
    }

    companion object {
        const val ARM_TIMEOUT_MS = 10_000L
        val DefaultWindowBounds = WindowBounds(x = 120.dp, y = 120.dp, width = 1100.dp, height = 760.dp)
    }
}
