package com.programmersbox.kmpuiviews.tabs

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.navigation.Nav3
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup

/** The tab the current composition belongs to. Null outside the desktop tab shell. */
val LocalTabId = staticCompositionLocalOf<TabId?> { null }

/**
 * One tab's pages. Only the active tab is composed; its saveable state lives in [stateHolder]
 * under the tab id, and its ViewModels in the tab's own store, so a parked tab comes back as it was.
 */
@Composable
fun TabContent(
    tab: TabState,
    stateHolder: SaveableStateHolder,
    genericInfo: KmpGenericInfo,
    windowSize: WindowSizeClass,
    customPreferences: ComposeSettingsDsl,
) {
    stateHolder.SaveableStateProvider(tab.id.v) {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides tab.viewModelStoreOwner,
            LocalTabId provides tab.id,
        ) {
            KmpLocalCompositionSetup(actions = tab.nav) {
                Nav3(
                    genericInfo = genericInfo,
                    windowSize = windowSize,
                    customPreferences = customPreferences,
                    navigation3Actions = tab.nav,
                    viewModelStoreProvider = tab.viewModelStoreProvider,
                )
            }
        }
    }
}
