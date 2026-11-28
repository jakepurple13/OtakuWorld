package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.motion.OverlayPlatform
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TabWindowsTest {

    private val controller = TabsController(
        homeNav = TabNavigation3Actions(Screen.RecentScreen),
        keys = TabKeys(listOf(CoreTabKeyProvider)),
        prefs = TabPrefsSnapshot(clickOpensNewTab = false),
    )

    private fun title(name: String) = Screen.DetailsScreen.Details(
        title = name,
        description = "d",
        url = "url-$name",
        imageUrl = "img-$name",
        source = "src",
    )

    private val main get() = controller.windows.first { it.isMain }
    private val homeId get() = main.tabs.first().id
    private val bounds = WindowBounds(0.dp, 0.dp, 800.dp, 600.dp)

    // Screen geometry in dp: the main window at (0, 0) with its top strip, a second window to the right.
    private val w1 = WindowId(1)
    private val w2 = WindowId(2)
    private val windows = listOf(w1 to Rect(0f, 0f, 1000f, 700f), w2 to Rect(1200f, 0f, 2000f, 600f))
    private val strips = mapOf(w1 to Rect(80f, 0f, 900f, 40f), w2 to Rect(1280f, 0f, 1900f, 40f))
    private val chips = mapOf(
        w1 to listOf(Rect(80f, 5f, 180f, 35f), Rect(184f, 5f, 284f, 35f), Rect(288f, 5f, 388f, 35f)),
        w2 to listOf(Rect(1280f, 5f, 1380f, 35f)),
    )

    private fun target(x: Float, y: Float, side: Boolean = false, tearOut: Boolean = true) = dropTarget(
        screenPoint = Offset(x, y),
        windows = windows,
        strips = strips,
        source = w1,
        side = side,
        chips = chips,
        tearOutEnabled = tearOut,
    )

    @Test
    fun dropInsideOwnStripReorders() {
        assertEquals(DropTarget.Reorder(w1, 0), target(100f, 20f))
        assertEquals(DropTarget.Reorder(w1, 2), target(250f, 20f))
        assertEquals(DropTarget.Reorder(w1, 3), target(600f, 20f))
    }

    @Test
    fun dropJustBelowStripStillReorders() {
        // 46 dp below the strip's bottom is the limit.
        assertEquals(DropTarget.Reorder(w1, 1), target(150f, 40f + 46f))
    }

    @Test
    fun dropFarBelowStripTearsOut() {
        val drop = target(500f, 40f + 47f)
        assertTrue(drop is DropTarget.TearOut)
        // The new window keeps the source window's size and puts the dragged tab under the pointer.
        assertEquals(1000.dp, drop.bounds.width)
        assertEquals(700.dp, drop.bounds.height)
        assertTrue(drop.bounds.x < 500.dp && drop.bounds.y < 87.dp)
    }

    @Test
    fun dropOverAnotherWindowsStripMoves() {
        assertEquals(DropTarget.MoveTo(w2, 0), target(1300f, 20f))
        assertEquals(DropTarget.MoveTo(w2, 1), target(1500f, 20f))
    }

    @Test
    fun sideLayoutUsesFiftyDpToTheRight() {
        val sideStrips = mapOf(w1 to Rect(0f, 40f, 200f, 700f))
        val sideChips = mapOf(w1 to listOf(Rect(6f, 46f, 194f, 82f), Rect(6f, 84f, 194f, 120f)))
        fun side(x: Float, y: Float) = dropTarget(Offset(x, y), windows, sideStrips, w1, side = true, chips = sideChips)

        assertEquals(DropTarget.Reorder(w1, 1), side(100f, 70f))
        assertEquals(DropTarget.Reorder(w1, 2), side(200f + 50f, 110f))
        assertTrue(side(200f + 51f, 110f) is DropTarget.TearOut)
    }

    @Test
    fun tearOutDisabledOnlyReorders() {
        assertEquals(DropTarget.Reorder(w1, 3), target(500f, 400f, tearOut = false))
        assertEquals(DropTarget.Reorder(w1, 3), target(1300f, 20f, tearOut = false))

        controller.prefs = controller.prefs.copy(tearOut = false)
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        assertNull(controller.drop(a, DropTarget.TearOut(bounds)))
        assertEquals(1, controller.windows.size)
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })

        // ⌘N and "Open in new window" fall back to this window.
        controller.perform(TabShortcut.NewWindow, main)
        assertEquals(1, controller.windows.size)
        controller.open(title("B"), TabOpenMode.NewWindow, homeId)
        assertEquals(1, controller.windows.size)
    }

    @Test
    fun shortcutsListHidesNewWindowWhenTearOutIsOff() {
        assertTrue(tabShortcutCaps(isMac = true).any { it.first == "New window" })
        assertFalse(tabShortcutCaps(isMac = true, tearOut = false).any { it.first == "New window" })
    }

    @Test
    fun newWindowShortcutOpensNewTabPageWithoutHome() {
        controller.perform(TabShortcut.NewWindow, main)

        assertEquals(2, controller.windows.size)
        val second = controller.windows.first { !it.isMain }
        assertEquals(listOf<NavKey>(NewTabRoute), second.tabs.map { it.current })
        assertFalse(second.tabs.any { it.pinned })
        assertEquals(second.id, controller.focusedWindow)
    }

    @Test
    fun closeWindowShortcutIgnoredOnMain() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        controller.perform(TabShortcut.CloseWindow, main)
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })

        val tornId = controller.tearOut(a, bounds)
        val w = controller.windows.first { it.id == tornId }
        controller.perform(TabShortcut.CloseWindow, w)
        assertEquals(listOf(main.id), controller.windows.map { it.id })
    }

    @Test
    fun closingTornWindowRecordsItsTabs() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val b = controller.open(title("B"), TabOpenMode.Foreground, a)
        val c = controller.open(title("C"), TabOpenMode.Foreground, b)
        val torn = controller.tearOut(a, bounds)
        controller.move(b, torn, 1)
        controller.move(c, torn, 2)
        controller.activate(b)

        controller.closeWindow(torn)

        assertEquals(listOf(main.id), controller.windows.map { it.id })
        assertEquals(main.id, controller.focusedWindow)
        // Every tab is recorded; the active one last, so ⌘⇧T brings it back first.
        assertEquals(listOf<NavKey>(title("B"), title("C"), title("A")), controller.recentlyClosed.map { it.current })
    }

    @Test
    fun closeWindowIgnoresMain() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        controller.closeWindow(main.id)
        assertEquals(listOf(homeId, a), main.tabs.map { it.id })
        assertTrue(controller.recentlyClosed.isEmpty())
    }

    @Test
    fun activateFromBackgroundCloseDoesNotStealFocus() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val b = controller.open(title("B"), TabOpenMode.Foreground, a)
        val torn = controller.tearOut(a, bounds)
        controller.move(b, torn, 1)
        controller.focus(main.id)

        // The torn window's active tab closes while the main window has focus: its neighbor becomes active there.
        controller.close(b)

        assertEquals(a, controller.windows.first { it.id == torn }.activeId.value)
        assertEquals(main.id, controller.focusedWindow)

        // A user's activation still focuses its window.
        controller.activate(a)
        assertEquals(torn, controller.focusedWindow)
    }

    @Test
    fun crossWindowActivationRaisesItsWindowButBackgroundCloseDoesNot() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val b = controller.open(title("B"), TabOpenMode.Foreground, a)
        val torn = controller.tearOut(a, bounds)
        controller.move(b, torn, 1)
        controller.activate(homeId)
        controller.focus(main.id)
        val beforeClose = controller.raiseRequest
        assertEquals(main.id, beforeClose?.window)

        // A close in the torn window activates its neighbor there without asking it forward.
        controller.close(b)
        assertTrue(controller.raiseRequest === beforeClose)

        // Going to a tab in the torn window asks its window forward.
        controller.goTo(a)
        assertEquals(torn, controller.focusedWindow)
        val request = controller.raiseRequest
        assertEquals(torn, request?.window)

        // Only the request's window raises, and only while the OS has another window focused.
        assertTrue(shouldRaise(request, torn, osFocused = false))
        assertFalse(shouldRaise(request, torn, osFocused = true))
        assertFalse(shouldRaise(request, main.id, osFocused = false))
        assertFalse(shouldRaise(null, torn, osFocused = false))

        // Activating the same window again is a new request, so it raises again after the user left it.
        controller.activate(a)
        assertFalse(controller.raiseRequest === request)
    }

    @Test
    fun closedWindowForgetsItsUiState() {
        val a = controller.open(title("A"), TabOpenMode.Foreground, homeId)
        val torn = controller.tearOut(a, bounds)
        controller.ui.contentHasFocus[torn] = true
        controller.motion.contentBounds[torn] = Rect(0f, 0f, 10f, 10f)

        controller.close(a)

        assertFalse(torn in controller.ui.contentHasFocus)
        assertFalse(torn in controller.motion.contentBounds)
    }

    @Test
    fun shouldConfirmQuitNeedsThreeTabsAndThePref() {
        assertFalse(shouldConfirmQuit(tabCount = 2, enabled = true))
        assertTrue(shouldConfirmQuit(tabCount = 3, enabled = true))
        assertTrue(shouldConfirmQuit(tabCount = 9, enabled = true))
        assertFalse(shouldConfirmQuit(tabCount = 9, enabled = false))
    }

    @Test
    fun tearOutStyleFollowsMotionAndPlatform() {
        assertEquals(TearOutStyle.Overlay, tearOutStyle(TabMotionLevel.Full, overlay = true))
        assertEquals(TearOutStyle.Lift, tearOutStyle(TabMotionLevel.Full, overlay = false))
        assertEquals(TearOutStyle.None, tearOutStyle(TabMotionLevel.Reduced, overlay = true))
        assertEquals(TearOutStyle.None, tearOutStyle(TabMotionLevel.Off, overlay = false))
    }

    @Test
    fun overlayOnlyOnMacUnlessForcedOff() {
        assertTrue(overlaySupported(OverlayPlatform.MacOS, forced = null))
        assertFalse(overlaySupported(OverlayPlatform.MacOS, forced = "false"))
        assertFalse(overlaySupported(OverlayPlatform.Windows, forced = null))
        assertFalse(overlaySupported(OverlayPlatform.Windows, forced = "true"))
        assertFalse(overlaySupported(OverlayPlatform.LinuxX11, forced = null))
    }
}
