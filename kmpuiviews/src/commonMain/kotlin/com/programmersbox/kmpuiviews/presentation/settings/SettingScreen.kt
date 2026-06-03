package com.programmersbox.kmpuiviews.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.showTranslationScreen
import com.programmersbox.kmpuiviews.presentation.settings.utils.showSourceChooser
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalCurrentSource
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.versionCode
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.currentSource
import otakuworld.kmpuiviews.generated.resources.currentVersion
import otakuworld.kmpuiviews.generated.resources.custom_lists_title
import otakuworld.kmpuiviews.generated.resources.general_menu_title
import otakuworld.kmpuiviews.generated.resources.global_search
import otakuworld.kmpuiviews.generated.resources.more_info_category
import otakuworld.kmpuiviews.generated.resources.more_settings
import otakuworld.kmpuiviews.generated.resources.notifications_category_title
import otakuworld.kmpuiviews.generated.resources.pending_saved_notifications
import otakuworld.kmpuiviews.generated.resources.playSettings
import otakuworld.kmpuiviews.generated.resources.settings
import otakuworld.kmpuiviews.generated.resources.viewFavoritesMenu
import otakuworld.kmpuiviews.generated.resources.viewTranslationModels
import otakuworld.kmpuiviews.generated.resources.view_extensions
import otakuworld.kmpuiviews.generated.resources.view_notifications_title
import otakuworld.kmpuiviews.generated.resources.view_source_in_browser

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun SettingScreen(
    composeSettingsDsl: ComposeSettingsDsl,
    navigationActions: NavigationActions = LocalNavActions.current,
    notificationClick: () -> Unit = navigationActions::notifications,
    favoritesClick: () -> Unit = navigationActions::favorites,
    historyClick: () -> Unit = navigationActions::history,
    globalSearchClick: () -> Unit = navigationActions::globalSearch,
    listClick: () -> Unit = navigationActions::customList,
    extensionClick: () -> Unit = navigationActions::extensionList,
    notificationSettingsClick: () -> Unit = navigationActions::notificationsSettings,
    generalClick: () -> Unit = navigationActions::general,
    otherClick: () -> Unit = navigationActions::otherSettings,
    moreInfoClick: () -> Unit = navigationActions::moreInfo,
    moreSettingsClick: () -> Unit = navigationActions::moreSettings,
    geminiClick: () -> Unit = { navigationActions.navigate(Screen.GeminiScreen) },
    sourcesOrderClick: () -> Unit = navigationActions::order,
    appDownloadsClick: () -> Unit = navigationActions::downloadInstall,
    scanQrCode: () -> Unit = navigationActions::scanQrCode,
    securityClick: () -> Unit = navigationActions::security,
    bookmarksClick: () -> Unit = navigationActions::bookmarks,
    accountSettings: @Composable () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    OtakuScaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
                scrollBehavior = scrollBehavior,
                actions = { accountSettings() }
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { p ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(p)
        ) {
            SettingsScreen(
                notificationClick = notificationClick,
                composeSettingsDsl = composeSettingsDsl,
                favoritesClick = favoritesClick,
                historyClick = historyClick,
                globalSearchClick = globalSearchClick,
                listClick = listClick,
                extensionClick = extensionClick,
                notificationSettingsClick = notificationSettingsClick,
                generalClick = generalClick,
                otherClick = otherClick,
                moreInfoClick = moreInfoClick,
                moreSettingsClick = moreSettingsClick,
                geminiClick = geminiClick,
                sourcesOrderClick = sourcesOrderClick,
                appDownloadsClick = appDownloadsClick,
                scanQrCode = scanQrCode,
                securityClick = securityClick,
                bookmarksClick = bookmarksClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsScreen(
    vm: SettingViewModel = koinViewModel(),
    notificationClick: () -> Unit,
    composeSettingsDsl: ComposeSettingsDsl,
    favoritesClick: () -> Unit,
    historyClick: () -> Unit,
    globalSearchClick: () -> Unit,
    listClick: () -> Unit,
    extensionClick: () -> Unit,
    notificationSettingsClick: () -> Unit,
    generalClick: () -> Unit,
    otherClick: () -> Unit,
    moreInfoClick: () -> Unit,
    moreSettingsClick: () -> Unit,
    geminiClick: () -> Unit,
    sourcesOrderClick: () -> Unit,
    appDownloadsClick: () -> Unit,
    scanQrCode: () -> Unit,
    securityClick: () -> Unit,
    bookmarksClick: () -> Unit,
) {
    val navController = LocalNavActions.current
    val uriHandler = LocalUriHandler.current
    val source by LocalCurrentSource
        .current
        .asFlow()
        .collectAsStateWithLifecycle(null)

    CategoryGroupListItem {
        segmentedListItem(
            content = { Text(stringResource(Res.string.view_notifications_title)) },
            leadingContent = { Icon(Icons.Default.Notifications, null) },
            supportingContent = { Text(stringResource(Res.string.pending_saved_notifications, vm.savedNotifications)) },
            onClick = notificationClick,
        )

        segmentedListItem(
            content = { Text(stringResource(Res.string.viewFavoritesMenu)) },
            leadingContent = { Icon(Icons.Default.Star, null) },
            onClick = favoritesClick,
        )

        segmentedListItem(
            content = { Text(stringResource(Res.string.custom_lists_title)) },
            leadingContent = { Icon(Icons.AutoMirrored.Default.List, null) },
            onClick = listClick
        )

        segmentedListItem(
            content = { Text(stringResource(Res.string.global_search)) },
            leadingContent = { Icon(Icons.Default.Search, null) },
            onClick = globalSearchClick
        )

        segmentedListItem(
            content = { Text("Scan QR Code") },
            leadingContent = { Icon(Icons.Default.QrCodeScanner, null) },
            onClick = scanQrCode
        )

        segmentedListItem(
            content = { Text("History") },
            leadingContent = { Icon(Icons.Default.History, null) },
            supportingContent = {
                val historyCount by LocalHistoryDao.current
                    .getAllRecentHistoryCount()
                    .collectAsStateWithLifecycle(0)

                Text(historyCount.toString())
            },
            onClick = historyClick
        )

        segmentedListItem(
            content = { Text("App Downloads") },
            leadingContent = { Icon(Icons.Default.GetApp, null) },
            onClick = appDownloadsClick
        )

        segmentedListItem(
            content = { Text("Bookmarks") },
            leadingContent = { Icon(Icons.Default.Bookmark, contentDescription = null) },
            supportingContent = { Text("View and manage bookmarked chapters") },
            onClick = bookmarksClick,
        )

        apply(composeSettingsDsl.viewSettings)
    }

    var showSourceChooser by showSourceChooser()
    var showTranslationScreen by showTranslationScreen()

    CategoryGroupListItem {
        segmentedListItem(
            content = { Text(stringResource(Res.string.currentSource, source?.serviceName.orEmpty())) },
            leadingContent = { Icon(Icons.Default.Source, null) },
            onClick = { showSourceChooser = true }
        )

        segmentedListItem(
            content = { Text("Sources Order") },
            leadingContent = { Icon(Icons.Default.Reorder, null) },
            onClick = sourcesOrderClick
        )

        segmentedListItem(
            content = { Text(stringResource(Res.string.view_extensions)) },
            leadingContent = { Icon(Icons.Default.Extension, null) },
            onClick = extensionClick
        )

        segmentedListItem(
            content = { Text("Url Opener") },
            leadingContent = { Icon(Icons.Default.Web, null) },
            onClick = { navController.navigate(Screen.UrlOpener) },
        )

        segmentedListItem(
            content = { Text("AI Recommendations") },
            leadingContent = { Icon(Icons.Default.AutoAwesome, null) },
            onClick = geminiClick
        )

        if (source != null) {
            segmentedListItem(
                content = { Text(stringResource(Res.string.view_source_in_browser)) },
                leadingContent = { Icon(Icons.Default.OpenInBrowser, null) },
                onClick = { source?.baseUrl?.let { uriHandler.openUri(it) } }
            )
        }

        segmentedListItem(
            content = { Text(stringResource(Res.string.viewTranslationModels)) },
            leadingContent = { Icon(Icons.Default.Language, null) },
            onClick = { showTranslationScreen = true }
        )
    }

    CategoryGroupListItem(
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        segmentedListItem(
            content = { Text(stringResource(Res.string.notifications_category_title)) },
            leadingContent = { Icon(Icons.Default.Notifications, null) },
            onClick = notificationSettingsClick
        )

        segmentedListItem(
            content = { Text(stringResource(Res.string.general_menu_title)) },
            leadingContent = { Icon(Icons.Default.PhoneAndroid, null) },
            onClick = generalClick
        )

        segmentedListItem(
            content = { Text("Security Settings") },
            leadingContent = { Icon(Icons.Default.Security, null) },
            onClick = securityClick
        )

        segmentedListItem(
            content = { Text(stringResource(Res.string.playSettings)) },
            leadingContent = { Icon(Icons.Default.PlayCircleOutline, null) },
            onClick = otherClick
        )

        segmentedListItem(
            content = { Text(stringResource(Res.string.more_settings)) },
            leadingContent = { Icon(Icons.Default.Settings, null) },
            onClick = moreSettingsClick
        )

        segmentedListItem(
            content = { Text(stringResource(Res.string.more_info_category)) },
            leadingContent = { Icon(Icons.Default.Info, null) },
            onClick = moreInfoClick
        )

        segmentedListItem(
            onClick = {},
            enabled = true,
            leadingContent = {
                Image(
                    painterLogo(),
                    null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
            },
            content = { Text(stringResource(Res.string.currentVersion, appVersion())) },
            overlineContent = { Text("Version code: ${versionCode()}") },
        )
    }
}