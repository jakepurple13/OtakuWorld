package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.quickactions.isMacOs
import com.programmersbox.kmpuiviews.presentation.tabs.LocalSearchFocusRequest
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

        Column(
            Modifier
                .fillMaxSize()
                .onFocusChanged { controller.contentHasFocus = it.hasFocus }
                // Reached only when no focused text field used the key: ⌘← / ⌘→ back and forward.
                .onKeyEvent { event ->
                    tabShortcut(event, isMac = isMacOs, chromeVisible = showNavBar)
                        ?.let { controller.perform(it, window) } != null
                }
        ) {
            if (showNavBar) TabToolbar(tab = active, controller = controller)

            Box(Modifier.weight(1f).onGloballyPositioned { controller.motion.contentBounds[window.id] = it.boundsInWindow() }) {
                val motion = controller.motion
                // Both tabs compose for the length of a switch; each keeps its own saveable-state and ViewModel keys.
                AnimatedContent(
                    targetState = active,
                    transitionSpec = {
                        if (initialState !in window.tabs) {
                            // Closed, or moved to another window: leave at once, in no window twice.
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val grows = motion.hasGrow(targetState.id)
                            // A grow shows the old page until the cover has covered it.
                            val enter = if (grows) EnterTransition.None else fadeIn(motion.fade(TabMotion.Switch))
                            val exit = fadeOut(motion.fade(if (grows) TabMotion.Grow else TabMotion.Switch))
                            enter togetherWith exit
                        }.using(null)
                    },
                    contentKey = { it.id.v },
                    modifier = Modifier.fillMaxSize(),
                ) { tab ->
                    // A closed tab's ViewModelStore is cleared only after its content has left composition.
                    DisposableEffect(tab.id) {
                        controller.attachContent(tab.id)
                        onDispose { controller.detachContent(tab.id) }
                    }
                    val tabActions = remember { DesktopTabActions(controller, tab.id) }
                    val grow = remember { motion.takeGrow(tab.id) }
                    CompositionLocalProvider(
                        LocalTabActions provides tabActions,
                        LocalSearchFocusRequest provides controller.searchFocus,
                    ) {
                        Box(
                            with(tabActions) {
                                Modifier.fillMaxSize()
                                    .graphicsLayer { alpha = grow?.contentAlpha ?: 1f }
                                    .disarmOnPress()
                            }
                        ) {
                            if (tab.pinned) {
                                HomeTab(
                                    tab = tab,
                                    controller = controller,
                                    stateHolder = stateHolder,
                                    genericInfo = genericInfo,
                                    windowSize = windowSize,
                                    customPreferences = customPreferences,
                                )
                            } else {
                                TabContent(
                                    tab = tab,
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
            }
        }
    }
}
