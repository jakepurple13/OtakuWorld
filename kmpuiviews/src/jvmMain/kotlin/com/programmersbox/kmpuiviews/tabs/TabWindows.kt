package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

/** How far below the top strip (dp) a drop still reorders; further is a tear-out. */
const val TopTearOutDistance = 46f

/** How far right of the side column (dp) a drop still reorders; further is a tear-out. */
const val SideTearOutDistance = 50f

/** Where the pointer sits in a torn-out window (dp from its top-left): over its first tab. */
private val TearOutGrab = Offset(80f, 20f)

/** Where a dragged tab would land. */
sealed interface DropTarget {
    /** Back into its own window's strip, at insertion [index] (counting the dragged tab's own slot). */
    data class Reorder(val window: WindowId, val index: Int) : DropTarget

    /** Into another window's strip, at [index]. */
    data class MoveTo(val window: WindowId, val index: Int) : DropTarget

    /** Out into a new window at [bounds]. */
    data class TearOut(val bounds: WindowBounds) : DropTarget
}

/**
 * Where a tab dragged out of [source] lands when dropped at [screenPoint]. Every rect is in screen dp.
 *
 * - Over [source]'s strip: [DropTarget.Reorder]. Over another window's strip: [DropTarget.MoveTo].
 * - Elsewhere, more than [TopTearOutDistance] above or below the top strip (or [SideTearOutDistance] beside the
 *   side column, when [side]): [DropTarget.TearOut], a window the size of [source]'s with the pointer over its tab.
 * - Otherwise: [DropTarget.Reorder] at the nearest slot.
 *
 * [chips] are each window's tab rects in strip order; the index is how many of them lie before the pointer. With
 * [tearOutEnabled] false every drop reorders.
 */
fun dropTarget(
    screenPoint: Offset,
    windows: List<Pair<WindowId, Rect>>,
    strips: Map<WindowId, Rect>,
    source: WindowId,
    side: Boolean = false,
    chips: Map<WindowId, List<Rect>> = emptyMap(),
    tearOutEnabled: Boolean = true,
): DropTarget {
    fun indexIn(window: WindowId) = chips[window].orEmpty().count { chip ->
        if (side) chip.center.y < screenPoint.y else chip.center.x < screenPoint.x
    }

    val reorder = DropTarget.Reorder(source, indexIn(source))
    if (!tearOutEnabled) return reorder

    strips[source]?.let { if (it.contains(screenPoint)) return reorder }
    strips.entries.firstOrNull { (window, strip) -> window != source && strip.contains(screenPoint) }
        ?.let { return DropTarget.MoveTo(it.key, indexIn(it.key)) }

    val strip = strips[source] ?: return reorder
    val away = if (side) {
        maxOf(strip.left - screenPoint.x, screenPoint.x - strip.right)
    } else {
        maxOf(strip.top - screenPoint.y, screenPoint.y - strip.bottom)
    }
    if (away <= (if (side) SideTearOutDistance else TopTearOutDistance)) return reorder

    val size = windows.firstOrNull { it.first == source }?.second?.size
    val defaults = TabsController.DefaultWindowBounds
    return DropTarget.TearOut(
        WindowBounds(
            x = (screenPoint.x - TearOutGrab.x).dp,
            y = (screenPoint.y - TearOutGrab.y).dp,
            width = size?.width?.dp ?: defaults.width,
            height = size?.height?.dp ?: defaults.height,
        )
    )
}

/**
 * Drops [tab] on [target]. Returns the window the tab ends up in, or null when nothing happened ("Drag tabs out
 * into windows" off and [target] needs a window).
 */
fun TabsController.drop(tab: TabId, target: DropTarget): WindowId? {
    val source = windows.firstOrNull { w -> w.tabs.any { it.id == tab } } ?: return null
    return when (target) {
        is DropTarget.Reorder -> if (target.window == source.id) reorder(tab, target.index) else null
        is DropTarget.MoveTo -> when {
            !prefs.tearOut -> null
            target.window == source.id -> reorder(tab, target.index)
            else -> move(tab, target.window, target.index).let { target.window }
        }

        is DropTarget.TearOut -> if (prefs.tearOut) tearOut(tab, target.bounds) else null
    }
}

