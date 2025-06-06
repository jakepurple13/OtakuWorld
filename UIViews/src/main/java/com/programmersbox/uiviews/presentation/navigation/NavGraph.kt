package com.programmersbox.uiviews.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.about.AboutLibrariesScreen
import com.programmersbox.kmpuiviews.presentation.all.AllScreen
import com.programmersbox.kmpuiviews.presentation.details.DetailsScreen
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteScreen
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchScreen
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreen
import com.programmersbox.kmpuiviews.presentation.recent.RecentView
import com.programmersbox.kmpuiviews.presentation.settings.accountinfo.AccountInfoScreen
import com.programmersbox.kmpuiviews.presentation.settings.general.GeneralSettings
import com.programmersbox.kmpuiviews.presentation.settings.incognito.IncognitoScreen
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuCustomListScreenStandAlone
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuListView
import com.programmersbox.kmpuiviews.presentation.settings.lists.deletefromlist.DeleteFromListScreen
import com.programmersbox.kmpuiviews.presentation.settings.moreinfo.MoreInfoScreen
import com.programmersbox.kmpuiviews.presentation.settings.moresettings.MoreSettingsScreen
import com.programmersbox.kmpuiviews.presentation.settings.notifications.NotificationSettings
import com.programmersbox.kmpuiviews.presentation.settings.player.PlaySettings
import com.programmersbox.kmpuiviews.presentation.settings.prerelease.PrereleaseScreen
import com.programmersbox.kmpuiviews.presentation.settings.qrcode.ScanQrCode
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoScreen
import com.programmersbox.kmpuiviews.presentation.webview.WebViewScreen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.chromeCustomTabs
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.animatedScopeComposable
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.presentation.DebugView
import com.programmersbox.uiviews.presentation.history.HistoryUi
import com.programmersbox.uiviews.presentation.lists.OtakuListScreen
import com.programmersbox.uiviews.presentation.lists.imports.ImportFullListScreen
import com.programmersbox.uiviews.presentation.lists.imports.ImportListScreen
import com.programmersbox.uiviews.presentation.onboarding.OnboardingScreen
import com.programmersbox.uiviews.presentation.settings.SettingScreen
import com.programmersbox.uiviews.presentation.settings.SourceOrderScreen
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadStateScreen
import com.programmersbox.uiviews.presentation.settings.extensions.ExtensionList
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.trackScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

//TODO: MAYBE give each screen an enum of where they are and the transitions are based off of that?

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
fun NavGraphBuilder.navGraph(
    customPreferences: ComposeSettingsDsl,
    windowSize: WindowSizeClass,
    genericInfo: GenericInfo,
    navController: NavigationActions,
    notificationLogo: NotificationLogo,
) {
    dialog<Screen.ScanQrCodeScreen> { ScanQrCode() }
    composable<Screen.OnboardingScreen> {
        OnboardingScreen(
            navController = LocalNavActions.current,
            customPreferences = customPreferences
        )
    }

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
        trackScreen(Screen.RecentScreen)
        RecentView()
    }

    animatedScopeComposable<Screen.AllScreen>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
    ) {
        trackScreen(Screen.AllScreen)
        AllScreen(
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
            windowSize = windowSize,
            details = koinViewModel { parametersOf(it.toRoute<Screen.DetailsScreen.Details>()) }
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
    navigationActions: NavigationActions,
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
                geminiClick = { /*navBackStack.add(Screen.GeminiScreen)*/ },
                sourcesOrderClick = navigationActions::order,
                appDownloadsClick = navigationActions::downloadInstall,
                scanQrCode = navigationActions::scanQrCode,
            )
        }

        composable<Screen.WorkerInfoScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) { WorkerInfoScreen() }

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
            MoreInfoScreen(
                usedLibraryClick = navigationActions::about,
                onPrereleaseClick = navigationActions::prerelease,
                onViewAccountInfoClick = navigationActions::accountInfo,
                shouldShowPrerelease = BuildConfig.DEBUG
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
            FavoriteScreen(
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
            GlobalSearchScreen(
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded,
                screen = it.toRoute()
            )
        }

        animatedScopeComposable<Screen.CustomListScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
        ) {
            trackScreen(Screen.CustomListScreen)
            OtakuListView()
        }

        animatedScopeComposable<Screen.CustomListScreen.CustomListItem>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) }
        ) {
            OtakuCustomListScreenStandAlone(it.toRoute())
        }

        dialog<Screen.CustomListScreen.DeleteFromList> {
            trackScreen("delete_from_list")
            DeleteFromListScreen(
                deleteFromList = it.toRoute()
            )
        }

        composable<Screen.ImportListScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen("import_list")
            ImportListScreen()
        }

        composable<Screen.ImportFullListScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen("import_full_list")
            ImportFullListScreen()
        }

        animatedScopeComposable<Screen.NotificationScreen>(
            deepLinks = listOf(navDeepLink { uriPattern = genericInfo.deepLinkUri + Screen.NotificationScreen.route }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            trackScreen(Screen.NotificationScreen)
            NotificationScreen()
        }

        composable<Screen.ExtensionListScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            trackScreen(Screen.ExtensionListScreen)
            ExtensionList()
        }

        /*composable<Screen.GeminiScreen> {
            trackScreen(Screen.GeminiScreen)
            GeminiRecommendationScreen(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }*/

        composable<Screen.AccountInfo>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            AccountInfoScreen(
                profileUrl = koinViewModel<AccountViewModel>().accountInfo?.photoUrl?.toString(),
            )
        }

        additionalSettings()

        if (BuildConfig.DEBUG) {
            composable<Screen.DebugScreen> {
                DebugView()
            }
        }

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
        trackScreen("global_search")
        GlobalSearchScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded,
            screen = it.toRoute()
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
        NotificationScreen()
    }

    animatedScopeComposable<Screen.FavoriteScreen.Home>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        trackScreen(Screen.FavoriteScreen.Home)
        FavoriteScreen(
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