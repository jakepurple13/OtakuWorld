package com.programmersbox.manga.shared.reader

import androidx.compose.ui.test.ExperimentalTestApi
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
}
