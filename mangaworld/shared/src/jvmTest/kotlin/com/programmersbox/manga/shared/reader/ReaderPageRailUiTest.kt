package com.programmersbox.manga.shared.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ReaderPageRailUiTest {

    private val labels: (Int) -> Pair<String, String?> = { "Page ${it + 1} / 10" to "Ch. 41" }

    @Test
    fun clickingTheTrackJumpsToThatPage() = runComposeUiTest {
        var jumped = -1
        setContent {
            ReaderPageRail(
                count = 10, position = { 0f }, visible = { 1f }, chapterStarts = emptyList(), scrolling = false,
                label = labels, onScrub = {}, onJump = { jumped = it }, modifier = Modifier.height(500.dp),
            )
        }
        onNodeWithTag(READER_RAIL_TAG).performTouchInput { click(Offset(centerX, height * 0.75f)) }
        assertEquals(7, jumped)
    }

    @Test
    fun hoveringShowsThePageBubble() = runComposeUiTest {
        setContent {
            // At the right edge like in the reader, so the bubble has room on its left
            Box(Modifier.size(400.dp, 500.dp)) {
                ReaderPageRail(
                    count = 10, position = { 4f }, visible = { 1f }, chapterStarts = listOf(6), scrolling = false,
                    label = labels, onScrub = {}, onJump = {}, modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
        onNodeWithText("Page 5 / 10").assertDoesNotExist()
        onNodeWithTag(READER_RAIL_TAG).performMouseInput { moveTo(center) }
        onNodeWithText("Page 5 / 10").assertIsDisplayed()
        onNodeWithText("Ch. 41").assertIsDisplayed()
    }

    @Test
    fun draggingScrubsForward() = runComposeUiTest {
        var position by mutableFloatStateOf(0f)
        setContent {
            ReaderPageRail(
                count = 10, position = { position }, visible = { 1f }, chapterStarts = emptyList(), scrolling = false,
                label = labels, onScrub = { position = it }, onJump = {}, modifier = Modifier.height(500.dp),
            )
        }
        onNodeWithTag(READER_RAIL_TAG).performTouchInput {
            down(Offset(centerX, 5f))
            moveTo(Offset(centerX, height / 2f))
            moveTo(Offset(centerX, height.toFloat()))
            up()
        }
        assertTrue(position > 8f, "position was $position")
    }
}
