package com.programmersbox.kmpuiviews.presentation.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.LocalNavigationAnimatedScope
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.LocalSharedElementScope
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun Nav3(
    genericInfo: KmpGenericInfo,
    windowSize: WindowSizeClass,
    customPreferences: ComposeSettingsDsl,
    navigation3Actions: Navigation3Actions = koinInject(),
) {
    val backStack = navigation3Actions.backStack
    LaunchedEffect(Unit) {
        // Read the top entry, not the list itself: the list instance never changes, so it would emit once
        snapshotFlow { backStack.lastOrNull() }
            .collect { screen ->
                logFirebaseMessage("Navigated to: ${screen.toString()}")
                analyticsScreen(screen.toString())
            }
    }

    val sharedEntryInSceneNavEntryDecorator = SharedElementNavDecorator<NavKey>(
        onPop = {},
        decorate = { entry ->
            val animatedScope = LocalNavAnimatedContentScope.current
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

    // Lets customSharedElement find the entry's AnimatedContentScope
    val navigationAnimatedScopeDecorator = remember {
        SharedElementNavDecorator<NavKey>(
            onPop = {},
            decorate = { entry ->
                CompositionLocalProvider(LocalNavigationAnimatedScope provides LocalNavAnimatedContentScope.current) {
                    entry.Content()
                }
            }
        )
    }

    val motion = MaterialTheme.motionScheme

    NavDisplay(
        backStack = backStack,
        //onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(
            rememberListDetailSceneStrategy<NavKey>(),
            DialogSceneStrategy(),
            BottomSheetSceneStrategy()
        ),
        onBack = { navigation3Actions.popBackStack() },
        entryDecorators = listOf(
            //sharedEntryInSceneNavEntryDecorator,
            navigationAnimatedScopeDecorator,
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = koinEntryProvider(),
        // Material shared-axis X: a short offset plus a crossfade instead of a full-width push
        transitionSpec = {
            (slideInHorizontally(motion.defaultSpatialSpec()) { it / 3 } + fadeIn(motion.defaultEffectsSpec())) togetherWith
                    (slideOutHorizontally(motion.defaultSpatialSpec()) { -it / 3 } + fadeOut(motion.fastEffectsSpec()))
        },
        popTransitionSpec = {
            (slideInHorizontally(motion.defaultSpatialSpec()) { -it / 3 } + fadeIn(motion.defaultEffectsSpec())) togetherWith
                    (slideOutHorizontally(motion.defaultSpatialSpec()) { it / 3 } + fadeOut(motion.fastEffectsSpec()))
        },
        // While the back gesture is held the leaving page shrinks, so the gesture reads as a peek
        predictivePopTransitionSpec = {
            (slideInHorizontally(motion.defaultSpatialSpec()) { -it / 4 } + fadeIn(motion.defaultEffectsSpec())) togetherWith
                    (scaleOut(motion.defaultSpatialSpec(), targetScale = 0.9f) + fadeOut(motion.defaultEffectsSpec()))
        },
        modifier = Modifier.fillMaxSize()
    )
}

private class SharedElementNavDecorator<T : Any>(
    onPop: (key: Any) -> Unit,
    decorate: @Composable ((entry: NavEntry<T>) -> Unit),
) : NavEntryDecorator<T>(onPop, decorate)