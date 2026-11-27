package com.programmersbox.kmpuiviews.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.kmpuiviews.presentation.components.PageRefresh
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RefreshButtonTest {

    private var refreshes = 0
    private var stops = 0

    private fun registry(page: PageRefresh?) = TabRefreshRegistry().also { r -> page?.let { r.register(it) } }

    private fun page(refreshing: Boolean, stop: (() -> Unit)?) =
        PageRefresh(refreshing = { refreshing }, refresh = {}, stop = stop)

    @Test
    fun disabledWhenNothingRegistered() = runComposeUiTest {
        setContent { RefreshButton(registry(null), reduceMotion = true, onRefresh = { refreshes++ }, onStop = { stops++ }) }
        onNodeWithContentDescription("Refresh").assertIsNotEnabled()
    }

    @Test
    fun clickRefreshes() = runComposeUiTest {
        setContent {
            RefreshButton(registry(page(refreshing = false, stop = null)), reduceMotion = true, onRefresh = { refreshes++ }, onStop = { stops++ })
        }
        onNodeWithContentDescription("Refresh").assertIsEnabled().performClick()
        assertEquals(1, refreshes)
    }

    @Test
    fun loadingWithStopShowsStop() = runComposeUiTest {
        setContent {
            RefreshButton(registry(page(refreshing = true, stop = {})), reduceMotion = true, onRefresh = { refreshes++ }, onStop = { stops++ })
        }
        onNodeWithContentDescription("Stop loading").assertIsEnabled().performClick()
        assertEquals(1, stops)
        assertEquals(0, refreshes)
    }

    @Test
    fun loadingWithoutStopStaysRefreshAndDisabled() = runComposeUiTest {
        setContent {
            RefreshButton(registry(page(refreshing = true, stop = null)), reduceMotion = true, onRefresh = { refreshes++ }, onStop = { stops++ })
        }
        onNodeWithContentDescription("Refresh").assertIsNotEnabled()
        onNodeWithContentDescription("Stop loading").assertDoesNotExist()
    }
}
