package com.programmersbox.manga.shared.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ReaderMotionUiTest {

    @Test
    fun failedPageShowsCopyAndRetries() = runComposeUiTest {
        var retries = 0
        setContent { PageLoadFailed(message = "timeout", onRetry = { retries++ }) }
        onNodeWithText("Page didn't load").assertIsDisplayed()
        onNodeWithText("timeout").assertIsDisplayed()
        onNodeWithText("Tap to retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun endCardShowsNextAndPosition() = runComposeUiTest {
        setContent { ChapterTransitionItem("Ch. 42", "Ch. 43", toChapterListIndex = 26, chapterCount = 69) }
        mainClock.advanceTimeBy(2_000)
        onNodeWithText("Finished").assertIsDisplayed()
        onNodeWithText("Ch. 43").assertIsDisplayed()
        onNodeWithText("Chapter 43 of 69").assertIsDisplayed()
    }

    @Test
    fun bannerShowsThenHides() = runComposeUiTest {
        val state = ChapterBannerState()
        setContent { ChapterBanner(state, pageCount = 24) }
        runOnIdle { state.show("Ch. 43") }
        onNodeWithText("Ch. 43").assertIsDisplayed()
        onNodeWithText(" · 24 pages", substring = true).assertIsDisplayed()
        mainClock.advanceTimeBy(CHAPTER_BANNER_MILLIS + 1_000)
        onNodeWithText("Ch. 43").assertDoesNotExist()
    }

    @Test
    fun burstOnlyOnUserAdd() = runComposeUiTest {
        mainClock.autoAdvance = false
        var bookmarked by mutableStateOf(false)
        setContent { BookmarkToggle(bookmarked, onBookmarkClick = { bookmarked = it }) }
        bookmarked = true // the bookmark flow catching up when the chapter opens
        mainClock.advanceTimeByFrame()
        onNodeWithTag("bookmarkBurst").assertDoesNotExist()
        bookmarked = false
        mainClock.advanceTimeBy(1_000)
        onNodeWithContentDescription("Bookmark chapter").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithTag("bookmarkBurst").assertExists()
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag("bookmarkBurst").assertDoesNotExist()
    }

    @Test
    fun endCardWaitsUntilVisible() = runComposeUiTest {
        var visible by mutableStateOf(false)
        setContent {
            ChapterTransitionItem("Ch. 42", "Ch. 43", toChapterListIndex = 26, chapterCount = 69, visible = visible)
        }
        mainClock.advanceTimeBy(2_000)
        onNodeWithText("Finished").assertDoesNotExist()
        visible = true
        mainClock.advanceTimeBy(2_000)
        onNodeWithText("Finished").assertIsDisplayed()
        onNodeWithText("Chapter 43 of 69").assertIsDisplayed()
    }
}
