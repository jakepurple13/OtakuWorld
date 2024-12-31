package com.programmersbox.uiviews.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.programmersbox.gemini.GeminiRecommendationScreen
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.presentation.all.AllView
import com.programmersbox.uiviews.presentation.details.DetailsScreen
import com.programmersbox.uiviews.presentation.favorite.FavoriteUi
import com.programmersbox.uiviews.presentation.globalsearch.GlobalSearchView
import com.programmersbox.uiviews.presentation.history.HistoryUi
import com.programmersbox.uiviews.presentation.lists.DeleteFromListScreen
import com.programmersbox.uiviews.presentation.lists.ImportListScreen
import com.programmersbox.uiviews.presentation.lists.OtakuListScreen
import com.programmersbox.uiviews.presentation.notifications.NotificationsScreen
import com.programmersbox.uiviews.presentation.notifications.cancelNotification
import com.programmersbox.uiviews.presentation.recent.RecentView
import com.programmersbox.uiviews.presentation.settings.ComposeSettingsDsl
import com.programmersbox.uiviews.presentation.settings.ExtensionList
import com.programmersbox.uiviews.presentation.settings.GeneralSettings
import com.programmersbox.uiviews.presentation.settings.InfoSettings
import com.programmersbox.uiviews.presentation.settings.MoreSettingsScreen
import com.programmersbox.uiviews.presentation.settings.NotificationSettings
import com.programmersbox.uiviews.presentation.settings.PlaySettings
import com.programmersbox.uiviews.presentation.settings.SettingScreen
import com.programmersbox.uiviews.presentation.settings.SourceOrderScreen
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.chromeCustomTabs
import com.programmersbox.uiviews.utils.sharedelements.animatedScopeComposable
import com.programmersbox.uiviews.utils.trackScreen

//TODO: MAYBE give each screen an enum of where they are and the transitions are based off of that?

