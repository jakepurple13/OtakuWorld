package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.ExceptionItem
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures
import com.programmersbox.kmpuiviews.domain.exceptions.parseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** Regression tests for the final-review findings. */
@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3AdaptiveApi::class)
class ExceptionFixPassTest {

    private val row = ExceptionRow(
        item = ExceptionItem(time = 1_000, message = ExceptionFixtures.mangaDexTimeout, id = 3),
        parsed = parseException(ExceptionFixtures.mangaDexTimeout),
        source = null,
        sourceName = "MangaDex",
        extensionVersion = null,
        seenThisWeek = 1,
    )

    // Important 1: the navigator must use the pane's partition count, not the window's.
    @Test
    fun navigatorUsesPanePartitionsNotWindow() = runComposeUiTest {
        var windowPartitions = -1
        var navigatorPartitions = -1
        setContent {
            Box(Modifier.size(1600.dp, 900.dp)) {
                windowPartitions = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()).maxHorizontalPartitions
                navigatorPartitions = rememberExceptionNavigator(twoPane = false).scaffoldDirective.maxHorizontalPartitions
            }
        }
        waitForIdle()
        println("window partitions = $windowPartitions")
        assertEquals(1, navigatorPartitions)
    }

    // Important 2: a row that finished exiting must not grow back while the list catches up.
    @Test
    fun exitedRowStaysHiddenUntilRemoved() = runComposeUiTest {
        val exiting = mutableStateOf(emptySet<Long>())
        setContent {
            MaterialTheme {
                ExceptionTimeline(
                    days = listOf(ExceptionDay(LocalDate(2026, 11, 27), listOf(row))),
                    hourly = List(24) { 0 }, digest = AiTextState.Idle, digestAvailable = false, explainAvailable = false,
                    twoPane = false, selected = null, onSelect = {}, onMoreInfo = {}, onDelete = {}, onCopy = {},
                    onSummarize = {}, explanation = { MutableStateFlow(AiTextState.Idle) }, onExplain = {},
                    exiting = exiting.value,
                    // The screen drops the id from `exiting` before the VM drops the row.
                    onExited = { exiting.value = exiting.value - it },
                )
            }
        }
        exiting.value = setOf(3L)
        waitForIdle()
        onAllNodesWithText("MangaDex took too long to answer").assertCountEquals(0)
    }

    // Important 3: rows still sliding out when the screen goes away are deleted anyway.
    @Test
    fun exitingRowsCommittedOnDispose() = runComposeUiTest {
        val show = mutableStateOf(true)
        val committed = mutableListOf<Long>()
        setContent {
            if (show.value) CommitExitingOnDispose(exiting = setOf(4L, 5L), commit = { committed += it })
        }
        waitForIdle()
        assertEquals(emptyList(), committed)
        show.value = false
        waitForIdle()
        assertEquals(listOf(4L, 5L), committed.sorted())
    }
}
