package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.LocalPageRefreshHost
import com.programmersbox.kmpuiviews.presentation.components.PageRefresh
import com.programmersbox.kmpuiviews.presentation.components.RegisterPageRefresh
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TabRefreshTest {

    private val controller = TabsController(
        homeNav = Navigation3Actions(Screen.RecentScreen),
        keys = TabKeys(listOf(CoreTabKeyProvider)),
        prefs = TabPrefsSnapshot(clickOpensNewTab = false),
        now = { 0L },
    )
    private val tab get() = controller.windows.first().tabs.first()

    private fun page(refreshing: Boolean = false, stop: (() -> Unit)? = null, onRefresh: () -> Unit = {}) =
        PageRefresh(refreshing = { refreshing }, refresh = onRefresh, stop = stop)

    @Test
    fun registryIsEmptyUntilAPageRegisters() {
        val registry = TabRefreshRegistry()
        assertNull(registry.page)
        assertFalse(registry.canRefresh)
        assertFalse(registry.refreshing)
        assertFalse(registry.canStop)
    }

    @Test
    fun lastRegistrationWinsAndUnregisterRestoresThePrevious() {
        val registry = TabRefreshRegistry()
        val a = page()
        val b = page()
        registry.register(a)
        val unregisterB = registry.register(b)
        assertSame(b, registry.page)
        unregisterB()
        assertSame(a, registry.page)
    }

    @Test
    fun refreshCallsThePage() {
        var calls = 0
        tab.refresh.register(page { calls++ })
        controller.refresh(tab.id)
        assertEquals(1, calls)
    }

    @Test
    fun refreshIsIgnoredWhileRefreshing() {
        var calls = 0
        tab.refresh.register(page(refreshing = true) { calls++ })
        controller.refresh(tab.id)
        assertEquals(0, calls)
    }

    @Test
    fun stopOnlyWhenThePageCanStop() {
        tab.refresh.register(page(refreshing = true, stop = null))
        assertFalse(tab.refresh.canStop)

        var stops = 0
        tab.refresh.register(page(refreshing = true, stop = { stops++ }))
        assertTrue(tab.refresh.canStop)
        controller.stopRefresh(tab.id)
        assertEquals(1, stops)
    }

    @Test
    fun stopIsIgnoredWhenNotRefreshing() {
        var stops = 0
        tab.refresh.register(page(refreshing = false, stop = { stops++ }))
        controller.stopRefresh(tab.id)
        assertEquals(0, stops)
    }

    @Test
    fun registrationLeavesAndReturnsWithThePage() = runComposeUiTest {
        val registry = TabRefreshRegistry()
        val shown = mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalPageRefreshHost provides registry) {
                if (shown.value) RegisterPageRefresh(refreshing = false, onRefresh = {})
            }
        }
        waitForIdle()
        assertTrue(registry.canRefresh)

        shown.value = false
        waitForIdle()
        assertFalse(registry.canRefresh)

        shown.value = true
        waitForIdle()
        assertTrue(registry.canRefresh)
    }

    @Test
    fun registrationReadsTheLatestRefreshingValue() = runComposeUiTest {
        val registry = TabRefreshRegistry()
        val refreshing = mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalPageRefreshHost provides registry) {
                RegisterPageRefresh(refreshing = refreshing.value, onRefresh = {})
            }
        }
        waitForIdle()
        assertFalse(registry.refreshing)
        refreshing.value = true
        waitForIdle()
        assertTrue(registry.refreshing)
    }

    @Test
    fun noHostMeansNoOp() = runComposeUiTest {
        setContent { RegisterPageRefresh(refreshing = false, onRefresh = {}) }
        waitForIdle()
    }
}
