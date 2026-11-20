package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.tabs.LocalTabActions
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.sharedcomponents.repository.ChangingSettingsRepository
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
    changingSettingsRepository: ChangingSettingsRepository = koinInject(),
) {
    // Onboarding and immersive screens hide the nav bar; the toolbar hides with it.
    val showNavBar by changingSettingsRepository.showNavBar.collectAsStateWithLifecycle(true)
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
            if (showNavBar) TabToolbar(tab = active, controller = controller)

            var contentRect by remember { mutableStateOf(Rect.Zero) }
            Box(Modifier.weight(1f).onGloballyPositioned { contentRect = it.boundsInWindow() }) {
                // Keyed so no remembered state leaks from one tab's composition into another's.
                key(active.id.v) {
                    val tabActions = remember { DesktopTabActions(controller, active.id) }
                    val motion = controller.motion
                    // A foreground open grows its cover in; any other switch crossfades. The window's first tab just appears.
                    val grow = remember { motion.takeGrow(active.id) }
                    val switchAlpha = remember { Animatable(if (grow == null && motion.window(window.id).enter()) 0f else 1f) }
                    LaunchedEffect(Unit) {
                        if (switchAlpha.value < 1f) {
                            switchAlpha.animateTo(1f, tween(motion.ms(TabMotion.Switch), easing = motion.easing(TabMotion.Switch)))
                        }
                    }
                    CompositionLocalProvider(LocalTabActions provides tabActions) {
                        Box(
                            with(tabActions) {
                                Modifier.fillMaxSize()
                                    .graphicsLayer { alpha = grow?.contentAlpha ?: switchAlpha.value }
                                    .disarmOnPress()
                            }
                        ) {
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
                    tabActions.Menu()
                }
                TabMotionLayer(
                    controller = controller,
                    window = window,
                    tabSlotBounds = { controller.motion.slotBounds[it] },
                    contentBounds = { contentRect },
                )
            }
        }
    }
}
