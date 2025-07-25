package com.programmersbox.kmpuiviews.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.categorySetting
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
import otakuworld.kmpuiviews.generated.resources.additional_settings
import otakuworld.kmpuiviews.generated.resources.currentSource
import otakuworld.kmpuiviews.generated.resources.currentVersion
import otakuworld.kmpuiviews.generated.resources.custom_lists_title
import otakuworld.kmpuiviews.generated.resources.general_menu_title
import otakuworld.kmpuiviews.generated.resources.global_search
import otakuworld.kmpuiviews.generated.resources.history
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
                securityClick = securityClick
            )
        }
    }
}

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
) {
    val navController = LocalNavActions.current
    val uriHandler = LocalUriHandler.current
    val source by LocalCurrentSource
        .current
        .asFlow()
        .collectAsStateWithLifecycle(null)

    CategoryGroup {
        item {
            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.view_notifications_title)) },
                settingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.fillMaxSize()) },
                summaryValue = { Text(stringResource(Res.string.pending_saved_notifications, vm.savedNotifications)) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = notificationClick
                )
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.viewFavoritesMenu)) },
                settingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = favoritesClick
                )
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.custom_lists_title)) },
                settingIcon = { Icon(Icons.AutoMirrored.Default.List, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = listClick
                )
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.global_search)) },
                settingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = globalSearchClick
                )
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text("Scan QR Code") },
                settingIcon = { Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = scanQrCode
                )
            )
        }

        item {
            val historyCount by LocalHistoryDao.current
                .getAllRecentHistoryCount()
                .collectAsStateWithLifecycle(0)

            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.history)) },
                summaryValue = { Text(historyCount.toString()) },
                settingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = historyClick
                )
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text("App Downloads") },
                settingIcon = { Icon(Icons.Default.GetApp, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = appDownloadsClick
                )
            )
        }

        apply(composeSettingsDsl.viewSettings)
    }

    CategoryGroup {
        categorySetting { Text(stringResource(Res.string.general_menu_title)) }

        item {
            var showSourceChooser by showSourceChooser()

            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.currentSource, source?.serviceName.orEmpty())) },
                settingIcon = { Icon(Icons.Default.Source, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null
                ) { showSourceChooser = true }
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text("Sources Order") },
                settingIcon = { Icon(Icons.Default.Reorder, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = sourcesOrderClick
                )
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.view_extensions)) },
                settingIcon = { Icon(Icons.Default.Extension, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = extensionClick
                )
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text("Url Opener") },
                settingIcon = { Icon(Icons.Default.Web, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = { navController.navigate(Screen.UrlOpener) }
                )
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text("AI Recommendations") },
                settingIcon = { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = geminiClick
                )
            )
        }

        if (source != null) {
            item {
                PreferenceSetting(
                    settingTitle = { Text(stringResource(Res.string.view_source_in_browser)) },
                    settingIcon = { Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        enabled = source != null,
                        indication = ripple(),
                        interactionSource = null
                    ) { source?.baseUrl?.let { uriHandler.openUri(it) } }
                )
            }
        }

        item {
            var showTranslationScreen by showTranslationScreen()

            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.viewTranslationModels)) },
                settingIcon = { Icon(Icons.Default.Language, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null,
                    onClick = { showTranslationScreen = true }
                )
            )
        }
    }

    CategoryGroup {
        categorySetting { Text(stringResource(Res.string.additional_settings)) }

        item {
            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.notifications_category_title)) },
                settingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.click(notificationSettingsClick)
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.general_menu_title)) },
                settingIcon = { Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.click(generalClick)
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text("Security Settings") },
                settingIcon = { Icon(Icons.Default.Security, null, modifier = Modifier.fillMaxSize()) },
                onClick = securityClick
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.playSettings)) },
                settingIcon = { Icon(Icons.Default.PlayCircleOutline, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.click(otherClick)
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.more_settings)) },
                settingIcon = { Icon(Icons.Default.Settings, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.click(moreSettingsClick)
            )
        }

        item {
            PreferenceSetting(
                settingTitle = { Text(stringResource(Res.string.more_info_category)) },
                settingIcon = { Icon(Icons.Default.Info, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.click(moreInfoClick)
            )
        }

        item {
            PreferenceSetting(
                settingIcon = {
                    Image(
                        painterLogo(),
                        null,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                settingTitle = {
                    Column {
                        Text(
                            "Version code: ${versionCode()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(stringResource(Res.string.currentVersion, appVersion()))
                    }
                },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun Modifier.click(action: () -> Unit): Modifier = clickable(
    indication = ripple(),
    interactionSource = null,
    onClick = action
)
