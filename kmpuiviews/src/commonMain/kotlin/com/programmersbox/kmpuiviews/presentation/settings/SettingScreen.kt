package com.programmersbox.kmpuiviews.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.CategorySetting
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.ShowWhen
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.showTranslationScreen
import com.programmersbox.kmpuiviews.presentation.settings.utils.showSourceChooser
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalCurrentSource
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
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
    notificationClick: () -> Unit = {},
    favoritesClick: () -> Unit = {},
    historyClick: () -> Unit = {},
    globalSearchClick: () -> Unit = {},
    listClick: () -> Unit = {},
    extensionClick: () -> Unit = {},
    notificationSettingsClick: () -> Unit = {},
    generalClick: () -> Unit = {},
    otherClick: () -> Unit = {},
    moreInfoClick: () -> Unit = {},
    moreSettingsClick: () -> Unit = {},
    geminiClick: () -> Unit = {},
    sourcesOrderClick: () -> Unit = {},
    appDownloadsClick: () -> Unit = {},
    onDebugBuild: @Composable () -> Unit,
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
                onDebugBuild = onDebugBuild
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
    onDebugBuild: @Composable () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val source by LocalCurrentSource.current.asFlow().collectAsStateWithLifecycle(null)

    onDebugBuild()

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

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.viewFavoritesMenu)) },
        settingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = favoritesClick
        )
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.custom_lists_title)) },
        settingIcon = { Icon(Icons.AutoMirrored.Default.List, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = listClick
        )
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.global_search)) },
        settingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = globalSearchClick
        )
    )

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

    PreferenceSetting(
        settingTitle = { Text("App Downloads") },
        settingIcon = { Icon(Icons.Default.GetApp, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = appDownloadsClick
        )
    )

    composeSettingsDsl.viewSettings()

    HorizontalDivider()

    CategorySetting { Text(stringResource(Res.string.general_menu_title)) }

    var showSourceChooser by showSourceChooser()

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.currentSource, source?.serviceName.orEmpty())) },
        settingIcon = { Icon(Icons.Default.Source, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null
        ) { showSourceChooser = true }
    )

    PreferenceSetting(
        settingTitle = { Text("Sources Order") },
        settingIcon = { Icon(Icons.Default.Reorder, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = sourcesOrderClick
        )
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.view_extensions)) },
        settingIcon = { Icon(Icons.Default.Extension, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = extensionClick
        )
    )

    ShowWhen(visibility = source != null) {
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

    //TODO: This will be for the future when this works again
    // right now it runs into java.lang.NoClassDefFoundError: Failed resolution of: Lio/ktor/client/plugins/HttpTimeout;
    // once it doesn't, this will be fully implemented
    /*val showGemini by vm.showGemini.collectAsStateWithLifecycle(false)
    if (showGemini) {
        PreferenceSetting(
            settingTitle = { Text("Gemini Recommendations") },
            settingIcon = { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = ripple(),
                interactionSource = null,
                onClick = geminiClick
            )
        )
    }*/

    HorizontalDivider()

    CategorySetting { Text(stringResource(Res.string.additional_settings)) }

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.notifications_category_title)) },
        settingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click(notificationSettingsClick)
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.general_menu_title)) },
        settingIcon = { Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click(generalClick)
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.playSettings)) },
        settingIcon = { Icon(Icons.Default.PlayCircleOutline, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click(otherClick)
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.more_settings)) },
        settingIcon = { Icon(Icons.Default.Settings, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click(moreSettingsClick)
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(Res.string.more_info_category)) },
        settingIcon = { Icon(Icons.Default.Info, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click(moreInfoClick)
    )

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
}

private fun Modifier.click(action: () -> Unit): Modifier = clickable(
    indication = ripple(),
    interactionSource = null,
    onClick = action
)
