package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.Modifier
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.details.LocalNotesPanelToggle
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
    motion: TabMotionState,
) {
    stateHolder.SaveableStateProvider(tab.id.v) {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides tab.viewModelStoreOwner,
            LocalTabId provides tab.id,
            LocalNotesPanelToggle provides if (tab.current is Screen.DetailsScreen.Details) tab::toggleNotes else null,
        ) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
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
                NotesPanelSlot(tab, motion)
            }
        }
    }
}
