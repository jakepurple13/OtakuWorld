package com.programmersbox.uiviews.presentation.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingScreen
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.PreviewThemeColorsSizes
import com.skydoves.landscapist.glide.GlideImage
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun SettingScreen(
    composeSettingsDsl: ComposeSettingsDsl,
    debugMenuClick: () -> Unit = {},
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
    accountSettings: @Composable () -> Unit = {
        val appConfig: AppConfig = koinInject()
        if (appConfig.buildType == BuildType.Full) {
            AccountSettings()
        }
    },
) {
    /*
    //TODO: This will be for the future when this works again
    // right now it runs into java.lang.NoClassDefFoundError: Failed resolution of: Lio/ktor/client/plugins/HttpTimeout;
    // once it doesn't, this will be fully implemented
    val showGemini by vm.showGemini.collectAsStateWithLifecycle(false)
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
    }
     */
    SettingScreen(
        composeSettingsDsl = composeSettingsDsl,
        notificationClick = notificationClick,
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
        accountSettings = accountSettings,
        onDebugBuild = {
            if (BuildConfig.DEBUG) {
                PreferenceSetting(
                    settingTitle = { Text("Debug Menu") },
                    settingIcon = { Icon(Icons.Default.Android, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null,
                        onClick = debugMenuClick
                    )
                )
            }
        }
    )
}

/*
//TODO: This will be for the future when this works again
internal enum class SettingChoice {
    Notification,
    General,
    Other,
    MoreInfo,
    None
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
internal fun SettingScreen(
    composeSettingsDsl: ComposeSettingsDsl,
    debugMenuClick: () -> Unit = {},
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
) {
    val navController = LocalNavController.current
    val navigator = rememberListDetailPaneScaffoldNavigator(
        //scaffoldDirective = calculateStandardPaneScaffoldDirective(currentWindowAdaptiveInfo())
    )
    var settingChoice by remember { mutableStateOf(SettingChoice.General) }

    BackHandler(settingChoice != SettingChoice.None) {
        navigator.navigateBack()
        settingChoice = SettingChoice.None
    }

    fun ChangeSetting(choice: SettingChoice) {
        settingChoice = choice
        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
                OtakuScaffold(
                    topBar = {
                        InsetLargeTopAppBar(
                            title = { Text(stringResource(R.string.settings)) },
                            scrollBehavior = scrollBehavior,
                            actions = {
                                if (BuildConfig.FLAVOR != "noFirebase") {
                                    AccountSettings()
                                }
                            }
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
                            debugMenuClick = debugMenuClick,
                            favoritesClick = favoritesClick,
                            historyClick = historyClick,
                            globalSearchClick = globalSearchClick,
                            listClick = listClick,
                            extensionClick = extensionClick,
                            notificationSettingsClick = { ChangeSetting(SettingChoice.Notification) },
                            generalClick = { ChangeSetting(SettingChoice.General) },
                            otherClick = { ChangeSetting(SettingChoice.Other) },
                            moreInfoClick = { ChangeSetting(SettingChoice.MoreInfo) }
                        )
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = navigator.currentDestination?.content ?: SettingChoice.None,
                    label = "",
                    transitionSpec = {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (fadeOut() + slideOutHorizontally { -it })
                    },
                ) { targetState ->
                    when (targetState) {
                        SettingChoice.Notification -> NotificationSettings()
                        SettingChoice.General -> GeneralSettings(composeSettingsDsl.generalSettings)
                        SettingChoice.Other -> PlaySettings(composeSettingsDsl.playerSettings)
                        SettingChoice.MoreInfo -> InfoSettings {
                            navController.navigate(Screen.AboutScreen.route) { launchSingleTop = true }
                        }

                        SettingChoice.None -> {}
                    }
                }
            }
        }
    )
}*/

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@PreviewThemeColorsSizes
@Composable
private fun SettingsPreview() {
    PreviewTheme {
        SettingScreen(
            composeSettingsDsl = ComposeSettingsDsl(),
            notificationClick = {},
            favoritesClick = {},
            historyClick = {},
            globalSearchClick = {},
            listClick = {},
            extensionClick = {},
            notificationSettingsClick = {},
            generalClick = {},
            otherClick = {},
            moreInfoClick = {},
            moreSettingsClick = {},
            geminiClick = {},
            sourcesOrderClick = {},
            appDownloadsClick = {},
            accountSettings = {},
            onDebugBuild = {}
        )
    }
}

@Composable
private fun AccountSettings(
    viewModel: AccountViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val navController = LocalNavController.current

    val accountInfo = viewModel.accountInfo

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.done)) } },
            icon = {
                GlideImage(
                    imageModel = { accountInfo?.photoUrl },
                    loading = { Icon(Icons.Default.AccountCircle, null) },
                    failure = { Icon(Icons.Default.AccountCircle, null) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(40.dp)
                        .clickable { showDialog = true }
                )
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.account_category_title))
                    Text(accountInfo?.displayName ?: "User")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(if (accountInfo != null) R.string.logOut else R.string.logIn)) },
                        modifier = Modifier.clickable {
                            showDialog = false
                            (activity as? ComponentActivity)?.let { viewModel.signInOrOut(context, it) }
                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("View Account Info") },
                        modifier = Modifier.clickable {
                            showDialog = false
                            navController.navigate(Screen.AccountInfo) { launchSingleTop = true }
                        }
                    )
                }
            }
        )
    }

    GlideImage(
        imageModel = { accountInfo?.photoUrl },
        loading = { Icon(Icons.Default.AccountCircle, null) },
        failure = { Icon(Icons.Default.AccountCircle, null) },
        modifier = Modifier
            .clip(CircleShape)
            .size(40.dp)
            .clickable { showDialog = true }
    )
}
