package com.programmersbox.uiviews.presentation.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.USE_NAV3
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.NotificationLogo

@Composable
fun NavigationGraph(
    backStack: NavBackStack,
    navigationActions: NavigationActions,
    genericInfo: GenericInfo,
    windowSize: WindowSizeClass,
    customPreferences: ComposeSettingsDsl,
    notificationLogo: NotificationLogo,
    startDestination: Screen,
    navController: NavHostController,
) {
    //TODO: Maybe put in a little thing to switch between these two? Prolly not though
    if (USE_NAV3) {
        Nav3(
            backStack = backStack,
            navigationActions = navigationActions,
            genericInfo = genericInfo,
            windowSize = windowSize,
            customPreferences = customPreferences,
            notificationLogo = notificationLogo
        )
    } else {
        Nav2(
            startDestination = startDestination,
            windowSize = windowSize,
            genericInfo = genericInfo,
            navigationActions = navigationActions,
            customPreferences = customPreferences,
            notificationLogo = notificationLogo,
            navController = navController
        )
    }
}


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun Nav3(
    backStack: NavBackStack,
    navigationActions: NavigationActions,
    genericInfo: GenericInfo,
    windowSize: WindowSizeClass,
    customPreferences: ComposeSettingsDsl,
    notificationLogo: NotificationLogo,
) {
    NavDisplay(
        backStack = backStack,
        //onBack = { backStack.removeLastOrNull() },
        sceneStrategy = rememberListDetailSceneStrategy(),
        onBack = { count ->
            repeat(count) {
                if (backStack.isNotEmpty()) {
                    backStack.removeLastOrNull()
                }
            }
        },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryGraph(
            customPreferences = customPreferences,
            notificationLogo = notificationLogo,
            windowSize = windowSize,
            navigationActions = navigationActions,
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

@Composable
private fun Nav2(
    startDestination: Screen,
    windowSize: WindowSizeClass,
    genericInfo: GenericInfo,
    navigationActions: NavigationActions,
    customPreferences: ComposeSettingsDsl,
    notificationLogo: NotificationLogo,
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) { navGraph(customPreferences, windowSize, genericInfo, navigationActions, notificationLogo) }
}