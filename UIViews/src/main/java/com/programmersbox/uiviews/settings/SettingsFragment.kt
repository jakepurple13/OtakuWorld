package com.programmersbox.uiviews.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.ripple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.CategorySetting
import com.programmersbox.uiviews.utils.InsetLargeTopAppBar
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LifecycleHandle
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalActivity
import com.programmersbox.uiviews.utils.LocalCurrentSource
import com.programmersbox.uiviews.utils.LocalHistoryDao
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.PreferenceSetting
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.ShowWhen
import com.programmersbox.uiviews.utils.appVersion
import com.programmersbox.uiviews.utils.components.ListBottomScreen
import com.programmersbox.uiviews.utils.components.ListBottomSheetItemModel
import com.programmersbox.uiviews.utils.components.OtakuScaffold
import com.programmersbox.uiviews.utils.currentService
import com.programmersbox.uiviews.utils.showSourceChooser
import com.programmersbox.uiviews.utils.showTranslationScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Locale

class ComposeSettingsDsl {
    internal var generalSettings: @Composable () -> Unit = {}
    internal var viewSettings: @Composable () -> Unit = {}
    internal var playerSettings: @Composable () -> Unit = {}

    fun generalSettings(block: @Composable () -> Unit) {
        generalSettings = block
    }

    fun viewSettings(block: @Composable () -> Unit) {
        viewSettings = block
    }

    fun playerSettings(block: @Composable () -> Unit) {
        playerSettings = block
    }
}

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
) {
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
                notificationSettingsClick = notificationSettingsClick,
                generalClick = generalClick,
                otherClick = otherClick,
                moreInfoClick = moreInfoClick
            )
        }
    }
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
) {
    val navController = LocalNavController.current
    val navigator = rememberListDetailPaneScaffoldNavigator(
        scaffoldDirective = calculateStandardPaneScaffoldDirective(currentWindowAdaptiveInfo())
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
        scaffoldState = navigator.scaffoldState,
        listPane = {
            AnimatedPane(modifier = Modifier) {
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
        }
    ) {
        AnimatedPane(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = settingChoice,
                label = "",
                transitionSpec = {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (fadeOut() + slideOutHorizontally { -it })
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
}*/

@Composable
private fun SettingsScreen(
    dao: ItemDao = LocalItemDao.current,
    vm: SettingsViewModel = viewModel { SettingsViewModel(dao) },
    notificationClick: () -> Unit,
    composeSettingsDsl: ComposeSettingsDsl,
    debugMenuClick: () -> Unit,
    favoritesClick: () -> Unit,
    historyClick: () -> Unit,
    globalSearchClick: () -> Unit,
    listClick: () -> Unit,
    extensionClick: () -> Unit,
    notificationSettingsClick: () -> Unit,
    generalClick: () -> Unit,
    otherClick: () -> Unit,
    moreInfoClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val source by LocalCurrentSource.current.asFlow().collectAsState(initial = null)

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

    ShowWhen(vm.savedNotifications > 0) {
        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_notifications_title)) },
            settingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.fillMaxSize()) },
            summaryValue = { Text(stringResource(R.string.pending_saved_notifications, vm.savedNotifications)) },
            modifier = Modifier.clickable(
                indication = ripple(),
                interactionSource = null,
                onClick = notificationClick
            )
        )
    }

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.viewFavoritesMenu)) },
        settingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = favoritesClick
        )
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(id = R.string.custom_lists_title)) },
        settingIcon = { Icon(Icons.AutoMirrored.Default.List, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = listClick
        )
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.global_search)) },
        settingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = globalSearchClick
        )
    )

    val historyCount by LocalHistoryDao.current.getAllRecentHistoryCount().collectAsState(initial = 0)

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.history)) },
        summaryValue = { Text(historyCount.toString()) },
        settingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = historyClick
        )
    )

    composeSettingsDsl.viewSettings()

    HorizontalDivider()

    CategorySetting { Text(stringResource(R.string.general_menu_title)) }

    var showSourceChooser by showSourceChooser()

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.currentSource, source?.serviceName.orEmpty())) },
        settingIcon = { Icon(Icons.Default.Source, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null
        ) { showSourceChooser = true }
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.view_extensions)) },
        settingIcon = { Icon(Icons.Default.Extension, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = extensionClick
        )
    )

    ShowWhen(visibility = source != null) {
        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_source_in_browser)) },
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
        settingTitle = { Text(stringResource(R.string.viewTranslationModels)) },
        settingIcon = { Icon(Icons.Default.Language, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = ripple(),
            interactionSource = null,
            onClick = { showTranslationScreen = true }
        )
    )

    HorizontalDivider()

    CategorySetting { Text(stringResource(R.string.additional_settings)) }

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.notifications_category_title)) },
        settingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click(notificationSettingsClick)
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.general_menu_title)) },
        settingIcon = { Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click(generalClick)
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.playSettings)) },
        settingIcon = { Icon(Icons.Default.PlayCircleOutline, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click(otherClick)
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.more_info_category)) },
        settingIcon = { Icon(Icons.Default.Info, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click(moreInfoClick)
    )

    PreferenceSetting(
        settingIcon = {
            Image(
                rememberDrawablePainter(drawable = koinInject<AppLogo>().logo),
                null,
                modifier = Modifier.fillMaxSize()
            )
        },
        settingTitle = { Text(stringResource(R.string.currentVersion, appVersion())) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@LightAndDarkPreviews
@Composable
private fun SettingsPreview() {
    PreviewTheme {
        SettingsScaffold(title = "Settings") {
            SettingsScreen(
                composeSettingsDsl = ComposeSettingsDsl(),
                notificationClick = {},
                debugMenuClick = {},
                favoritesClick = {},
                historyClick = {},
                globalSearchClick = {},
                listClick = {},
                extensionClick = {},
                notificationSettingsClick = {},
                generalClick = {},
                otherClick = {},
                moreInfoClick = {}
            )
        }
    }
}

@Composable
private fun AccountSettings(
    viewModel: AccountViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val accountInfo = viewModel.accountInfo

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.done)) } },
            icon = {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(accountInfo?.photoUrl)
                        .crossfade(true)
                        .lifecycle(LocalLifecycleOwner.current)
                        .transformations(CircleCropTransformation())
                        .build(),
                    contentDescription = null,
                    loading = { Icon(Icons.Default.AccountCircle, null) },
                    error = { Icon(Icons.Default.AccountCircle, null) },
                    success = { SubcomposeAsyncImageContent() }
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
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(if (accountInfo != null) R.string.logOut else R.string.logIn)) },
                        modifier = Modifier.clickable {
                            showDialog = false
                            viewModel.signInOrOut(context, activity)
                        }
                    )
                }
            }
        )
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(accountInfo?.photoUrl)
            .crossfade(true)
            .lifecycle(LocalLifecycleOwner.current)
            .transformations(CircleCropTransformation())
            .build(),
        contentDescription = null,
        loading = { Icon(Icons.Default.AccountCircle, null) },
        error = { Icon(Icons.Default.AccountCircle, null) },
        success = { SubcomposeAsyncImageContent() },
        modifier = Modifier
            .size(40.dp)
            .clickable { showDialog = true }
    )
}

