package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.LocalPageRefreshHost
import com.programmersbox.kmpuiviews.presentation.components.PageRefreshHost
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class DetailRefreshMenuTest {

    private val calls = mutableListOf<String>()

    @Test
    fun showsRefreshAndTapDismissesThenRefreshes() = runComposeUiTest {
        setContent {
            Column {
                RefreshMenuItem(isRefreshing = false, onRefresh = { calls += "refresh" }, dismiss = { calls += "dismiss" })
            }
        }
        onNodeWithText("Refresh").assertIsEnabled().performClick()
        assertEquals(listOf("dismiss", "refresh"), calls)
    }

    @Test
    fun refreshingRowIsDisabled() = runComposeUiTest {
        setContent {
            Column {
                RefreshMenuItem(isRefreshing = true, onRefresh = { calls += "refresh" }, dismiss = { calls += "dismiss" })
            }
        }
        onNodeWithText("Refreshing…").assertIsNotEnabled().performClick()
        assertEquals(emptyList(), calls)
    }

    @Test
    fun hiddenInTheDesktopTabShell() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalPageRefreshHost provides PageRefreshHost { {} }) {
                Column {
                    RefreshMenuItem(isRefreshing = false, onRefresh = {}, dismiss = {})
                }
            }
        }
        onNodeWithText("Refresh").assertDoesNotExist()
    }

    @Test
    fun highlightOnlyFlashesWhenTheValueChanges() = runComposeUiTest {
        val key = mutableIntStateOf(1)
        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.background(Color.White)) {
                Box(Modifier.size(20.dp).highlightOnChange(key.intValue).testTag("field"))
            }
        }
        mainClock.advanceTimeBy(100)
        fun center() = onNodeWithTag("field").captureToImage().toPixelMap().let { it[it.width / 2, it.height / 2] }
        assertEquals(Color.White, center())

        key.intValue = 2
        mainClock.advanceTimeBy(64)
        assertNotEquals(Color.White, center())

        mainClock.advanceTimeBy(1_200)
        assertEquals(Color.White, center())
    }

    @Test
    fun errorRetryRefreshes() = runComposeUiTest {
        setContent { DetailErrorRetry(isRefreshing = false, onRetry = { calls += "retry" }) }
        onNodeWithText("Retry").assertIsEnabled().performClick()
        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun errorRetryIsDisabledWhileRetrying() = runComposeUiTest {
        setContent { DetailErrorRetry(isRefreshing = true, onRetry = { calls += "retry" }) }
        onNodeWithText("Retrying…").assertIsNotEnabled()
    }

    @Test
    fun errorRetryIsHiddenInTheDesktopTabShell() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalPageRefreshHost provides PageRefreshHost { {} }) {
                DetailErrorRetry(isRefreshing = false, onRetry = {})
            }
        }
        onNodeWithText("Retry").assertDoesNotExist()
    }
}
