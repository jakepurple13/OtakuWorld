package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.tabs.LocalTabActions
import com.programmersbox.kmpuiviews.presentation.tabs.tabTarget
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.SharedItemKey
import net.thauvin.erik.urlencoder.UrlEncoderUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DesktopTabActionsTest {

    private fun mode(
        isMac: Boolean,
        meta: Boolean = false,
        ctrl: Boolean = false,
        shift: Boolean = false,
        button: PointerButton = PointerButton.Primary,
    ) = modeFor(isMac = isMac, meta = meta, ctrl = ctrl, shift = shift, button = button)

    @Test
    fun commandClickOnMacOpensInBackground() {
        assertEquals(TabOpenMode.Background, mode(isMac = true, meta = true))
    }

    @Test
    fun ctrlClickOnMacIsPlain() {
        assertNull(mode(isMac = true, ctrl = true))
    }

    @Test
    fun ctrlClickOnWindowsOpensInBackground() {
        assertEquals(TabOpenMode.Background, mode(isMac = false, ctrl = true))
    }

    @Test
    fun commandShiftClickOpensInForeground() {
        assertEquals(TabOpenMode.Foreground, mode(isMac = true, meta = true, shift = true))
        assertEquals(TabOpenMode.Foreground, mode(isMac = false, ctrl = true, shift = true))
    }

    @Test
    fun middleClickOpensInBackground() {
        assertEquals(TabOpenMode.Background, mode(isMac = true, button = PointerButton.Tertiary))
        assertEquals(TabOpenMode.Background, mode(isMac = false, button = PointerButton.Tertiary))
    }

    @Test
    fun plainClickIsPlain() {
        assertNull(mode(isMac = true))
        assertNull(mode(isMac = false))
        assertNull(mode(isMac = true, shift = true))
        assertNull(mode(isMac = false, meta = true))
    }

    @Test
    fun rightClickIsNotAMode() {
        assertNull(mode(isMac = true, meta = true, button = PointerButton.Secondary))
    }

    @Test
    fun windowClickArmsOnlyForModifierPrimaryClicks() {
        assertEquals(TabOpenMode.Background, windowClickMode(true, meta = true, ctrl = false, shift = false, button = PointerButton.Primary))
        assertEquals(TabOpenMode.Foreground, windowClickMode(false, meta = false, ctrl = true, shift = true, button = PointerButton.Primary))
        assertNull(windowClickMode(true, meta = false, ctrl = false, shift = false, button = PointerButton.Primary))
        // Middle and right clicks need the element's own target: clickables ignore them.
        assertNull(windowClickMode(true, meta = false, ctrl = false, shift = false, button = PointerButton.Tertiary))
        assertNull(windowClickMode(true, meta = true, ctrl = false, shift = false, button = PointerButton.Secondary))
    }

    private fun click(
        consumed: Boolean = false,
        inside: Boolean = true,
        movedPastSlop: Boolean = false,
        heldMillis: Long = 100,
    ) = isTabClick(consumed = consumed, inside = inside, movedPastSlop = movedPastSlop, heldMillis = heldMillis, longPressMillis = 500)

    @Test
    fun quickReleaseInsideIsAClick() {
        assertTrue(click())
    }

    @Test
    fun longPressIsNotAClick() {
        assertFalse(click(heldMillis = 500))
        assertFalse(click(heldMillis = 800))
    }

    @Test
    fun releaseOutsideIsNotAClick() {
        assertFalse(click(inside = false))
    }

    @Test
    fun dragOrSwipeIsNotAClick() {
        assertFalse(click(movedPastSlop = true))
    }

    @Test
    fun consumedReleaseIsNotAClick() {
        assertFalse(click(consumed = true))
    }

    private val controller = TabsController(
        homeNav = TabNavigation3Actions(Screen.RecentScreen),
        keys = TabKeys(listOf(CoreTabKeyProvider)),
        prefs = TabPrefsSnapshot(clickOpensNewTab = false),
    )
    private val main get() = controller.windows.first()
    private val homeId get() = main.tabs.first().id

    private fun details(url: String, source: String) = Screen.DetailsScreen.Details(
        title = "Title",
        description = "d",
        url = UrlEncoderUtil.encode(url),
        imageUrl = "img",
        source = source,
    )

    /** Right-clicks an item target for raw url "u" from source "s" in the Home tab. */
    private fun ComposeUiTest.rightClickItem() {
        val actions = DesktopTabActions(controller, homeId)
        setContent {
            CompositionLocalProvider(LocalTabActions provides actions) {
                Box(Modifier.size(100.dp).testTag("item").tabTarget({}, item = SharedItemKey("u", "s")))
            }
            actions.Menu()
        }
        onNodeWithTag("item").performMouseInput {
            press(MouseButton.Secondary)
            release(MouseButton.Secondary)
        }
        waitForIdle()
    }

    @Test
    fun goToHiddenWhenTitleNotOpenElsewhere() = runComposeUiTest {
        controller.open(details("other", "s"), TabOpenMode.Background, homeId)
        rightClickItem()
        onNodeWithText("Open in new tab").assertIsDisplayed()
        onNodeWithText("Go to open tab").assertDoesNotExist()
    }

    @Test
    fun singleMatchGoesStraightThere() = runComposeUiTest {
        val match = controller.open(details("u", "s"), TabOpenMode.Background, homeId)
        val events = mutableListOf<TabEvent>()
        controller.addListener { it?.let(events::add) }
        rightClickItem()

        onNodeWithText("This window").assertIsDisplayed()
        onNodeWithText("Go to open tab").performClick()
        waitForIdle()

        assertEquals(match, main.activeId.value)
        assertEquals(TabEvent.Flash(match), events.last())
        onNodeWithText("Go to open tab").assertDoesNotExist()
    }

    @Test
    fun multipleMatchesShowSubmenu() = runComposeUiTest {
        val torn = controller.open(details("u", "s"), TabOpenMode.NewWindow, homeId)
        controller.open(details("u", "s"), TabOpenMode.Background, homeId)
        controller.focus(main.id)
        rightClickItem()

        onNodeWithText("This window").assertDoesNotExist()
        onNodeWithText("Go to open tab").performClick()
        waitForIdle()

        val here = onNodeWithText("This window").assertIsDisplayed().getBoundsInRoot()
        val there = onNodeWithText("Window 2").assertIsDisplayed().getBoundsInRoot()
        assertTrue(here.top < there.top, "this window's tab comes first")

        onNodeWithText("Window 2").performClick()
        waitForIdle()

        assertEquals(controller.windowOf(torn).id, controller.focusedWindow)
        assertEquals(torn, controller.windowOf(torn).activeId.value)
    }
}