@Composable
fun SourceChooserScreen(
    onChosen: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sourceRepository = LocalSourcesRepository.current
    val currentSourceRepository = LocalCurrentSource.current

    ListBottomScreen(
        includeInsetPadding = true,
        title = stringResource(R.string.chooseASource),
        list = sourceRepository.list.filterNot { it.apiService.notWorking },
        onClick = { service ->
            onChosen()
            scope.launch {
                service.let {
                    currentSourceRepository.emit(it.apiService)
                    context.currentService = it.apiService.serviceName
                }
            }
        }
    ) {
        ListBottomSheetItemModel(
            primaryText = it.apiService.serviceName,
            icon = if (it.apiService.serviceName == context.currentService) Icons.Default.Check else null
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun SourceChooserPreview() {
    PreviewTheme {
        SourceChooserScreen {}
    }
}

@Composable
fun TranslationScreen(vm: TranslationViewModel = viewModel()) {
    val scope = rememberCoroutineScope()

    LifecycleHandle(onResume = { vm.loadModels() })

    ListBottomScreen(
        title = stringResource(R.string.chooseModelToDelete),
        list = vm.translationModels.toList(),
        onClick = { item -> scope.launch { vm.deleteModel(item) } },
    ) {
        ListBottomSheetItemModel(
            primaryText = it.language,
            overlineText = try {
                Locale.forLanguageTag(it.language).displayLanguage
            } catch (e: Exception) {
                null
            },
            icon = Icons.Default.Delete
        )
    }
}

private fun Modifier.click(action: () -> Unit): Modifier = this.composed {
    clickable(
        indication = ripple(),
        interactionSource = null,
        onClick = action
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScaffold(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit = {
        InsetSmallTopAppBar(
            title = { Text(title) },
            navigationIcon = { BackButton() },
            scrollBehavior = it,
        )
    },
    content: @Composable ColumnScope.() -> Unit,
) {
    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { topBar(scrollBehavior) },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
    ) { p ->
        Column(
            content = content,
            modifier = Modifier
                .padding(p)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        )
    }
}