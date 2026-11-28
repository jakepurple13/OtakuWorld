package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
class ExceptionTimelineTest {

    private fun row(id: Long, text: String, sourceName: String?) = ExceptionRow(
        item = ExceptionItem(time = 1_000L * id, message = text, id = id),
        parsed = parseException(text),
        source = null,
        sourceName = sourceName,
        extensionVersion = null,
        seenThisWeek = 1,
    )

    private val days = listOf(
        ExceptionDay(
            LocalDate(2026, 11, 27),
            listOf(
                row(1, ExceptionFixtures.mangaDexTimeout, "MangaDex"),
                row(2, ExceptionFixtures.comickJson, "Comick"),
            )
        )
    )

    @Test
    fun expandShowsMoreInfoWhichReportsTheId() = runComposeUiTest {
        var opened: Long? = null
        setContent {
            MaterialTheme {
                ExceptionTimeline(
                    days = days,
                    hourly = List(23) { 0 } + 2,
                    digest = AiTextState.Idle,
                    digestAvailable = false,
                    explainAvailable = false,
                    twoPane = false,
                    selected = null,
                    onSelect = {},
                    onMoreInfo = { opened = it },
                    onDelete = {},
                    onCopy = {},
                    onSummarize = {},
                    explanation = { MutableStateFlow(AiTextState.Idle) },
                    onExplain = {},
                )
            }
        }
        onNodeWithText("MangaDex took too long to answer").assertIsDisplayed()
        onNodeWithText("Last 24 hours").assertIsDisplayed()
        onNodeWithText("MangaDex took too long to answer").performClick()
        onNodeWithText("More info").assertIsDisplayed().performClick()
        assertEquals(1L, opened)
    }

    @Test
    fun knownErrorShowsFixedTextWhenExpanded() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ExceptionTimeline(
                    days = days, hourly = List(24) { 0 }, digest = AiTextState.Idle, digestAvailable = false,
                    explainAvailable = true, twoPane = false, selected = null, onSelect = {}, onMoreInfo = {},
                    onDelete = {}, onCopy = {}, onSummarize = {}, explanation = { MutableStateFlow(AiTextState.Idle) },
                    onExplain = {},
                )
            }
        }
        onNodeWithText("MangaDex took too long to answer").performClick()
        onNodeWithText("What this means").assertIsDisplayed()
        onAllNodesWithText("Explain this one").assertCountEquals(0)
    }

    @Test
    fun emptyShowsEmptyStateAndNoDigest() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ExceptionTimeline(
                    days = emptyList(), hourly = List(24) { 0 }, digest = AiTextState.Idle, digestAvailable = true,
                    explainAvailable = true, twoPane = false, selected = null, onSelect = {}, onMoreInfo = {},
                    onDelete = {}, onCopy = {}, onSummarize = {}, explanation = { MutableStateFlow(AiTextState.Idle) },
                    onExplain = {},
                )
            }
        }
        onNodeWithText("No exceptions").assertIsDisplayed()
        onAllNodesWithText("Last 24 hours").assertCountEquals(0)
    }

    @Test
    fun twoPaneTapSelectsWithoutExpanding() = runComposeUiTest {
        var selected: Long? = null
        setContent {
            MaterialTheme {
                ExceptionTimeline(
                    days = days, hourly = List(24) { 0 }, digest = AiTextState.Idle, digestAvailable = false,
                    explainAvailable = false, twoPane = true, selected = null, onSelect = { selected = it }, onMoreInfo = {},
                    onDelete = {}, onCopy = {}, onSummarize = {}, explanation = { MutableStateFlow(AiTextState.Idle) },
                    onExplain = {},
                )
            }
        }
        onNodeWithText("Comick sent something the app couldn't read").performClick()
        assertEquals(2L, selected)
        onAllNodesWithText("More info").assertCountEquals(0)
    }
}
