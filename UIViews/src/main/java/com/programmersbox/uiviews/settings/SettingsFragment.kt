package com.programmersbox.uiviews.settings

import android.Manifest
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdateAlt
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
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.models.sourceFlow
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.sharedutils.updateAppCheck
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.SystemThemeMode
import com.programmersbox.uiviews.UpdateFlowWorker
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.CategorySetting
import com.programmersbox.uiviews.utils.DownloadUpdate
import com.programmersbox.uiviews.utils.HISTORY_SAVE
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LifecycleHandle
import com.programmersbox.uiviews.utils.ListSetting
import com.programmersbox.uiviews.utils.LocalActivity
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalHistoryDao
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.PreferenceSetting
import com.programmersbox.uiviews.utils.SHOULD_CHECK
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.ShowWhen
import com.programmersbox.uiviews.utils.SliderSetting
import com.programmersbox.uiviews.utils.SwitchSetting
import com.programmersbox.uiviews.utils.appVersion
import com.programmersbox.uiviews.utils.components.ListBottomScreen
import com.programmersbox.uiviews.utils.components.ListBottomSheetItemModel
import com.programmersbox.uiviews.utils.currentService
import com.programmersbox.uiviews.utils.historySave
import com.programmersbox.uiviews.utils.navigateChromeCustomTabs
import com.programmersbox.uiviews.utils.updatePref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject
import java.io.File
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
    val source by sourceFlow.collectAsState(initial = null)

    CategorySetting { Text(stringResource(R.string.view_menu_category_title)) }

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
        settingTitle = { Text("Lists") },
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
        settingTitle = { Text(stringResource(R.string.view_source_in_browser)) },
        settingIcon = { Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier
            .alpha(animateFloatAsState(targetValue = if (source != null) 1f else 0f, label = "").value)
            .clickable(
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
    val source by sourceFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navController = LocalNavController.current
    val genericInfo = LocalGenericInfo.current

    ListBottomScreen(
        includeInsetPadding = true,
        title = stringResource(R.string.chooseASource),
        list = genericInfo.sourceList(),
        onClick = { service ->
            navController.popBackStack()
            scope.launch {
                service.let {
                    sourceFlow.emit(it)
                    context.currentService = it.serviceName
                }
            }
        }
    ) {
        ListBottomSheetItemModel(
            primaryText = it.serviceName,
            icon = if (it == source) Icons.Default.Check else null
        )
    }
}

@Composable
fun NotificationSettings(
    dao: ItemDao = LocalItemDao.current,
    context: Context = LocalContext.current,
    notiViewModel: NotificationViewModel = viewModel { NotificationViewModel(dao, context) }
) {
    SettingsScaffold(stringResource(R.string.notification_settings)) {
        val scope = rememberCoroutineScope()
        ShowWhen(notiViewModel.savedNotifications > 0) {
            var showDialog by remember { mutableStateOf(false) }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(stringResource(R.string.are_you_sure_delete_notifications)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val number = dao.deleteAllNotifications()
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.deleted_notifications, number),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        context.notificationManager.cancel(42)
                                    }
                                }
                                showDialog = false
                            }
                        ) { Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.no)) } }
                )
            }

            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.delete_saved_notifications_title)) },
                summaryValue = { Text(stringResource(R.string.delete_notifications_summary)) },
                modifier = Modifier
                    .clickable(
                        indication = rememberRipple(),
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showDialog = true }
                    .padding(bottom = 16.dp, top = 8.dp)
            )
        }

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.last_update_check_time)) },
            summaryValue = { Text(notiViewModel.time) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "oneTimeUpdate",
                        ExistingWorkPolicy.KEEP,
                        OneTimeWorkRequestBuilder<UpdateFlowWorker>()
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                    .setRequiresBatteryNotLow(false)
                                    .setRequiresCharging(false)
                                    .setRequiresDeviceIdle(false)
                                    .setRequiresStorageNotLow(false)
                                    .build()
                            )
                            .build()
                    )
            }
        )

        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.are_you_sure_stop_checking)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                context.updatePref(SHOULD_CHECK, false)
                                OtakuApp.updateSetupNow(context, false)
                            }
                            showDialog = false
                        }
                    ) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.no)) } }
            )
        }

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.check_for_periodic_updates)) },
            value = notiViewModel.canCheck,
            updateValue = {
                if (!it) {
                    showDialog = true
                } else {
                    scope.launch {
                        context.updatePref(SHOULD_CHECK, it)
                        OtakuApp.updateSetupNow(context, it)
                    }
                }
            }
        )

        ShowWhen(notiViewModel.canCheck) {
            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.clear_update_queue)) },
                summaryValue = { Text(stringResource(R.string.clear_update_queue_summary)) },
                modifier = Modifier
                    .alpha(if (notiViewModel.canCheck) 1f else .38f)
                    .clickable(
                        enabled = notiViewModel.canCheck,
                        indication = rememberRipple(),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        val work = WorkManager.getInstance(context)
                        work.cancelUniqueWork("updateFlowChecks")
                        work.pruneWork()
                        OtakuApp.updateSetup(context)
                        Toast
                            .makeText(context, R.string.cleared, Toast.LENGTH_SHORT)
                            .show()
                    }
            )
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Composable
fun GeneralSettings(
    customSettings: (@Composable () -> Unit)?,
) {
    SettingsScaffold(stringResource(R.string.general_menu_title)) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val handling = LocalSettingsHandling.current

        val themeSetting by handling.systemThemeMode.collectAsState(initial = SystemThemeMode.FollowSystem)

        val themeText by remember {
            derivedStateOf {
                when (themeSetting) {
                    SystemThemeMode.FollowSystem -> "System"
                    SystemThemeMode.Day -> "Light"
                    SystemThemeMode.Night -> "Dark"
                    else -> "None"
                }
            }
        }

        ListSetting(
            settingTitle = { Text(stringResource(R.string.theme_choice_title)) },
            dialogIcon = { Icon(Icons.Default.SettingsBrightness, null) },
            settingIcon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.fillMaxSize()) },
            dialogTitle = { Text(stringResource(R.string.choose_a_theme)) },
            summaryValue = { Text(themeText) },
            confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.cancel)) } },
            value = themeSetting,
            options = listOf(SystemThemeMode.FollowSystem, SystemThemeMode.Day, SystemThemeMode.Night),
            updateValue = { it, d ->
                d.value = false
                scope.launch { handling.setSystemThemeMode(it) }
                when (it) {
                    SystemThemeMode.FollowSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    SystemThemeMode.Day -> AppCompatDelegate.MODE_NIGHT_NO
                    SystemThemeMode.Night -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> null
                }?.let(AppCompatDelegate::setDefaultNightMode)
            }
        )

        val shareChapter by handling.shareChapter.collectAsState(initial = true)

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.share_chapters)) },
            settingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.fillMaxSize()) },
            value = shareChapter,
            updateValue = { scope.launch { handling.setShareChapter(it) } }
        )

        val showAllScreen by handling.showAll.collectAsState(initial = true)

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.show_all_screen)) },
            settingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.fillMaxSize()) },
            value = showAllScreen,
            updateValue = { scope.launch { handling.setShowAll(it) } }
        )

        var sliderValue by remember { mutableFloatStateOf(runBlocking { context.historySave.first().toFloat() }) }

        SliderSetting(
            sliderValue = sliderValue,
            settingTitle = { Text(stringResource(R.string.history_save_title)) },
            settingSummary = { Text(stringResource(R.string.history_save_summary)) },
            settingIcon = { Icon(Icons.Default.ChangeHistory, null) },
            range = -1f..100f,
            updateValue = {
                sliderValue = it
                scope.launch { context.updatePref(HISTORY_SAVE, sliderValue.toInt()) }
            }
        )

        customSettings?.invoke()
    }
}

