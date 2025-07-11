package com.programmersbox.uiviews.presentation.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.navEntryDecorator
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.analyticsScreen
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.navigation.AddBreadcrumbLogging
import com.programmersbox.kmpuiviews.presentation.navigation.navGraph
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScreen
import com.programmersbox.kmpuiviews.presentation.settings.SettingScreen
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.USE_NAV3
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.LocalSharedElementScope
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.presentation.DebugView
import com.programmersbox.uiviews.presentation.navigation.strategy.DialogStrategy
import com.programmersbox.uiviews.presentation.navigation.strategy.TwoPaneSceneStrategy
import com.programmersbox.uiviews.presentation.settings.AccountSettings
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.uiviews.presentation.onboarding.AccountContent
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NavigationGraph(
    navigationActions: NavigationActions,
    genericInfo: GenericInfo,
    windowSize: WindowSizeClass,
    customPreferences: ComposeSettingsDsl,
    notificationLogo: NotificationLogo,
    startDestination: Screen,
    navController: NavHostController,
) {
    if (USE_NAV3) {
        Nav3(
            backStack = (navigationActions as Navigation3Actions).backstack(),
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun Nav3(
    backStack: NavBackStack,
    navigationActions: NavigationActions,
    genericInfo: GenericInfo,
    windowSize: WindowSizeClass,
    customPreferences: ComposeSettingsDsl,
    notificationLogo: NotificationLogo,
) {
    LaunchedEffect(Unit) {
        snapshotFlow { backStack }
            .collect {
                val screen = it.lastOrNull()
                logFirebaseMessage("Navigated to: ${screen.toString()}")
                analyticsScreen(screen.toString())
            }
    }

    val sharedEntryInSceneNavEntryDecorator = navEntryDecorator { entry ->
        with(LocalSharedElementScope.current!!) {
            Box(
                Modifier.sharedElement(
                    rememberSharedContentState(entry.key),
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                ),
            ) {
                entry.content(entry.key)
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        //onBack = { backStack.removeLastOrNull() },
        sceneStrategy = rememberListDetailSceneStrategy<NavKey>()
                then TwoPaneSceneStrategy()
                then DialogStrategy(),
        onBack = { count ->
            repeat(count) {
                if (backStack.isNotEmpty()) {
                    backStack.removeLastOrNull()
                }
            }
        },
        entryDecorators = listOf(
            sharedEntryInSceneNavEntryDecorator,
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

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
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
    AddBreadcrumbLogging(navController)
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        //navGraph(customPreferences, windowSize, genericInfo, navigationActions, notificationLogo)
        navGraph(
            customPreferences = customPreferences,
            windowSize = windowSize,
            genericInfo = genericInfo,
            navController = navigationActions,
            isDebug = BuildConfig.DEBUG,
            onboarding = {
                OnboardingScreen(
                    navController = LocalNavActions.current,
                    customPreferences = it,
                    accountContent = { AccountContent() }
                )
            },
            profileIcon = {
                koinViewModel<AccountViewModel>()
                    .accountInfo
                    ?.photoUrl
                    ?.toString()
                    .orEmpty()
            },
            settingsScreen = {
                SettingScreen(
                    composeSettingsDsl = customPreferences,
                    navigationActions = navigationActions,
                    accountSettings = {
                        val appConfig: AppConfig = koinInject()
                        if (appConfig.buildType == BuildType.Full) {
                            AccountSettings()
                        }
                    }
                )
            },
            deepLink = genericInfo.deepLinkUri,
            settingsNavSetup = {
                if (BuildConfig.DEBUG) {
                    composable<Screen.DebugScreen> {
                        DebugView()
                    }
                }
            }
        )
    }
}