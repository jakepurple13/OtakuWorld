package com.programmersbox.uiviews.presentation.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.about.AboutLibrariesScreen
import com.programmersbox.kmpuiviews.presentation.all.AllScreen
import com.programmersbox.kmpuiviews.presentation.details.DetailsScreen
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteScreen
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchScreen
import com.programmersbox.kmpuiviews.presentation.history.HistoryUi
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreen
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScreen
import com.programmersbox.kmpuiviews.presentation.recent.RecentView
import com.programmersbox.kmpuiviews.presentation.recommendations.RecommendationScreen
import com.programmersbox.kmpuiviews.presentation.settings.SettingScreen
import com.programmersbox.kmpuiviews.presentation.settings.accountinfo.AccountInfoScreen
import com.programmersbox.kmpuiviews.presentation.settings.downloadstate.DownloadStateScreen
import com.programmersbox.kmpuiviews.presentation.settings.exceptions.ExceptionsScreen
import com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionList
import com.programmersbox.kmpuiviews.presentation.settings.general.DetailsSettingsScreen
import com.programmersbox.kmpuiviews.presentation.settings.general.GeneralSettings
import com.programmersbox.kmpuiviews.presentation.settings.general.ThemeSettingsScreen
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
import com.programmersbox.kmpuiviews.presentation.settings.security.SecurityScreen
import com.programmersbox.kmpuiviews.presentation.settings.sourceorder.SourceOrderScreen
import com.programmersbox.kmpuiviews.presentation.settings.utils.ColorHelperScreen
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoScreen
import com.programmersbox.kmpuiviews.presentation.urlopener.UrlOpenerScreen
import com.programmersbox.kmpuiviews.presentation.webview.WebViewScreen
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.presentation.DebugView
import com.programmersbox.uiviews.presentation.onboarding.AccountContent
import com.programmersbox.uiviews.presentation.settings.AccountSettings
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
fun entryGraph(
    customPreferences: ComposeSettingsDsl,
    notificationLogo: NotificationLogo,
    windowSize: WindowSizeClass,
    genericInfo: GenericInfo,
    navigationActions: NavigationActions,
) = entryProvider<NavKey> {
    entry<Screen.RecentScreen> { RecentView() }
    entry<Screen.DetailsScreen.Details> {
        DetailsScreen(
            windowSize = windowSize,
            details = koinViewModel { parametersOf(it) }
        )
    }

    dialogEntry<Screen.ScanQrCodeScreen> { ScanQrCode() }

    entry<Screen.OnboardingScreen> {
        OnboardingScreen(
            navController = LocalNavActions.current,
            customPreferences = customPreferences,
            accountContent = { AccountContent() }
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
        AllScreen(
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

    genericInfo.globalNav3Setup()
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3AdaptiveApi::class)
private fun EntryProviderScope<NavKey>.settingsEntryGraph(
    customPreferences: ComposeSettingsDsl,
    notificationLogo: NotificationLogo,
    windowSize: WindowSizeClass,
    genericInfo: GenericInfo,
    navigationActions: NavigationActions,
) {
    entry<Screen.Settings>(
        metadata = ListDetailSceneStrategy.listPane()
    ) {
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
    }

    twoPaneEntry<Screen.WorkerInfoScreen> { WorkerInfoScreen() }

    twoPaneEntry<Screen.OrderScreen> {
        SourceOrderScreen()
    }

    twoPaneEntry<Screen.NotificationsSettings> {
        NotificationSettings()
    }

    twoPaneEntry<Screen.GeneralSettings> {
        GeneralSettings(customPreferences.generalSettings)
    }

    twoPaneEntry<Screen.MoreInfoSettings> {
        MoreInfoScreen(
            usedLibraryClick = navigationActions::about,
            onViewAccountInfoClick = navigationActions::accountInfo
        )
    }

    entry<Screen.PrereleaseScreen> { PrereleaseScreen() }

    twoPaneEntry<Screen.OtherSettings> {
        PlaySettings(customPreferences.playerSettings)
    }

    twoPaneEntry<Screen.MoreSettings> {
        MoreSettingsScreen()
    }

    twoPaneEntry<Screen.HistoryScreen> {
        HistoryUi()
    }

    entry<Screen.FavoriteScreen> {
        FavoriteScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    twoPaneEntry<Screen.AboutScreen> {
        AboutLibrariesScreen()
    }

    entry<Screen.GlobalSearchScreen> {
        GlobalSearchScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded,
            screen = it
        )
    }

    entry<Screen.CustomListScreen>(
        metadata = ListDetailSceneStrategy.listPane()
    ) {
        OtakuListView()
    }

    twoPaneEntry<Screen.CustomListScreen.CustomListItem> {
        OtakuCustomListScreenStandAlone(it)
    }

    dialogEntry<Screen.CustomListScreen.DeleteFromList> {
        DeleteFromListScreen(
            deleteFromList = it
        )
    }

    entry<Screen.ImportListScreen> {
        ImportListScreen()
    }

    entry<Screen.ImportFullListScreen> {
        ImportFullListScreen()
    }

    twoPaneEntry<Screen.NotificationScreen> {
        NotificationScreen()
    }

    entry<Screen.ExtensionListScreen> {
        ExtensionList()
    }

    entry<Screen.GeminiScreen> {
        RecommendationScreen()
    }

    twoPaneEntry<Screen.AccountInfo> {
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

    entry<Screen.UrlOpener> { UrlOpenerScreen() }
    entry<Screen.ColorHelper> { ColorHelperScreen() }
    entry<Screen.ThemeSettings> { ThemeSettingsScreen() }
    entry<Screen.DetailsSettings> { DetailsSettingsScreen() }
    entry<Screen.SecuritySettings> { SecurityScreen() }
    entry<Screen.ExceptionScreen> { ExceptionsScreen() }

    genericInfo.settingsNav3Setup()
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private inline fun <reified T : NavKey> EntryProviderScope<NavKey>.twoPaneEntry(
    noinline content: @Composable (T) -> Unit,
) = entry<T>(
    metadata = ListDetailSceneStrategy.extraPane()
) { content(it) }

private inline fun <reified T : NavKey> EntryProviderScope<NavKey>.dialogEntry(
    noinline content: @Composable (T) -> Unit,
) = entry<T>(
    //TODO: Need to fix
    //metadata = DialogSceneStrategy.dialog()
) { content(it) }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private inline fun <reified T : NavKey> EntryProviderScope<NavKey>.detailEntry(
    noinline content: @Composable (T) -> Unit,
) = entry<T>(
    metadata = ListDetailSceneStrategy.detailPane()
) { content(it) }

/*
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private inline fun <reified T : Any> EntryProviderBuilder<*>.animatedEntry(
    metadata: Map<String, Any> = emptyMap(),
    noinline content: @Composable (T) -> Unit,
) = entry<T>(
    metadata = ListDetailSceneStrategy.detailPane()
) { CompositionLocalProvider(LocalNavigationAnimatedScope provides this) { content(it) } }*/
