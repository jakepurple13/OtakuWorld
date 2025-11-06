package com.programmersbox.kmpuiviews.presentation.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.analyticsScreen
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import com.programmersbox.kmpuiviews.presentation.navigation.scenestrategy.BottomSheetSceneStrategy
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.LocalSharedElementScope

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun Nav3(
    navigation3Actions: Navigation3Actions,
    genericInfo: KmpGenericInfo,
    windowSize: WindowSizeClass,
    customPreferences: ComposeSettingsDsl,
) {
    val backStack = navigation3Actions.backStack
    LaunchedEffect(Unit) {
        snapshotFlow { backStack }
            .collect {
                val screen = it.lastOrNull()
                logFirebaseMessage("Navigated to: ${screen.toString()}")
                analyticsScreen(screen.toString())
            }
    }

    val sharedEntryInSceneNavEntryDecorator = SharedElementNavDecorator<NavKey>(
        onPop = {},
        decorate = { entry ->
            val animatedScope = runCatching { LocalNavAnimatedContentScope.current }.getOrNull()
            if (animatedScope == null) {
                entry.Content()
                return@SharedElementNavDecorator
            }
            with(LocalSharedElementScope.current!!) {
                Box(
                    Modifier.sharedElement(
                        rememberSharedContentState(entry.contentKey),
                        animatedVisibilityScope = animatedScope,
                    ),
                ) {
                    entry.Content()
                }
            }
        }
    )

    NavDisplay(
        backStack = backStack,
        //onBack = { backStack.removeLastOrNull() },
        sceneStrategy = rememberListDetailSceneStrategy<NavKey>()
                then remember { DialogSceneStrategy() }
                then remember { BottomSheetSceneStrategy() },
        onBack = { navigation3Actions.popBackStack() },
        entryDecorators = listOf(
            sharedEntryInSceneNavEntryDecorator,
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryGraph(
            customPreferences = customPreferences,
            windowSize = windowSize,
            navigationActions = navigation3Actions,
            genericInfo = genericInfo
        ),
        transitionSpec = {
            // Slide in from right when navigating forward
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        modifier = Modifier.fillMaxSize()
    )
}

private class SharedElementNavDecorator<T : Any>(
    onPop: (key: Any) -> Unit,
    decorate: @Composable ((entry: NavEntry<T>) -> Unit),
) : NavEntryDecorator<T>(onPop, decorate)