@Composable
fun PlaySettings(customSettings: (@Composable () -> Unit)?) {
    SettingsScaffold(stringResource(R.string.playSettings)) {
        val scope = rememberCoroutineScope()
        val settingsHandling = LocalSettingsHandling.current
        val slider by settingsHandling.batteryPercentage.collectAsState(runBlocking { settingsHandling.batteryPercentage.first() })
        var sliderValue by remember(slider) { mutableFloatStateOf(slider.toFloat()) }

        SliderSetting(
            sliderValue = sliderValue,
            settingTitle = { Text(stringResource(R.string.battery_alert_percentage)) },
            settingSummary = { Text(stringResource(R.string.battery_default)) },
            settingIcon = { Icon(Icons.Default.BatteryAlert, null) },
            range = 1f..100f,
            updateValue = { sliderValue = it },
            onValueChangedFinished = { scope.launch { settingsHandling.setBatteryPercentage(sliderValue.toInt()) } }
        )

        customSettings?.invoke()
    }
}

@Composable
fun InfoSettings(
    infoViewModel: MoreInfoViewModel = viewModel(),
    logo: MainLogo,
    usedLibraryClick: () -> Unit,
) {
    val activity = LocalActivity.current
    val genericInfo = LocalGenericInfo.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    SettingsScaffold(stringResource(R.string.more_info_category)) {
        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_libraries_used)) },
            settingIcon = { Icon(Icons.Default.LibraryBooks, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() },
                onClick = usedLibraryClick
            )
        )

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_on_github)) },
            settingIcon = { Icon(painterResource(R.drawable.github_icon), null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { navController.navigateChromeCustomTabs("https://github.com/jakepurple13/OtakuWorld/releases/latest") }
        )

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.join_discord)) },
            settingIcon = { Icon(painterResource(R.drawable.ic_baseline_discord_24), null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { navController.navigateChromeCustomTabs("https://discord.gg/MhhHMWqryg") }
        )

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.support)) },
            summaryValue = { Text(stringResource(R.string.support_summary)) },
            settingIcon = { Icon(Icons.Default.AttachMoney, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { navController.navigateChromeCustomTabs("https://ko-fi.com/V7V3D3JI") }
        )

        val appUpdate by updateAppCheck.collectAsState(null)

        PreferenceSetting(
            settingIcon = {
                Image(
                    bitmap = AppCompatResources.getDrawable(context, logo.logoId)!!.toBitmap().asImageBitmap(),
                    null,
                    modifier = Modifier.fillMaxSize()
                )
            },
            settingTitle = { Text(stringResource(R.string.currentVersion, appVersion())) },
            modifier = Modifier.clickable { scope.launch(Dispatchers.IO) { infoViewModel.updateChecker(context) } }
        )

        ShowWhen(
            visibility = AppUpdate.checkForUpdate(appVersion(), appUpdate?.update_real_version.orEmpty())
        ) {
            var showDialog by remember { mutableStateOf(false) }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(stringResource(R.string.updateTo, appUpdate?.update_real_version.orEmpty())) },
                    text = { Text(stringResource(R.string.please_update_for_latest_features)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                activity.requestPermissions(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                ) {
                                    if (it.isGranted) {
                                        updateAppCheck.value
                                            ?.let { a ->
                                                val isApkAlreadyThere = File(
                                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/",
                                                    a.let(genericInfo.apkString).toString()
                                                )
                                                if (isApkAlreadyThere.exists()) isApkAlreadyThere.delete()
                                                DownloadUpdate(context, context.packageName).downloadUpdate(a)
                                            }
                                    }
                                }
                                showDialog = false
                            }
                        ) { Text(stringResource(R.string.update)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.notNow)) }
                        TextButton(
                            onClick = {
                                navController.navigateChromeCustomTabs("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                                showDialog = false
                            }
                        ) { Text(stringResource(R.string.gotoBrowser)) }
                    }
                )
            }

            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.update_available)) },
                summaryValue = { Text(stringResource(R.string.updateTo, appUpdate?.update_real_version.orEmpty())) },
                modifier = Modifier.clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { showDialog = true },
                settingIcon = {
                    Icon(
                        Icons.Default.SystemUpdateAlt,
                        null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        }
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
private fun SettingsScaffold(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
    ) { p ->
        Column(
            modifier = Modifier
                .padding(p)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            content = content
        )
    }
}