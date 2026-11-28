package com.programmersbox.kmpuiviews.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TabHistoryMenuTest {
    private val r = Screen.RecentScreen
    private val a = Screen.HistoryScreen
    private val b = Screen.Settings
    private val c = Screen.CustomListScreen

    @Test
    fun historyRowsNewestFirstWithSections() {
        val rows = historyRows(listOf(r, a, b, c), index = 1)
        assertEquals(listOf(3, 2, 1, 0), rows.map { it.index })
        assertEquals(
            listOf(HistorySection.Forward, HistorySection.Forward, HistorySection.Now, HistorySection.Back),
            rows.map { it.section },
        )
        assertEquals(listOf(2, 1, 0, -1), rows.map { it.delta })
        assertEquals(listOf(c, b, a, r), rows.map { it.key })
    }

    @Test
    fun holdOpensMenuAndShortClickSteps() = runComposeUiTest {
        mainClock.autoAdvance = false
        val clicks = mutableIntStateOf(0)
        val opens = mutableIntStateOf(0)
        setContent {
            HistoryButton(
                icon = Icons.AutoMirrored.Default.ArrowBack,
                description = "Back",
                enabled = true,
                onClick = { clicks.value++ },
                onOpenMenu = { opens.value++ },
                modifier = Modifier.testTag("button"),
            )
        }

        onNodeWithTag("button").performMouseInput { press() }
        mainClock.advanceTimeBy(200)
        onNodeWithTag("button").performMouseInput { release() }
        mainClock.advanceTimeByFrame()
        assertEquals(1, clicks.value, "a short press steps")
        assertEquals(0, opens.value, "a short press does not open the menu")

        onNodeWithTag("button").performMouseInput { press() }
        mainClock.advanceTimeBy(500)
        assertEquals(1, opens.value, "holding past $HISTORY_HOLD_MS ms opens the menu")
        onNodeWithTag("button").performMouseInput { release() }
        mainClock.advanceTimeByFrame()
        assertEquals(1, clicks.value, "the release after a hold does not step")
    }

    @Test
    fun rightClickOpensMenu() = runComposeUiTest {
        val clicks = mutableIntStateOf(0)
        val opens = mutableIntStateOf(0)
        setContent {
            HistoryButton(
                icon = Icons.AutoMirrored.Default.ArrowBack,
                description = "Back",
                enabled = true,
                onClick = { clicks.value++ },
                onOpenMenu = { opens.value++ },
                modifier = Modifier.testTag("button"),
            )
        }

        onNodeWithTag("button").performMouseInput {
            press(MouseButton.Secondary)
            release(MouseButton.Secondary)
        }
        waitForIdle()

        assertEquals(1, opens.value)
        assertEquals(0, clicks.value)
    }

    @Test
    fun clickingRowJumps() = runComposeUiTest {
        val nav = TabNavigation3Actions(r)
        nav.history.restore(listOf(r, a, b), 2)
        val tab = TabState(TabId(1), nav, pinned = false)
        val keys = TabKeys(emptyList())
        val jumps = mutableStateListOf<Int>()
        setContent {
            TabHistoryMenu(
                tab = tab,
                expanded = true,
                onDismiss = {},
                onJump = { jumps += it },
                hotIndex = null,
                keys = keys,
            )
        }

        onNodeWithText(keys.labelOf(r).text).performClick()
        waitForIdle()

        assertEquals(listOf(0), jumps.toList())
    }
}
