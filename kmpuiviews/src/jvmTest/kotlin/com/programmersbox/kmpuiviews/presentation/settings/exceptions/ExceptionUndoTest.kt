package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.favoritesdatabase.ExceptionItem
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures
import com.programmersbox.kmpuiviews.domain.exceptions.parseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ExceptionUndoTest {
    private val row = ExceptionRow(
        item = ExceptionItem(time = 1_000, message = ExceptionFixtures.mangaDexTimeout, id = 3),
        parsed = parseException(ExceptionFixtures.mangaDexTimeout),
        source = null,
        sourceName = "MangaDex",
        extensionVersion = null,
        seenThisWeek = 1,
    )

    @Test
    fun undoActionCallsUndo() = runComposeUiTest {
        val host = SnackbarHostState()
        var result = ""
        setContent {
            MaterialTheme {
                SnackbarHost(host)
                LaunchedEffect(Unit) { showUndo(host, message = "Exception deleted", onUndo = { result = "undo" }, onCommit = { result = "commit" }) }
            }
        }
        onNodeWithText("Undo").performClick()
        waitForIdle()
        assertEquals("undo", result)
    }

    @Test
    fun deletedRowSlidesOutThenReportsExit() = runComposeUiTest {
        val exiting = mutableStateOf(emptySet<Long>())
        var exited: Long? = null
        setContent {
            MaterialTheme {
                ExceptionTimeline(
                    days = listOf(ExceptionDay(LocalDate(2026, 11, 27), listOf(row))),
                    hourly = List(24) { 0 }, digest = AiTextState.Idle, digestAvailable = false, explainAvailable = false,
                    twoPane = false, selected = null, onSelect = {}, onMoreInfo = {}, onDelete = {}, onCopy = {},
                    onSummarize = {}, explanation = { MutableStateFlow(AiTextState.Idle) }, onExplain = {},
                    exiting = exiting.value,
                    onExited = { exited = it },
                )
            }
        }
        onNodeWithText("MangaDex took too long to answer").assertIsDisplayed()
        exiting.value = setOf(3L)
        waitForIdle()
        assertEquals(3L, exited)
        onAllNodesWithText("MangaDex took too long to answer").assertCountEquals(0)
    }
}
