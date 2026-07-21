package com.programmersbox.kmpuiviews.presentation.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.about.AboutLibrariesScreen
import com.programmersbox.kmpuiviews.presentation.all.AllScreen
import com.programmersbox.kmpuiviews.presentation.bookmarks.BookmarkScreen
import com.programmersbox.kmpuiviews.presentation.details.DetailsScreen
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryDetailScreen
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryFormScreen
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryListScreen
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteScreen
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchScreen
import com.programmersbox.kmpuiviews.presentation.history.HistoryUi
import com.programmersbox.kmpuiviews.presentation.notes.NotesScreen
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreen
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScreen
import com.programmersbox.kmpuiviews.presentation.recent.RecentView
import com.programmersbox.kmpuiviews.presentation.settings.SettingScreen
import com.programmersbox.kmpuiviews.presentation.settings.about.AboutScreen
import com.programmersbox.kmpuiviews.presentation.settings.about.DeveloperScreen
import com.programmersbox.kmpuiviews.presentation.settings.about.DiagnosticsScreen
import com.programmersbox.kmpuiviews.presentation.settings.accountinfo.AccountInfoScreen
import com.programmersbox.kmpuiviews.presentation.settings.appearance.AppearanceScreen
import com.programmersbox.kmpuiviews.presentation.settings.appearance.ColorsScreen
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.BackupWizardScreen
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.RestoreWizardScreen
import com.programmersbox.kmpuiviews.presentation.settings.behavior.BehaviorScreen
import com.programmersbox.kmpuiviews.presentation.settings.behavior.ContentReadingScreen
import com.programmersbox.kmpuiviews.presentation.settings.behavior.LayoutScreen
import com.programmersbox.kmpuiviews.presentation.settings.behavior.PrivacySecurityScreen
import com.programmersbox.kmpuiviews.presentation.settings.data.DataManagementScreen
import com.programmersbox.kmpuiviews.presentation.settings.discover.DiscoverScreen
import com.programmersbox.kmpuiviews.presentation.settings.downloadstate.DownloadStateScreen
import com.programmersbox.kmpuiviews.presentation.settings.exceptions.ExceptionsScreen
import com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionList
import com.programmersbox.kmpuiviews.presentation.settings.general.BlurSettingsScreen
import com.programmersbox.kmpuiviews.presentation.settings.general.DetailsSettingsScreen
import com.programmersbox.kmpuiviews.presentation.settings.general.GeneralSettings
import com.programmersbox.kmpuiviews.presentation.settings.general.ThemeSettingsScreen
import com.programmersbox.kmpuiviews.presentation.settings.incognito.IncognitoScreen
import com.programmersbox.kmpuiviews.presentation.settings.integrations.IntegrationsScreen
import com.programmersbox.kmpuiviews.presentation.settings.library.LibraryScreen
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
import com.programmersbox.kmpuiviews.presentation.settings.sources.SourcesScreen
import com.programmersbox.kmpuiviews.presentation.settings.utils.ColorHelperScreen
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoScreen
import com.programmersbox.kmpuiviews.presentation.urlopener.UrlOpenerScreen
import com.programmersbox.kmpuiviews.presentation.webview.WebViewScreen
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalComposeUiApi::class,
    KoinExperimentalAPI::class
)
fun buildKmpGraph(): Module = module {
    navigation<Screen.RecentScreen> { RecentView() }
    navigation<Screen.DetailsScreen.Details> {
        DetailsScreen(
            windowSize = LocalWindowSizeClass.current,
            details = koinViewModel { parametersOf(it) }
        )
    }

    navigation<Screen.OnboardingScreen> {
        val genericInfo: KmpGenericInfo = koinInject()
        val customPreferences = koinInject<ComposeSettingsDsl>()
        OnboardingScreen(
            navController = LocalNavActions.current,
            customPreferences = customPreferences,
            accountContent = genericInfo::AccountContent
        )
    }

    navigation<Screen.WebViewScreen> {
        WebViewScreen(
            url = it.url
        )
    }

    navigation<Screen.IncognitoScreen> {
        IncognitoScreen()
    }

    navigation<Screen.AllScreen> {
        val windowSize = LocalWindowSizeClass.current
        AllScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    navigation<Screen.BookmarkScreen> {
        val navActions = LocalNavActions.current
        BookmarkScreen(
            onBackPress = { navActions.popBackStack() },
        )
    }

    navigation<Screen.NotesScreen> {
        val navActions = LocalNavActions.current
        NotesScreen(onBackPress = { navActions.popBackStack() })
    }

    dialogEntry<Screen.ScanQrCodeScreen> { ScanQrCode() }

    navigation<Screen.Settings>(
        metadata = ListDetailSceneStrategy.listPane()
    ) {
        val customPreferences = koinInject<ComposeSettingsDsl>()
        val genericInfo: KmpGenericInfo = koinInject()
        val navigationActions = LocalNavActions.current
        SettingScreen(
            composeSettingsDsl = customPreferences,
            navigationActions = navigationActions,
            accountSettings = {
                val appConfig: AppConfig = koinInject()
                if (appConfig.buildType == BuildType.Full) {
                    genericInfo.AccountSettings()
                }
            }
        )
    }

    navigation<Screen.DictionaryScreen>(
        metadata = ListDetailSceneStrategy.listPane()
    ) {
        val navActions = LocalNavActions.current
        HideNavBarWhileOnScreen()
        DictionaryListScreen(
            onBackPress = { navActions.popBackStack() },
            onEntryClick = { id -> navActions.dictionaryDetail(id) },
            onAddClick = { navActions.dictionaryForm(null) },
        )
    }

    detailEntry<Screen.DictionaryScreen.Detail> {
        val navActions = LocalNavActions.current
        HideNavBarWhileOnScreen()
        DictionaryDetailScreen(
            onBackPress = { navActions.popBackStack() },
            onEditClick = { id -> navActions.dictionaryForm(id) },
            vm = koinViewModel { parametersOf(it.id) },
        )
    }

    detailEntry<Screen.DictionaryScreen.Form> {
        val navActions = LocalNavActions.current
        HideNavBarWhileOnScreen()
        DictionaryFormScreen(
            onDone = { navActions.popBackStack() },
            vm = koinViewModel { parametersOf(it) },
        )
    }

    detailEntry<Screen.WorkerInfoScreen> { WorkerInfoScreen() }

    detailEntry<Screen.OrderScreen> {
        SourceOrderScreen()
    }

    detailEntry<Screen.NotificationsSettings> {
        NotificationSettings()
    }

    detailEntry<Screen.GeneralSettings> {
        val customPreferences = koinInject<ComposeSettingsDsl>()
        GeneralSettings(customPreferences.generalSettings)
    }

    detailEntry<Screen.MoreInfoSettings> {
        val navigationActions = LocalNavActions.current
        MoreInfoScreen(
            usedLibraryClick = navigationActions::about,
            onViewAccountInfoClick = navigationActions::accountInfo
        )
    }

    navigation<Screen.PrereleaseScreen> { PrereleaseScreen() }

    detailEntry<Screen.OtherSettings> {
        val customPreferences = koinInject<ComposeSettingsDsl>()
        PlaySettings(customPreferences.playerSettings)
    }

    detailEntry<Screen.MoreSettings> {
        MoreSettingsScreen()
    }

    detailEntry<Screen.BackupWizard> {
        val navActions = LocalNavActions.current
        BackupWizardScreen(onDone = { navActions.popBackStack() })
    }
    detailEntry<Screen.RestoreWizard> {
        val navActions = LocalNavActions.current
        RestoreWizardScreen(onDone = { navActions.popBackStack() })
    }

    detailEntry<Screen.HistoryScreen> {
        HistoryUi()
    }

    navigation<Screen.FavoriteScreen> {
        val windowSize = LocalWindowSizeClass.current
        FavoriteScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }

    twoPaneEntry<Screen.AboutScreen> {
        AboutLibrariesScreen()
    }

    navigation<Screen.GlobalSearchScreen> {
        val windowSize = LocalWindowSizeClass.current
        GlobalSearchScreen(
            isHorizontal = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded,
            screen = it
        )
    }

    navigation<Screen.ImportListScreen> {
        ImportListScreen()
    }

    navigation<Screen.ImportFullListScreen> {
        ImportFullListScreen()
    }

    navigation<Screen.NotificationScreen> {
        NotificationScreen()
    }

    navigation<Screen.ExtensionListScreen> {
        ExtensionList()
    }

    twoPaneEntry<Screen.AccountInfo> {
        val genericInfo: KmpGenericInfo = koinInject()
        AccountInfoScreen(
            profileUrl = genericInfo.ProfileIcon(),
        )
    }

    //additionalSettings()

    navigation<Screen.DownloadInstallScreen> {
        DownloadStateScreen()
    }

    navigation<Screen.UrlOpener> { UrlOpenerScreen() }
    navigation<Screen.ColorHelper> { ColorHelperScreen() }
    twoPaneEntry<Screen.ThemeSettings> { ThemeSettingsScreen() }
    twoPaneEntry<Screen.Settings.Blur> { BlurSettingsScreen() }
    twoPaneEntry<Screen.DetailsSettings> { DetailsSettingsScreen() }
    twoPaneEntry<Screen.SecuritySettings> { SecurityScreen() }
    twoPaneEntry<Screen.ExceptionScreen> { ExceptionsScreen() }

    detailEntry<Screen.Settings.Library> { LibraryScreen() }
    detailEntry<Screen.Settings.Discover> { DiscoverScreen() }
    detailEntry<Screen.Settings.Sources> { SourcesScreen() }
    detailEntry<Screen.Settings.Integrations> { IntegrationsScreen() }
    detailEntry<Screen.Settings.Appearance> { AppearanceScreen() }
    detailEntry<Screen.Settings.Colors> { ColorsScreen() }
    detailEntry<Screen.Settings.Behavior> { BehaviorScreen() }
    detailEntry<Screen.Settings.Layout> { LayoutScreen() }
    detailEntry<Screen.Settings.ContentReading> { ContentReadingScreen() }
    detailEntry<Screen.Settings.PrivacySecurity> { PrivacySecurityScreen() }
    detailEntry<Screen.Settings.Data> { DataManagementScreen() }
    detailEntry<Screen.Settings.About> {
        val navigationActions = LocalNavActions.current
        AboutScreen(
            usedLibraryClick = { navigationActions.about() },
            onViewAccountInfoClick = { navigationActions.accountInfo() },
        )
    }
    detailEntry<Screen.Settings.Diagnostics> { DiagnosticsScreen() }
    detailEntry<Screen.Settings.Developer> { DeveloperScreen() }

    navigation<Screen.CustomListScreen>(
        metadata = ListDetailSceneStrategy.listPane()
    ) {
        OtakuListView()
    }

    navigation<Screen.CustomListScreen.CustomListItem>(
        metadata = ListDetailSceneStrategy.detailPane()
    ) {
        OtakuCustomListScreenStandAlone(it)
    }

    dialogEntry<Screen.CustomListScreen.DeleteFromList> {
        DeleteFromListScreen(
            deleteFromList = it
        )
    }
}

@OptIn(KoinExperimentalAPI::class)
private inline fun <reified T : NavKey> Module.dialogEntry(
    noinline content: @Composable (T) -> Unit,
) = navigation<T>(
    metadata = DialogSceneStrategy.dialog()
) { content(it) }

@OptIn(ExperimentalMaterial3AdaptiveApi::class, KoinExperimentalAPI::class)
private inline fun <reified T : NavKey> Module.twoPaneEntry(
    noinline content: @Composable (T) -> Unit,
) = navigation<T>(
    metadata = ListDetailSceneStrategy.extraPane()
) { content(it) }

@OptIn(ExperimentalMaterial3AdaptiveApi::class, KoinExperimentalAPI::class)
private inline fun <reified T : NavKey> Module.detailEntry(
    noinline content: @Composable (T) -> Unit,
) = navigation<T>(
    metadata = ListDetailSceneStrategy.detailPane()
) { content(it) }
