package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A parked tab leaves composition. Its page ViewModels must survive that (no reload when switching
 * back) and be cleared only when the tab closes.
 */
@OptIn(ExperimentalTestApi::class)
class TabViewModelStoreTest {

    class PageViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    private val shown = mutableStateOf(true)
    private var vm: PageViewModel? = null

    @Composable
    private fun Page(owner: ViewModelStoreOwner, provider: ViewModelStoreProvider?) {
        if (!shown.value) return
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
            NavDisplay(
                backStack = listOf<NavKey>(Screen.Settings),
                onBack = {},
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    if (provider != null) rememberViewModelStoreNavEntryDecorator(provider)
                    else rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = { key -> NavEntry(key) { vm = viewModel { PageViewModel() } } },
            )
        }
    }

    // Why tabs pass their own provider: the default one clears every store when the NavDisplay leaves.
    @Test
    fun defaultDecoratorClearsViewModelsWhenNavDisplayLeaves() = runComposeUiTest {
        val owner = TabViewModelStoreOwner()
        setContent { Page(owner, provider = null) }
        val first = vm!!

        runOnIdle { shown.value = false }
        waitForIdle()

        assertTrue(first.cleared)
    }

    @Test
    fun parkedTabKeepsViewModelsUntilClosed() = runComposeUiTest {
        val controller = TabsController(
            homeNav = TabNavigation3Actions(Screen.RecentScreen),
            keys = TabKeys(listOf(CoreTabKeyProvider)),
            prefs = TabPrefsSnapshot(clickOpensNewTab = false),
        )
        val window = controller.windows.single()
        val id = controller.open(Screen.Settings, TabOpenMode.Foreground, window.activeId.value)
        val tab = window.tabs.single { it.id == id }

        setContent { Page(tab.viewModelStoreOwner, tab.viewModelStoreProvider) }
        val first = vm!!

        // Park, then show again: same instance, never cleared.
        runOnIdle { shown.value = false }
        waitForIdle()
        assertFalse(first.cleared)

        runOnIdle { shown.value = true }
        waitForIdle()
        assertSame(first, vm)
        assertFalse(first.cleared)

        // Park, then close: cleared.
        runOnIdle { shown.value = false }
        waitForIdle()
        controller.close(id)
        assertTrue(first.cleared)
    }
}
