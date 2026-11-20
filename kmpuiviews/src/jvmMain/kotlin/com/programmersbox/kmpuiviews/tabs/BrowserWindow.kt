package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import kotlinx.coroutines.flow.filterIsInstance
import org.koin.compose.koinInject

/**
 * One browser window's body: the toolbar, then the active tab. Other tabs are parked: not
 * composed, with their saveable state and ViewModels kept until they close.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun BrowserWindow(
    window: BrowserWindowState,
    controller: TabsController,
    genericInfo: KmpGenericInfo = koinInject(),
) {
    val customPreferences = remember { ComposeSettingsDsl().apply(genericInfo.composeCustomPreferences()) }
    val windowSize = calculateWindowSizeClass()
    val stateHolder = rememberSaveableStateHolder()

    LaunchedEffect(controller) {
        controller.events
            .filterIsInstance<TabEvent.Closed>()
            .collect { stateHolder.removeState(it.tab.v) }
    }

    DesktopAppEffects(windowSize = windowSize, genericInfo = genericInfo) {
        val active = window.tabs.firstOrNull { it.id == window.activeId.value } ?: window.tabs.first()

        Column(Modifier.fillMaxSize()) {
            TabToolbar(tab = active, controller = controller)

            Box(Modifier.weight(1f)) {
                // Keyed so no remembered state leaks from one tab's composition into another's.
                key(active.id.v) {
                    if (active.pinned) {
                        HomeTab(
                            tab = active,
                            controller = controller,
                            stateHolder = stateHolder,
                            genericInfo = genericInfo,
                            windowSize = windowSize,
                            customPreferences = customPreferences,
                        )
                    } else {
                        TabContent(
                            tab = active,
                            stateHolder = stateHolder,
                            genericInfo = genericInfo,
                            windowSize = windowSize,
                            customPreferences = customPreferences,
                        )
                    }
                }
            }
        }
    }
}
