package com.programmersbox.kmpuiviews.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.about.AboutLibrariesScreen
import com.programmersbox.kmpuiviews.presentation.all.AllScreen
import com.programmersbox.kmpuiviews.presentation.details.DetailsScreen
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteScreen
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchScreen
import com.programmersbox.kmpuiviews.presentation.history.HistoryUi
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreen
import com.programmersbox.kmpuiviews.presentation.recent.RecentView
import com.programmersbox.kmpuiviews.presentation.recommendations.RecommendationScreen
import com.programmersbox.kmpuiviews.presentation.settings.accountinfo.AccountInfoScreen
import com.programmersbox.kmpuiviews.presentation.settings.downloadstate.DownloadStateScreen
import com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionList
import com.programmersbox.kmpuiviews.presentation.settings.general.GeneralSettings
import com.programmersbox.kmpuiviews.presentation.settings.incognito.IncognitoScreen
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuCustomListScreenStandAlone
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuListView
import com.programmersbox.kmpuiviews.presentation.settings.lists.deletefromlist.DeleteFromListScreen
import com.programmersbox.kmpuiviews.presentation.settings.lists.imports.ImportFullListScreen
import com.programmersbox.kmpuiviews.presentation.settings.lists.imports.ImportListScreen
import com.programmersbox.kmpuiviews.presentation.settings.moreinfo.MoreInfoScreen
import com.programmersbox.kmpuiviews.presentation.settings.moresettings.MoreSettingsScreen
import com.programmersbox.kmpuiviews.presentation.settings.notifications.NotificationSettings
import com.programmersbox.kmpuiviews.presentation.settings.player.PlaySettings
import com.programmersbox.kmpuiviews.presentation.settings.prerelease.PrereleaseScreen
import com.programmersbox.kmpuiviews.presentation.settings.qrcode.ScanQrCode
import com.programmersbox.kmpuiviews.presentation.settings.sourceorder.SourceOrderScreen
import com.programmersbox.kmpuiviews.presentation.settings.utils.ColorHelperScreen
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoScreen
import com.programmersbox.kmpuiviews.presentation.urlopener.UrlOpenerScreen
import com.programmersbox.kmpuiviews.presentation.webview.WebViewScreen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.animatedScopeComposable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
fun NavGraphBuilder.navGraph(
    customPreferences: ComposeSettingsDsl,
    windowSize: WindowSizeClass,
    navController: NavigationActions,
    isDebug: Boolean,
    genericInfo: KmpGenericInfo,
    deepLink: String,
    onboarding: @Composable (ComposeSettingsDsl) -> Unit,
    settingsScreen: @Composable (ComposeSettingsDsl) -> Unit,
    profileIcon: @Composable () -> String,
    settingsNavSetup: NavGraphBuilder.() -> Unit,
) {
    dialog<Screen.ScanQrCodeScreen> { ScanQrCode() }
    composable<Screen.OnboardingScreen> { onboarding(customPreferences) }
    composable<Screen.GeminiScreen> { RecommendationScreen() }

    composable<Screen.WebViewScreen>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
    ) {
        WebViewScreen(
            url = it.toRoute<Screen.WebViewScreen>().url
        )
    }

    composable<Screen.IncognitoScreen>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
    ) {
        IncognitoScreen()
    }

    animatedScopeComposable<Screen.RecentScreen>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) }
    ) {
        RecentView()
    }

    animatedScopeComposable<Screen.AllScreen>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
    ) {
        AllScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    settings(
        customPreferences = customPreferences,
        windowSize = windowSize,
        debug = isDebug,
        navigationActions = navController,
        settingsScreen = settingsScreen,
        profileIcon = profileIcon,
        deepLink = deepLink,
    ) { genericInfo.settingsNavSetup() }

    animatedScopeComposable<Screen.DetailsScreen.Details>(
        deepLinks = listOf(
            navDeepLink<Screen.DetailsScreen.Details>(
                basePath = deepLink + Screen.DetailsScreen.Details::class.qualifiedName
            )
        ),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        DetailsScreen(
            windowSize = windowSize,
            details = koinViewModel { parametersOf(it.toRoute<Screen.DetailsScreen.Details>()) }
        )
    }

    settingsNavSetup()

    genericInfo.globalNavSetup()
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
private fun NavGraphBuilder.settings(
    customPreferences: ComposeSettingsDsl,
    windowSize: WindowSizeClass,
    debug: Boolean,
    deepLink: String = "",
    navigationActions: NavigationActions,
    settingsScreen: @Composable (ComposeSettingsDsl) -> Unit,
    profileIcon: @Composable () -> String,
    additionalSettings: NavGraphBuilder.() -> Unit,
) {
    navigation<Screen.Settings>(
        startDestination = Screen.SettingsScreen,
    ) {
        composable<Screen.SettingsScreen>(
            deepLinks = listOf(navDeepLink { uriPattern = deepLink + Screen.SettingsScreen.route }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            /*SettingScreen(
                composeSettingsDsl = customPreferences,
                notificationClick = navigationActions::notifications,
                favoritesClick = navigationActions::favorites,
                historyClick = navigationActions::history,
                globalSearchClick = navigationActions::globalSearch,
                listClick = navigationActions::customList,
                debugMenuClick = navigationActions::debug,
                extensionClick = navigationActions::extensionList,
                notificationSettingsClick = navigationActions::notificationsSettings,
                generalClick = navigationActions::general,
                otherClick = navigationActions::otherSettings,
                moreInfoClick = navigationActions::moreInfo,
                moreSettingsClick = navigationActions::moreSettings,
                geminiClick = { *//*navBackStack.add(Screen.GeminiScreen)*//* },
                sourcesOrderClick = navigationActions::order,
                appDownloadsClick = navigationActions::downloadInstall,
                scanQrCode = navigationActions::scanQrCode,
            )*/
            settingsScreen(customPreferences)
        }

        composable<Screen.WorkerInfoScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) { WorkerInfoScreen() }

        composable<Screen.OrderScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            SourceOrderScreen()
        }

        composable<Screen.NotificationsSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            NotificationSettings()
        }

        composable<Screen.GeneralSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            GeneralSettings(customPreferences.generalSettings)
        }

        composable<Screen.MoreInfoSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            MoreInfoScreen(
                usedLibraryClick = navigationActions::about,
                onViewAccountInfoClick = navigationActions::accountInfo
            )
        }

        composable<Screen.PrereleaseScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) { PrereleaseScreen() }

        composable<Screen.OtherSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            PlaySettings(customPreferences.playerSettings)
        }

        composable<Screen.MoreSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            MoreSettingsScreen()
        }

        animatedScopeComposable<Screen.HistoryScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            HistoryUi()
        }

        animatedScopeComposable<Screen.FavoriteScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            FavoriteScreen(
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
            )
        }

        composable<Screen.AboutScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            AboutLibrariesScreen()
        }

        composable<Screen.GlobalSearchScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            GlobalSearchScreen(
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded,
                screen = it.toRoute()
            )
        }

        animatedScopeComposable<Screen.CustomListScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
        ) {
            OtakuListView()
        }

        animatedScopeComposable<Screen.CustomListScreen.CustomListItem>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
        ) {
            OtakuCustomListScreenStandAlone(it.toRoute())
        }

        dialog<Screen.CustomListScreen.DeleteFromList> {
            DeleteFromListScreen(
                deleteFromList = it.toRoute()
            )
        }

        composable<Screen.ImportListScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            ImportListScreen()
        }

        composable<Screen.ImportFullListScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            ImportFullListScreen()
        }

        animatedScopeComposable<Screen.NotificationScreen>(
            deepLinks = listOf(navDeepLink { uriPattern = deepLink + Screen.NotificationScreen.route }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            NotificationScreen()
        }

        composable<Screen.ExtensionListScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            ExtensionList()
        }

        composable<Screen.AccountInfo>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            AccountInfoScreen(
                profileUrl = profileIcon()
            )
        }

        additionalSettings()

        composable<Screen.DownloadInstallScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            DownloadStateScreen()
        }
    }

    //These few are specifically so Settings won't get highlighted when selecting these screens from navigation
    composable<Screen.GlobalSearchScreen.Home>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        GlobalSearchScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded,
            screen = it.toRoute()
        )
    }

    animatedScopeComposable<Screen.CustomListScreen.Home>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
    ) {
        OtakuListView()
    }

    animatedScopeComposable<Screen.NotificationScreen.Home>(
        deepLinks = listOf(navDeepLink { uriPattern = deepLink + Screen.NotificationScreen.route }),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        NotificationScreen()
    }

    animatedScopeComposable<Screen.FavoriteScreen.Home>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        FavoriteScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    composable<Screen.ExtensionListScreen.Home>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        ExtensionList()
    }

    composable<Screen.UrlOpener> {
        UrlOpenerScreen()
    }

    composable<Screen.ColorHelper> { ColorHelperScreen() }
}