/** Moves [tab] to insertion [index] in its own window (never before a pinned tab) and activates it. */
private fun TabsController.reorder(tab: TabId, index: Int): WindowId? {
    val window = windowOf(tab)
    val state = window.tabs.first { it.id == tab }
    if (state.pinned) return window.id
    val from = window.tabs.indexOf(state)
    val to = (if (index > from) index - 1 else index).coerceIn(window.tabs.count { it.pinned }, window.tabs.size - 1)
    if (to != from) {
        window.tabs.removeAt(from)
        window.tabs.add(to, state)
    }
    activate(tab)
    return window.id
}

/**
 * Closes a torn-out window and every tab in it; each is recorded in Recently closed, the active one last so ⌘⇧T
 * brings it back first. The main window is never closed this way (its close button quits).
 */
fun TabsController.closeWindow(id: WindowId) {
    val window = windows.firstOrNull { it.id == id }?.takeUnless { it.isMain } ?: return
    val active = window.activeId.value
    window.tabs.sortedBy { it.id == active }.forEach { close(it.id) }
}

/** ⌘N: a new window showing a new tab page, cascaded from the last window. Null when tear-out is off. */
fun TabsController.openNewWindow(): WindowId? {
    if (!prefs.tearOut) return null
    val from = windows.first { it.id == focusedWindow }.activeId.value
    val window = windowOf(open(NewTabRoute, TabOpenMode.NewWindow, from))
    val step = (windows.size - 2) * 30
    window.bounds.value = TabsController.DefaultWindowBounds.let { it.copy(x = it.x + step.dp, y = it.y + step.dp) }
    return window.id
}

/** Every open tab, in every window. Home counts. */
val TabsController.tabCount: Int get() = windows.sumOf { it.tabs.size }

/** Whether quitting should ask first: "Ask before quitting" is on and 3 or more tabs are open. */
fun shouldConfirmQuit(tabCount: Int, enabled: Boolean): Boolean = enabled && tabCount >= 3

/** A tab being dragged out of [from]'s strip. [origin] is its chip in screen dp, where a tear-out grows from. */
class TabDrag(val tab: TabId, val from: WindowId, val origin: Rect) {
    /** The pointer in screen dp. */
    var point: Offset by mutableStateOf(origin.center)

    /** Where a drop now would land. */
    var target: DropTarget by mutableStateOf(DropTarget.Reorder(from, 0))
}

/**
 * Where a window is on screen, so strip and chip rects in its coordinates can be turned into screen dp. [screenOrigin]
 * is its content's top-left and [size] its frame's size, both in screen dp.
 */
class WindowGeometry(val screenOrigin: () -> Offset, val density: () -> Float, val size: () -> Size) {
    /** [rect] in this window's coordinates (px) as screen dp. */
    fun toScreen(rect: Rect): Rect {
        val d = density()
        val o = screenOrigin()
        return Rect(o.x + rect.left / d, o.y + rect.top / d, o.x + rect.right / d, o.y + rect.bottom / d)
    }

    /** [rect] in screen dp as this window's coordinates (px). */
    fun fromScreen(rect: Rect): Rect {
        val d = density()
        val o = screenOrigin()
        return Rect((rect.left - o.x) * d, (rect.top - o.y) * d, (rect.right - o.x) * d, (rect.bottom - o.y) * d)
    }
}

/** A tear-out in flight: the new window grows from [from] (its tab, screen dp) to its bounds. */
class TearOutRun(val from: Rect, val style: TearOutStyle) {
    /** The overlay has finished; the window shows. */
    var shown: Boolean by mutableStateOf(style != TearOutStyle.Overlay)
}

/** Per-window UI state of [TabsController]'s windows. Entries for a closed window are dropped by [forget]. */
class TabWindowsUi {
    /**
     * Per window, true while something inside its content has focus. ⌘← / ⌘→ then wait for the content to refuse
     * them (a focused text field uses them to move the caret) instead of being taken at the window level.
     */
    val contentHasFocus = HashMap<WindowId, Boolean>()

    /** The tab being dragged, if any. */
    var drag: TabDrag? by mutableStateOf(null)

    /** Each window's strip (top strip or side column) in its window's coordinates. */
    val stripBounds = HashMap<WindowId, Rect>()

    /** Each window's on-screen geometry, set by the window. */
    val geometry = HashMap<WindowId, WindowGeometry>()

    /** Tear-outs whose motion is still running, by new window. */
    val tearOuts = mutableStateMapOf<WindowId, TearOutRun>()

    fun forget(window: WindowId) {
        contentHasFocus.remove(window)
        stripBounds.remove(window)
        geometry.remove(window)
        tearOuts.remove(window)
        if (drag?.from == window) drag = null
    }
}