@OptIn(
    ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class
)
fun NavGraphBuilder.navGraph(
    customPreferences: ComposeSettingsDsl,
    windowSize: WindowSizeClass,
    genericInfo: GenericInfo,
    navController: NavHostController,
    notificationLogo: NotificationLogo,
) {
    animatedScopeComposable<Screen.RecentScreen>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) }
    ) {
        trackScreen(Screen.RecentScreen)
        RecentView()
    }

    animatedScopeComposable<Screen.AllScreen>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
    ) {
        trackScreen(Screen.AllScreen)
        AllView(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    settings(customPreferences, windowSize, genericInfo, navController, notificationLogo) { with(genericInfo) { settingsNavSetup() } }

    animatedScopeComposable<Screen.DetailsScreen.Details>(
        deepLinks = listOf(
            navDeepLink<Screen.DetailsScreen.Details>(
                basePath = genericInfo.deepLinkUri + Screen.DetailsScreen.Details::class.qualifiedName
            )
        ),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        trackScreen(Screen.DetailsScreen)
        DetailsScreen(
            logo = notificationLogo,
            windowSize = windowSize
        )
    }

    chromeCustomTabs()

    with(genericInfo) { globalNavSetup() }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
private fun NavGraphBuilder.settings(
    customPreferences: ComposeSettingsDsl,
    windowSize: WindowSizeClass,
    genericInfo: GenericInfo,
    navController: NavHostController,
    notificationLogo: NotificationLogo,
    additionalSettings: NavGraphBuilder.() -> Unit,
) {
    navigation<Screen.Settings>(
        startDestination = Screen.SettingsScreen,
    ) {
        composable<Screen.SettingsScreen>(
            deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + Screen.SettingsScreen.route }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen(Screen.SettingsScreen)
            SettingScreen(
                composeSettingsDsl = customPreferences,
                notificationClick = { navController.navigate(Screen.NotificationScreen) { launchSingleTop = true } },
                favoritesClick = { navController.navigate(Screen.FavoriteScreen) { launchSingleTop = true } },
                historyClick = { navController.navigate(Screen.HistoryScreen) { launchSingleTop = true } },
                globalSearchClick = { navController.navigate(Screen.GlobalSearchScreen()) { launchSingleTop = true } },
                listClick = { navController.navigate(Screen.CustomListScreen) { launchSingleTop = true } },
                debugMenuClick = { navController.navigate(Screen.DebugScreen) { launchSingleTop = true } },
                extensionClick = { navController.navigate(Screen.ExtensionListScreen) { launchSingleTop = true } },
                notificationSettingsClick = { navController.navigate(Screen.NotificationsSettings) },
                generalClick = { navController.navigate(Screen.GeneralSettings) },
                otherClick = { navController.navigate(Screen.OtherSettings) },
                moreInfoClick = { navController.navigate(Screen.MoreInfoSettings) },
                moreSettingsClick = { navController.navigate(Screen.MoreSettings) },
                geminiClick = { navController.navigate(Screen.GeminiScreen) },
                sourcesOrderClick = { navController.navigate(Screen.OrderScreen) }
            )
        }

        composable<Screen.OrderScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen(Screen.OrderScreen)
            SourceOrderScreen()
        }

        composable<Screen.NotificationsSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen(Screen.NotificationsSettings)
            NotificationSettings()
        }

        composable<Screen.GeneralSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen(Screen.GeneralSettings)
            GeneralSettings(customPreferences.generalSettings)
        }

        composable<Screen.MoreInfoSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen(Screen.MoreInfoSettings)
            InfoSettings(
                usedLibraryClick = { navController.navigate(Screen.AboutScreen) { launchSingleTop = true } }
            )
        }

        composable<Screen.OtherSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen(Screen.OtherSettings)
            PlaySettings(customPreferences.playerSettings)
        }

        composable<Screen.MoreSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen(Screen.MoreSettings)
            MoreSettingsScreen()
        }

        animatedScopeComposable<Screen.HistoryScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            trackScreen(Screen.HistoryScreen)
            HistoryUi()
        }

        animatedScopeComposable<Screen.FavoriteScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            trackScreen(Screen.FavoriteScreen)
            FavoriteUi(
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        composable<Screen.AboutScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            trackScreen(Screen.AboutScreen)
            AboutLibrariesScreen()
        }

        composable<Screen.GlobalSearchScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            trackScreen("global_search")
            GlobalSearchView(
                notificationLogo = notificationLogo,
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        animatedScopeComposable<Screen.CustomListScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
        ) {
            trackScreen(Screen.CustomListScreen)
            OtakuListScreen(
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        dialog<Screen.CustomListScreen.DeleteFromList> {
            trackScreen("delete_from_list")
            DeleteFromListScreen(
                deleteFromList = it.toRoute()
            )
        }

        composable<Screen.ImportListScreen> {
            trackScreen("import_list")
            ImportListScreen()
        }

        animatedScopeComposable<Screen.NotificationScreen>(
            deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + Screen.NotificationScreen.route }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            trackScreen(Screen.NotificationScreen)
            val context = LocalContext.current
            val notificationManager = remember { context.notificationManager }
            NotificationsScreen(
                notificationLogo = notificationLogo,
                cancelNotificationById = notificationManager::cancel,
                cancelNotification = notificationManager::cancelNotification
            )
        }

        composable<Screen.ExtensionListScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen(Screen.ExtensionListScreen)
            ExtensionList()
        }

        composable<Screen.GeminiScreen> {
            trackScreen(Screen.GeminiScreen)
            GeminiRecommendationScreen(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }

        additionalSettings()

        if (BuildConfig.DEBUG) {
            composable<Screen.DebugScreen> {
                DebugView()
            }
        }
    }

    //These few are specifically so Settings won't get highlighted when selecting these screens from navigation
    composable<Screen.GlobalSearchScreen.Home>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        trackScreen("global_search")
        GlobalSearchView(
            notificationLogo = notificationLogo,
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    animatedScopeComposable<Screen.CustomListScreen.Home>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
    ) {
        trackScreen(Screen.CustomListScreen.Home)
        OtakuListScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    animatedScopeComposable<Screen.NotificationScreen.Home>(
        deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + Screen.NotificationScreen.route }),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        trackScreen(Screen.NotificationScreen.Home)
        val context = LocalContext.current
        val notificationManager = remember { context.notificationManager }
        NotificationsScreen(
            notificationLogo = notificationLogo,
            cancelNotificationById = notificationManager::cancel,
            cancelNotification = notificationManager::cancelNotification
        )
    }

    animatedScopeComposable<Screen.FavoriteScreen.Home>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        trackScreen(Screen.FavoriteScreen.Home)
        FavoriteUi(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    composable<Screen.ExtensionListScreen.Home>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        trackScreen(Screen.ExtensionListScreen.Home)
        ExtensionList()
    }
}