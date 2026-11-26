package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.navactions.NavInterceptor
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import com.programmersbox.kmpuiviews.presentation.tabs.SearchFocusRequest
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
    private val handoffs: List<TabHandoff> = emptyList(),
    /** The app's Downloads page, or null when it has none; the toolbar hides its button then. */
    val downloadsKey: NavKey? = null,
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

    /** Raised by the search shortcut, consumed by the global search screen. */
    val searchFocus = SearchFocusRequest()

    /**
     * True while something inside a window's content has focus. ⌘← / ⌘→ then wait for the content to refuse them
     * (a focused text field uses them to move the caret) instead of being taken at the window level.
     */
    var contentHasFocus = false

    /** Shared state for the open, close, switch and flash motion. */
    val motion = TabMotionState()

    private class Arm(val mode: TabOpenMode, val origin: Rect?, val at: Long)

    private val arms = mutableMapOf<TabId, Arm>()

    /** Per window, the last background tab opened in a row from the active tab. */
    private val backgroundRun = mutableMapOf<WindowId, TabId>()

    /** Per window, its tabs in the order they were last activated, most recent last. For [AfterClose.LastUsed]. */
    private val usedOrder = mutableMapOf<WindowId, MutableList<TabId>>()

    /** Closed tabs, most recent last, at most [TabPrefsSnapshot.closedKept]. Session only. */
    private val closed = mutableStateListOf<ClosedTab>()

    /** Closed tabs, most recent first, for the new tab page. */
    val recentlyClosed: List<ClosedTab> get() = closed.asReversed()

    /** How many windows compose each tab's content right now. */
    private val contentCount = mutableMapOf<TabId, Int>()

    /** Closed tabs whose content is still composed; their stores are cleared on the last [detachContent]. */
    private val closing = mutableMapOf<TabId, TabState>()

    /** Sleeping tabs whose content is still composed; their stores are cleared on the last [detachContent]. */
    private val sleepPending = mutableMapOf<TabId, TabState>()

    /** Per tab, when it was opened or last stopped being its window's active tab. */
    private val lastShown = mutableMapOf<TabId, Long>()

    /** Each returns true for a tab that must stay awake, such as one in the mini player. */
    internal val sleepGuards = mutableListOf<(TabId) -> Boolean>()

    /** Per tab, what [handoffs] held for it, saved when it was opened or last left. */
    private val snapshots = mutableMapOf<TabId, List<Any?>>()

    /** The tab whose state [handoffs] hold now. */
    private var handoffOwner: TabId? = windows.first().activeId.value

    private val listeners = mutableListOf<(TabEvent?) -> Unit>()

    init {
        homeNav.interceptor = this
        windows.first().let { touch(it.id, it.activeId.value) }
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
        // An untouched new tab page gives way: it becomes the page, or closes when the page is already open.
        val replacesNewTab = fromTab.isUntouchedNewTab
        if (tk != null) {
            val existing = allTabs().firstOrNull { it !== fromTab && keys.keyOf(it.current) == tk }
            if (existing != null) {
                reuse(existing, key, activate = true)
                if (replacesNewTab) {
                    discard(fromTab)
                    // Closing it activated a neighbor in its own window; the reused tab keeps focus.
                    activate(existing.id)
                }
                return true
            }
            if (tk.isSingleInstanceFamily && keys.keyOf(fromTab.current) != tk && !replacesNewTab) {
                open(key, TabOpenMode.Foreground, fromTab.id)
                return true
            }
        }

        if (prefs.clickOpensNewTab && tk is TabKey.Title && !replacesNewTab) {
            open(key, TabOpenMode.Foreground, fromTab.id)
            return true
        }

        // The click that navigates in place just wrote this tab's handoffs; keep them.
        if (handoffs.isNotEmpty()) handoffOwner = fromTab.id
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
        // The click that opened it just wrote its chapter into the handoff holders.
        if (handoffs.isNotEmpty()) snapshots[tab.id] = captureHandoffs()
        if (mode == TabOpenMode.NewWindow) {
            val window = newWindow(tab, DefaultWindowBounds)
            emit(TabEvent.Opened(tab.id, window.id, mode, origin))
            return tab.id
        }

        val window = findTab(from)?.let { windowOf(it.id) } ?: window(focusedWindow)!!
        val atEnd = prefs.newPlace == NewTabPlace.AtEnd
        if (atEnd) {
            window.tabs.add(tab)
        } else {
            val after = backgroundRun[window.id]?.takeIf { id -> window.tabs.any { it.id == id } }
                ?: window.activeId.value
            window.tabs.add(window.tabs.indexOfFirst { it.id == after } + 1, tab)
        }

        if (mode == TabOpenMode.Foreground) {
            activate(tab.id)
            backgroundRun.remove(window.id)
        } else if (!atEnd) {
            backgroundRun[window.id] = tab.id
        }
        emit(TabEvent.Opened(tab.id, window.id, mode, origin))
        return tab.id
    }

    fun activate(tab: TabId) {
        val window = findTab(tab)?.let { windowOf(it.id) } ?: return
        switchHandoffs(tab)
        wake(window.tabs.first { it.id == tab })
        if (window.activeId.value != tab) {
            lastShown[window.activeId.value] = now()
            window.activeId.value = tab
            backgroundRun.remove(window.id)
        }
        touch(window.id, tab)
        focusedWindow = window.id
        listeners.forEach { it(null) }
    }

    /**
     * Called synchronously after every [activate] (with null) and every event. Mid-operation (a move's detach), a
     * tab may briefly be in no window; the event that ends the operation follows.
     */
    fun addListener(listener: (TabEvent?) -> Unit) {
        listeners += listener
    }

    fun close(tab: TabId) {
        val state = findTab(tab) ?: return
        if (state.pinned) return
        remove(state, record = true)
    }

    /** Closes an untouched new tab page that gave way to another tab. It is not worth reopening, so it is not recorded. */
    private fun discard(state: TabState) = remove(state, record = false)

    private fun remove(state: TabState, record: Boolean) {
        val tab = state.id
        val window = windowOf(tab)

        // Its chapter, so a reopened reader reads it again.
        val held = if (handoffs.isEmpty()) null else if (handoffOwner == tab) captureHandoffs() else snapshots[tab]
        detach(state, window)
        arms.remove(tab)
        snapshots.remove(tab)
        lastShown.remove(tab)
        sleepPending.remove(tab)
        if (handoffOwner == tab) handoffOwner = null
        if (record) {
            closed.add(ClosedTab(state.nav.backStack.toList(), held))
            while (closed.size > prefs.closedKept) closed.removeFirst()
        }
        // Content still on screen (a crossfade frame) keeps its store until it leaves composition.
        if ((contentCount[tab] ?: 0) > 0) closing[tab] = state else clearStore(state)
        emit(TabEvent.Closed(tab, window.id))
    }

    /** A window's content for [tab] entered composition. Pair with [detachContent]. */
    fun attachContent(tab: TabId) {
        contentCount[tab] = (contentCount[tab] ?: 0) + 1
    }

    /** That content left composition; a closed tab's ViewModels are cleared once none is left. */
    fun detachContent(tab: TabId) {
        val left = (contentCount[tab] ?: 1) - 1
        if (left > 0) {
            contentCount[tab] = left
            return
        }
        contentCount.remove(tab)
        closing.remove(tab)?.let(::clearStore)
        sleepPending.remove(tab)?.let(::dropViewModels)
    }

    /** When [tab] was opened or last left, or null for a tab this controller does not know. */
    internal fun lastShownAt(tab: TabId): Long? = lastShown[tab]

    /**
     * Puts [state], a parked tab of [window], to sleep: its ViewModels are cleared (once no content shows it) and
     * [window] drops its saved UI state on [TabEvent.Slept]. Its back stack and handoff snapshot stay.
     */
    internal fun sleep(state: TabState, window: BrowserWindowState) {
        state.sleeping.value = true
        if ((contentCount[state.id] ?: 0) > 0) sleepPending[state.id] = state else dropViewModels(state)
        emit(TabEvent.Slept(state.id, window.id))
    }

    /**
     * Reopens [entry] (default: the most recently closed tab) next to [window]'s active tab. When [replacing] is an
     * untouched new tab page (the one the entry was picked from), the reopened tab takes its place and it closes.
     */
    fun reopenClosed(window: WindowId, entry: ClosedTab? = null, replacing: TabId? = null): TabId? {
        val target = window(window) ?: return null
        val newTabPage = replacing?.let(::findTab)?.takeIf { it.isUntouchedNewTab }
        val closedTab = if (entry != null) entry.takeIf { closed.remove(it) } else closed.removeLastOrNull()
        if (closedTab == null) return null
        val stack = closedTab.stack
        val tk = keys.keyOf(stack.last())
        val existing = tk?.let { allTabs().firstOrNull { tab -> keys.keyOf(tab.current) == tk } }
        if (existing != null) {
            activate(existing.id)
            emit(TabEvent.Flash(existing.id))
            newTabPage?.let {
                discard(it)
                activate(existing.id)
            }
            return existing.id
        }

        val tab = newTab(stack.first())
        stack.drop(1).forEach { tab.nav.navBackStack.add(it) }
        closedTab.handoffs?.let { snapshots[tab.id] = it }

        val newTabIndex = newTabPage?.let { page -> target.tabs.indexOfFirst { it.id == page.id } } ?: -1
        val index = if (newTabIndex >= 0) newTabIndex else target.tabs.indexOfFirst { it.id == target.activeId.value } + 1
        target.tabs.add(index, tab)
        activate(tab.id)
        emit(TabEvent.Opened(tab.id, target.id, TabOpenMode.Foreground, null))
        newTabPage?.let(::discard)
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

    /** Drops [tab]'s pending arm, if any. */
    fun disarm(tab: TabId) {
        arms.remove(tab)
    }

    /** Sends the next navigation from [tab], within [ARM_TIMEOUT_MS], to a new tab instead. */
    fun arm(tab: TabId, mode: TabOpenMode, origin: Rect?) {
        arms[tab] = Arm(mode, origin, now())
    }

    /**
     * [arm] for a ⌘/Ctrl or middle click. With [ModClick.Foreground], ⌘-click (and middle-click, which follows it)
     * opens in the foreground and ⌘⇧-click in the background.
     */
    fun armClick(tab: TabId, mode: TabOpenMode, origin: Rect?) {
        val swapped = when {
            prefs.modClick == ModClick.Background -> mode
            mode == TabOpenMode.Background -> TabOpenMode.Foreground
            mode == TabOpenMode.Foreground -> TabOpenMode.Background
            else -> mode
        }
        arm(tab, swapped, origin)
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

    /** A tab for a restored session showing [stack]. Nothing composes or loads until it is first shown. */
    internal fun restoredTab(stack: List<NavKey>): TabState = newTab(stack.first()).also { tab ->
        stack.drop(1).forEach { tab.nav.navBackStack.add(it) }
    }

    /** A restored torn-out window holding [first]. */
    internal fun restoredWindow(first: TabState, bounds: WindowBounds): BrowserWindowState = newWindow(first, bounds)

    /** Readers null the handoff holders when cleared; the tab that owns them keeps its state. */
    private fun clearStore(state: TabState) {
        val held = captureHandoffs()
        state.viewModelStoreOwner.viewModelStore.clear()
        restoreHandoffs(held)
    }

    /** Clears [state]'s ViewModels and gives it a new page store provider bound to its (now empty) store. */
    private fun dropViewModels(state: TabState) {
        clearStore(state)
        state.viewModelStoreProvider = ViewModelStoreProvider(parentOwner = state.viewModelStoreOwner)
    }

    /** [state] is being shown: it is awake, and a clear still waiting for its old content is dropped. */
    private fun wake(state: TabState) {
        state.sleeping.value = false
        sleepPending.remove(state.id)
    }

    private fun allTabs() = windows.flatMap { it.tabs }

    private fun findTab(tab: TabId) = allTabs().firstOrNull { it.id == tab }

    private fun window(id: WindowId) = windows.firstOrNull { it.id == id }

    private fun newTab(key: NavKey) = TabState(
        id = TabId(nextId++),
        nav = Navigation3Actions(startKey = key).also { it.interceptor = this },
        pinned = false,
    ).also { lastShown[it.id] = now() }

    private fun newWindow(tab: TabState, bounds: WindowBounds): BrowserWindowState {
        val window = BrowserWindowState(WindowId(nextId++), isMain = false, first = tab, bounds = bounds)
        windows.add(window)
        wake(tab)
        touch(window.id, tab.id)
        focusedWindow = window.id
        return window
    }

    /** Records [tab] as [window]'s most recently activated tab. */
    private fun touch(window: WindowId, tab: TabId) {
        val order = usedOrder.getOrPut(window) { mutableListOf() }
        order.remove(tab)
        order.add(tab)
    }

    /** The tab to activate after the active tab at [index] left [window] (already removed from its tabs). */
    private fun nextActive(window: BrowserWindowState, index: Int): TabState {
        val tabs = window.tabs
        val right = tabs.getOrNull(index)
        val leftOf = tabs.getOrNull(index - 1)
        return when (prefs.afterClose) {
            AfterClose.Right -> right ?: leftOf!!
            AfterClose.Left -> leftOf ?: right!!
            AfterClose.LastUsed -> usedOrder[window.id].orEmpty()
                .asReversed()
                .firstNotNullOfOrNull { id -> tabs.firstOrNull { it.id == id } }
                ?: right ?: leftOf!!
        }
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

    /**
     * Removes [tab] from [window], activating the neighbor [TabPrefsSnapshot.afterClose] picks and closing an
     * emptied torn-out window.
     */
    private fun detach(tab: TabState, window: BrowserWindowState) {
        val index = window.tabs.indexOf(tab)
        window.tabs.removeAt(index)
        usedOrder[window.id]?.remove(tab.id)
        if (backgroundRun[window.id] == tab.id) backgroundRun.remove(window.id)

        if (window.tabs.isEmpty()) {
            windows.remove(window)
            backgroundRun.remove(window.id)
            usedOrder.remove(window.id)
            if (focusedWindow == window.id) focusedWindow = windows.first { it.isMain }.id
        } else if (window.activeId.value == tab.id) {
            activate(nextActive(window, index).id)
        }
    }

    /** Saves the holders into the tab that owns them, then restores [to]'s snapshot before it composes. */
    private fun switchHandoffs(to: TabId) {
        if (handoffs.isEmpty() || handoffOwner == to) return
        handoffOwner?.let { snapshots[it] = captureHandoffs() }
        snapshots[to]?.let(::restoreHandoffs)
        handoffOwner = to
    }

    private fun captureHandoffs(): List<Any?> = handoffs.map { it.capture() }

    private fun restoreHandoffs(state: List<Any?>) {
        handoffs.forEachIndexed { i, handoff -> handoff.restore(state[i]) }
    }

    private fun emit(event: TabEvent) {
        motion.record(event)
        listeners.forEach { it(event) }
        _events.tryEmit(event)
    }

    companion object {
        const val ARM_TIMEOUT_MS = 10_000L
        val DefaultWindowBounds = WindowBounds(x = 120.dp, y = 120.dp, width = 1100.dp, height = 760.dp)
    }
}
