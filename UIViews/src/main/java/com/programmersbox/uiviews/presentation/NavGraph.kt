package com.programmersbox.uiviews.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import com.programmersbox.kmpuiviews.presentation.NavigationActions
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.about.AboutLibrariesScreen
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreen
import com.programmersbox.kmpuiviews.presentation.settings.accountinfo.AccountInfoScreen
import com.programmersbox.kmpuiviews.presentation.settings.incognito.IncognitoScreen
import com.programmersbox.kmpuiviews.presentation.settings.lists.deletefromlist.DeleteFromListScreen
import com.programmersbox.kmpuiviews.presentation.settings.moresettings.MoreSettingsScreen
import com.programmersbox.kmpuiviews.presentation.settings.notifications.NotificationSettings
import com.programmersbox.kmpuiviews.presentation.settings.player.PlaySettings
import com.programmersbox.kmpuiviews.presentation.settings.qrcode.ScanQrCode
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoScreen
import com.programmersbox.kmpuiviews.presentation.webview.WebViewScreen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.chromeCustomTabs
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.animatedScopeComposable
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.presentation.all.AllView
import com.programmersbox.uiviews.presentation.details.DetailsScreen
import com.programmersbox.uiviews.presentation.favorite.FavoriteUi
import com.programmersbox.uiviews.presentation.globalsearch.GlobalSearchView
import com.programmersbox.uiviews.presentation.history.HistoryUi
import com.programmersbox.uiviews.presentation.lists.OtakuListScreen
import com.programmersbox.uiviews.presentation.lists.imports.ImportFullListScreen
import com.programmersbox.uiviews.presentation.lists.imports.ImportListScreen
import com.programmersbox.uiviews.presentation.onboarding.OnboardingScreen
import com.programmersbox.uiviews.presentation.recent.RecentView
import com.programmersbox.uiviews.presentation.settings.GeneralSettings
import com.programmersbox.uiviews.presentation.settings.InfoSettings
import com.programmersbox.uiviews.presentation.settings.SettingScreen
import com.programmersbox.uiviews.presentation.settings.SourceOrderScreen
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadStateScreen
import com.programmersbox.uiviews.presentation.settings.extensions.ExtensionList
import com.programmersbox.uiviews.presentation.settings.updateprerelease.PrereleaseScreen
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
    navController: NavHostController,
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
                geminiClick = { /*navController.navigate(Screen.GeminiScreen)*/ },
                sourcesOrderClick = { navController.navigate(Screen.OrderScreen) },
                appDownloadsClick = { navController.navigate(Screen.DownloadInstallScreen) },
                scanQrCode = { navController.navigate(Screen.ScanQrCodeScreen) },
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
            InfoSettings(
                usedLibraryClick = { navController.navigate(Screen.AboutScreen) { launchSingleTop = true } },
                onPrereleaseClick = { navController.navigate(Screen.PrereleaseScreen) { launchSingleTop = true } },
                onViewAccountInfoClick = { navController.navigate(Screen.AccountInfo) { launchSingleTop = true } }
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
                isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded,
                screen = it.toRoute()
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
        GlobalSearchView(
            notificationLogo = notificationLogo,
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

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
fun entryGraph(
    customPreferences: ComposeSettingsDsl,
    notificationLogo: NotificationLogo,
    windowSize: WindowSizeClass,
    genericInfo: GenericInfo,
    navigationActions: NavigationActions,
) = entryProvider<Any> {
    entry<Screen.RecentScreen> { RecentView() }
    entry<Screen.DetailsScreen.Details> {
        DetailsScreen(
            logo = notificationLogo,
            windowSize = windowSize,
            details = koinViewModel { parametersOf(it) }
        )
    }

    entry<Screen.ScanQrCodeScreen> { ScanQrCode() }

    entry<Screen.OnboardingScreen> {
        OnboardingScreen(
            navController = LocalNavActions.current,
            customPreferences = customPreferences
        )
    }

    entry<Screen.WebViewScreen> {
        WebViewScreen(
            url = it.url
        )
    }

    entry<Screen.IncognitoScreen> {
        IncognitoScreen()
    }

    entry<Screen.AllScreen> {
        trackScreen(Screen.AllScreen)
        AllView(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    settingsEntryGraph(
        customPreferences = customPreferences,
        notificationLogo = notificationLogo,
        windowSize = windowSize,
        genericInfo = genericInfo,
        navigationActions = navigationActions
    )

    with(genericInfo) { globalNav3Setup() }

}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3AdaptiveApi::class)
private fun EntryProviderBuilder<Any>.settingsEntryGraph(
    customPreferences: ComposeSettingsDsl,
    notificationLogo: NotificationLogo,
    windowSize: WindowSizeClass,
    genericInfo: GenericInfo,
    navigationActions: NavigationActions,
) {
    entry<Screen.Settings>(
        metadata = ListDetailSceneStrategy.listPane {
            //TODO: Need to add info here
            Text("Please Wait")
        }
    ) {
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

    detailEntry<Screen.WorkerInfoScreen> { WorkerInfoScreen() }

    detailEntry<Screen.OrderScreen> {
        trackScreen(Screen.OrderScreen)
        SourceOrderScreen()
    }

    detailEntry<Screen.NotificationsSettings> {
        trackScreen(Screen.NotificationsSettings)
        NotificationSettings()
    }

    detailEntry<Screen.GeneralSettings> {
        trackScreen(Screen.GeneralSettings)
        GeneralSettings(customPreferences.generalSettings)
    }

    detailEntry<Screen.MoreInfoSettings> {
        trackScreen(Screen.MoreInfoSettings)
        InfoSettings(
            usedLibraryClick = navigationActions::about,
            onPrereleaseClick = navigationActions::prerelease,
            onViewAccountInfoClick = navigationActions::accountInfo
        )
    }

    entry<Screen.PrereleaseScreen> { PrereleaseScreen() }

    detailEntry<Screen.OtherSettings> {
        trackScreen(Screen.OtherSettings)
        PlaySettings(customPreferences.playerSettings)
    }

    detailEntry<Screen.MoreSettings> {
        trackScreen(Screen.MoreSettings)
        MoreSettingsScreen()
    }

    detailEntry<Screen.HistoryScreen> {
        trackScreen(Screen.HistoryScreen)
        HistoryUi()
    }

    entry<Screen.FavoriteScreen> {
        trackScreen(Screen.FavoriteScreen)
        FavoriteUi(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    detailEntry<Screen.AboutScreen> {
        trackScreen(Screen.AboutScreen)
        AboutLibrariesScreen()
    }

    entry<Screen.GlobalSearchScreen> {
        trackScreen("global_search")
        GlobalSearchView(
            notificationLogo = notificationLogo,
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded,
            screen = it
        )
    }

    //TODO: Need to listPane this
    entry<Screen.CustomListScreen> {
        trackScreen(Screen.CustomListScreen)
        OtakuListScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    entry<Screen.CustomListScreen.DeleteFromList> {
        trackScreen("delete_from_list")
        DeleteFromListScreen(
            deleteFromList = it
        )
    }

    entry<Screen.ImportListScreen> {
        trackScreen("import_list")
        ImportListScreen()
    }

    entry<Screen.ImportFullListScreen> {
        trackScreen("import_full_list")
        ImportFullListScreen()
    }

    detailEntry<Screen.NotificationScreen> {
        trackScreen(Screen.NotificationScreen)
        NotificationScreen()
    }

    entry<Screen.ExtensionListScreen> {
        trackScreen(Screen.ExtensionListScreen)
        ExtensionList()
    }

    detailEntry<Screen.AccountInfo> {
        AccountInfoScreen(
            profileUrl = koinViewModel<AccountViewModel>().accountInfo?.photoUrl?.toString(),
        )
    }

    //additionalSettings()

    entry<Screen.DownloadInstallScreen> {
        DownloadStateScreen()
    }

    if (BuildConfig.DEBUG) {
        entry<Screen.DebugScreen> {
            DebugView()
        }
    }

    with(genericInfo) { settingsNav3Setup() }
}

private inline fun <reified T : Any> EntryProviderBuilder<*>.twoPaneEntry(
    noinline content: @Composable (T) -> Unit,
) = entry<T>(
    metadata = TwoPaneScene.twoPane()
) { content(it) }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private inline fun <reified T : Any> EntryProviderBuilder<*>.detailEntry(
    noinline content: @Composable (T) -> Unit,
) = entry<T>(
    metadata = ListDetailSceneStrategy.detailPane()
) { content(it) }