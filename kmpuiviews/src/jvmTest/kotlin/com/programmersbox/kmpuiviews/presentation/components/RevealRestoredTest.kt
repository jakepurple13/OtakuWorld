package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Mirrors the History / Notifications lists: grouped rows under sticky headers, rows hidden and
 * restored by Undo. LazyList keeps its first visible item by key, so a restored group that lands
 * above it would otherwise come back scrolled off the top.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalFoundationApi::class)
class RevealRestoredTest {

    private val all = listOf(
        "November" to listOf("helmut"),
        "October" to (1..20).map { "row$it" },
    )

    private val hidden = mutableStateOf(emptySet<String>())
    private lateinit var state: LazyListState

    private fun visibleGroups() =
        all.map { (header, rows) -> header to rows.filterNot { it in hidden.value } }.filter { it.second.isNotEmpty() }

    private fun keys(): List<Any> = visibleGroups().flatMap { (header, rows) -> listOf(header) + rows }

    // The viewport is filled by the remaining rows, as in a real history list.
    private fun ComposeUiTest.showList() {
        setContent {
            state = rememberLazyListState()
            LazyColumn(state = state, modifier = Modifier.size(width = 200.dp, height = 600.dp)) {
                visibleGroups().forEach { (header, rows) ->
                    stickyHeader(key = header) { Text(header, Modifier.animateItem().height(40.dp).testTag(header)) }
                    items(rows, key = { it }) { Text(it, Modifier.animateItem().height(72.dp).testTag(it)) }
                }
            }
        }
    }

    // Undo the way the screens do it: restore, then reveal, in the same frame.
    private fun ComposeUiTest.removeThenUndo(row: String) {
        runOnIdle { hidden.value = setOf(row) }
        waitForIdle()
        runOnIdle {
            hidden.value = emptySet()
            state.revealRestored(keys(), setOf(row))
        }
        waitForIdle()
    }

    @Test
    fun `restored top row and its group header come back on screen`() = runComposeUiTest {
        showList()

        removeThenUndo("helmut")

        onNodeWithTag("November").assertIsDisplayed()
        onNodeWithTag("helmut").assertIsDisplayed()
        assertEquals(0, state.firstVisibleItemIndex)
    }

    @Test
    fun `restored row below the top of the viewport does not scroll`() = runComposeUiTest {
        showList()

        removeThenUndo("row5")

        onNodeWithTag("row5").assertIsDisplayed()
        assertEquals(0, state.firstVisibleItemIndex)
    }

    @Test
    fun `restored first visible row shows below the pinned header`() = runComposeUiTest {
        showList()
        runOnIdle { state.requestScrollToItem(keys().indexOf("row2")) }
        waitForIdle()

        removeThenUndo("row2")

        onNodeWithTag("row2").assertIsDisplayed()
        assertEquals(keys().indexOf("row1"), state.firstVisibleItemIndex)
    }
}
