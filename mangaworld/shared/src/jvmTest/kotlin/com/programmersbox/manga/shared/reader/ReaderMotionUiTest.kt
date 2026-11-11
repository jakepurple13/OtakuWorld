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
}
