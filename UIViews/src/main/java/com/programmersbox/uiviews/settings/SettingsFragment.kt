package com.programmersbox.uiviews.settings

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.CategorySetting
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LifecycleHandle
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalActivity
import com.programmersbox.uiviews.utils.LocalCurrentSource
import com.programmersbox.uiviews.utils.LocalHistoryDao
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.MockAppIcon
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.PreferenceSetting
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.ShowWhen
import com.programmersbox.uiviews.utils.appVersion
import com.programmersbox.uiviews.utils.components.ListBottomScreen
import com.programmersbox.uiviews.utils.components.ListBottomSheetItemModel
import com.programmersbox.uiviews.utils.currentService
import com.programmersbox.uiviews.utils.navigateChromeCustomTabs
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Locale

class ComposeSettingsDsl {
    internal var generalSettings: (@Composable () -> Unit)? = null
    internal var viewSettings: (@Composable () -> Unit)? = null
    internal var playerSettings: (@Composable () -> Unit)? = null

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
    listClick: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (BuildConfig.FLAVOR != "noFirebase") {
                        AccountSettings()
                    }
                }
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
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
                listClick = listClick
            )
        }
    }

}

@Composable
private fun SettingsScreen(
    dao: ItemDao = LocalItemDao.current,
    vm: SettingsViewModel = viewModel { SettingsViewModel(dao) },
    logo: MainLogo = koinInject(),
    notificationClick: () -> Unit,
    composeSettingsDsl: ComposeSettingsDsl,
    debugMenuClick: () -> Unit,
    favoritesClick: () -> Unit,
    historyClick: () -> Unit,
    globalSearchClick: () -> Unit,
    listClick: () -> Unit,
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val source by LocalCurrentSource.current.asFlow().collectAsState(initial = null)

    if (BuildConfig.DEBUG) {
        PreferenceSetting(
            settingTitle = { Text("Debug Menu") },
            settingIcon = { Icon(Icons.Default.Android, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() },
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
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() },
                onClick = notificationClick
            )
        )
    }

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.viewFavoritesMenu)) },
        settingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = favoritesClick
        )
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(id = R.string.custom_lists_title)) },
        settingIcon = { Icon(Icons.Default.List, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = listClick
        )
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.global_search)) },
        settingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = globalSearchClick
        )
    )

    val historyCount by LocalHistoryDao.current.getAllRecentHistoryCount().collectAsState(initial = 0)

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.history)) },
        summaryValue = { Text(historyCount.toString()) },
        settingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = historyClick
        )
    )

    composeSettingsDsl.viewSettings?.invoke()

    Divider()

    CategorySetting { Text(stringResource(R.string.general_menu_title)) }

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.currentSource, source?.serviceName.orEmpty())) },
        settingIcon = { Icon(Icons.Default.Source, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() }
        ) { navController.navigate(Screen.SourceChooserScreen.route) }
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.view_extensions)) },
        settingIcon = { Icon(Icons.Default.Extension, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() }
        ) { navController.navigate(Screen.ExtensionListScreen.route) }
    )

    ShowWhen(visibility = source != null) {
        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_source_in_browser)) },
            settingIcon = { Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                enabled = source != null,
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                source?.baseUrl?.let {
                    navController.navigateChromeCustomTabs(
                        it,
                        {
                            anim {
                                enter = R.anim.slide_in_right
                                popEnter = R.anim.slide_in_right
                                exit = R.anim.slide_out_left
                                popExit = R.anim.slide_out_left
                            }
                        }
                    )
                }
            }
        )
    }

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.viewTranslationModels)) },
        settingIcon = { Icon(Icons.Default.Language, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = { navController.navigate(Screen.TranslationScreen.route) }
        )
    )

    Divider()

    CategorySetting { Text(stringResource(R.string.additional_settings)) }

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.notifications_category_title)) },
        settingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click { navController.navigate(Screen.NotificationsSettings.route) }
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.general_menu_title)) },
        settingIcon = { Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click { navController.navigate(Screen.GeneralSettings.route) }
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.playSettings)) },
        settingIcon = { Icon(Icons.Default.PlayCircleOutline, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click { navController.navigate(Screen.OtherSettings.route) }
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.more_info_category)) },
        settingIcon = { Icon(Icons.Default.Info, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.click { navController.navigate(Screen.MoreInfoSettings.route) }
    )

    PreferenceSetting(
        settingIcon = {
            Image(
                bitmap = AppCompatResources.getDrawable(context, logo.logoId)!!.toBitmap().asImageBitmap(),
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
                logo = MockAppIcon
            )
        }
    }
}

@Composable
private fun AccountSettings(
    viewModel: AccountViewModel = viewModel()
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
fun SourceChooserScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navController = LocalNavController.current
    val sourceRepository = LocalSourcesRepository.current
    val currentSourceRepository = LocalCurrentSource.current

    ListBottomScreen(
        includeInsetPadding = true,
        title = stringResource(R.string.chooseASource),
        list = sourceRepository.list.filterNot { it.apiService.notWorking },
        onClick = { service ->
            navController.popBackStack()
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
        SourceChooserScreen()
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

private fun Modifier.click(action: () -> Unit): Modifier = composed {
    clickable(
        indication = rememberRipple(),
        interactionSource = remember { MutableInteractionSource() },
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
    content: @Composable ColumnScope.() -> Unit
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