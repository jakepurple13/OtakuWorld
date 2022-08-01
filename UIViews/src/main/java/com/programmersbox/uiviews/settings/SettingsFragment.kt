package com.programmersbox.uiviews.settings

import android.Manifest
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.models.ApiService
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.BuildConfig
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.sharedutils.appUpdateCheck
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.UpdateFlowWorker
import com.programmersbox.uiviews.utils.*
import com.programmersbox.uiviews.utils.components.ListBottomSheet
import com.programmersbox.uiviews.utils.components.ListBottomSheetItemModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

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
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@Composable
fun SettingScreen(
    navController: NavController,
    logo: MainLogo,
    genericInfo: GenericInfo,
    activity: ComponentActivity,
    usedLibraryClick: () -> Unit = {},
    debugMenuClick: () -> Unit = {},
    notificationClick: () -> Unit = {},
    favoritesClick: () -> Unit = {},
    historyClick: () -> Unit = {},
    globalSearchClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val customPreferences = remember { ComposeSettingsDsl().apply(genericInfo.composeCustomPreferences(navController)) }

    val topAppBarScrollState = rememberTopAppBarState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Insets {
                SmallTopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { p ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(p)
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
        ) {

            if (BuildConfig.FLAVOR != "noFirebase") {
                /*Account*/
                AccountSettings(
                    context = context,
                    activity = activity,
                    logo = logo
                )

                Divider()
            }

            /*About*/
            AboutSettings(
                context = context,
                scope = scope,
                activity = activity,
                genericInfo = genericInfo,
                logo = logo
            )

            Divider(modifier = Modifier.padding(top = 5.dp))

            /*Notifications*/
            NotificationSettings(
                context = context,
                scope = scope,
                notificationClick = notificationClick
            )

            /*View*/
            ViewSettings(
                customSettings = customPreferences.viewSettings,
                debugMenuClick = debugMenuClick,
                favoritesClick = favoritesClick,
                historyClick = historyClick
            )

            Divider()

            /*General*/
            GeneralSettings(
                context = context,
                scope = scope,
                //fragment = fragment,
                genericInfo = genericInfo,
                customSettings = customPreferences.generalSettings,
                globalSearchClick = globalSearchClick
            )

            Divider()

            /*Player*/
            PlaySettings(
                context = context,
                scope = scope,
                customSettings = customPreferences.playerSettings
            )

            Divider(modifier = Modifier.padding(top = 5.dp))

            /*More Info*/
            InfoSettings(
                context = context,
                usedLibraryClick = usedLibraryClick
            )
        }
    }

}

@ExperimentalMaterialApi
@Composable
private fun AccountSettings(context: Context, activity: ComponentActivity, logo: MainLogo) {
    CategorySetting { Text(stringResource(R.string.account_category_title)) }

    val viewModel: AccountViewModel = viewModel()
    val accountInfo = viewModel.accountInfo

    PreferenceSetting(
        settingTitle = { Text(accountInfo?.displayName ?: "User") },
        settingIcon = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(accountInfo?.photoUrl)
                    .error(logo.logoId)
                    .placeholder(logo.logoId)
                    .crossfade(true)
                    .lifecycle(LocalLifecycleOwner.current)
                    .transformations(CircleCropTransformation())
                    .build(),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() }
        ) { viewModel.signInOrOut(context, activity) }
    )
}

@ExperimentalMaterialApi
@Composable
private fun AboutSettings(
    context: Context,
    scope: CoroutineScope,
    activity: ComponentActivity,
    genericInfo: GenericInfo,
    logo: MainLogo,
    aboutViewModel: AboutViewModel = viewModel()
) {
    LaunchedEffect(Unit) { aboutViewModel.init(context) }

    CategorySetting(
        settingTitle = { Text(stringResource(R.string.about)) },
        settingIcon = {
            Image(
                bitmap = AppCompatResources.getDrawable(context, logo.logoId)!!.toBitmap().asImageBitmap(),
                null,
                modifier = Modifier.fillMaxSize()
            )
        }
    )

    val appUpdate by appUpdateCheck.subscribeAsState(null)

    PreferenceSetting(
        settingTitle = {
            Text(
                stringResource(
                    R.string.currentVersion,
                    context.packageManager.getPackageInfo(context.packageName, 0)?.versionName.orEmpty()
                )
            )
        },
        modifier = Modifier.clickable { scope.launch(Dispatchers.IO) { aboutViewModel.updateChecker(context) } }
    )

    ShowWhen(
        visibility = AppUpdate.checkForUpdate(
            context.packageManager.getPackageInfo(context.packageName, 0)?.versionName.orEmpty(),
            appUpdate?.update_real_version.orEmpty()
        )
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
                                    appUpdateCheck.value
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
                            context.openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/releases/latest")
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

    val time by Observables.combineLatest(
        updateCheckPublish.map { "Start: ${context.getSystemDateTimeFormat().format(it)}" },
        updateCheckPublishEnd.map { "End: ${context.getSystemDateTimeFormat().format(it)}" }
    )
        .map { "${it.first}\n${it.second}" }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeAsState(
            listOfNotNull(
                context.lastUpdateCheck?.let { "Start: ${context.getSystemDateTimeFormat().format(it)}" },
                context.lastUpdateCheckEnd?.let { "End: ${context.getSystemDateTimeFormat().format(it)}" }
            ).joinToString("\n")
        )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.last_update_check_time)) },
        summaryValue = { Text(time) },
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
        value = aboutViewModel.canCheck,
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

    ShowWhen(aboutViewModel.canCheck) {
        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.clear_update_queue)) },
            summaryValue = { Text(stringResource(R.string.clear_update_queue_summary)) },
            modifier = Modifier
                .alpha(if (aboutViewModel.canCheck) 1f else .38f)
                .clickable(
                    enabled = aboutViewModel.canCheck,
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    val work = WorkManager.getInstance(context)
                    work.cancelUniqueWork("updateChecks")
                    work.pruneWork()
                    OtakuApp.updateSetup(context)
                    Toast
                        .makeText(context, R.string.cleared, Toast.LENGTH_SHORT)
                        .show()
                }
        )
    }
}

@ExperimentalMaterialApi
@Composable
private fun NotificationSettings(
    context: Context,
    scope: CoroutineScope,
    notificationClick: () -> Unit
) {
    val dao = remember { ItemDatabase.getInstance(context).itemDao() }
    val notiViewModel: NotificationViewModel = viewModel { NotificationViewModel(dao = dao) }

    ShowWhen(notiViewModel.savedNotifications > 0) {
        CategorySetting { Text(stringResource(R.string.notifications_category_title)) }

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_notifications_title)) },
            summaryValue = { Text(stringResource(R.string.pending_saved_notifications, notiViewModel.savedNotifications)) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() },
                onClick = notificationClick
            )
        )

        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.are_you_sure_delete_notifications)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val number = dao.deleteAllNotificationsFlow()
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

        Divider()
    }
}

@ExperimentalMaterialApi
@Composable
private fun ViewSettings(
    customSettings: (@Composable () -> Unit)?,
    debugMenuClick: () -> Unit,
    favoritesClick: () -> Unit,
    historyClick: () -> Unit
) {
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
        settingTitle = { Text(stringResource(R.string.viewFavoritesMenu)) },
        settingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = favoritesClick
        )
    )

    val context = LocalContext.current
    val dao = remember { HistoryDatabase.getInstance(context).historyDao() }
    val historyCount by dao.getAllRecentHistoryCount().collectAsState(initial = 0)

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

    customSettings?.invoke()
}

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
private fun GeneralSettings(
    context: Context,
    scope: CoroutineScope,
    genericInfo: GenericInfo,
    customSettings: (@Composable () -> Unit)?,
    globalSearchClick: () -> Unit
) {
    CategorySetting { Text(stringResource(R.string.general_menu_title)) }

    val vm: GeneralViewModel = viewModel()

    val source: ApiService? by sourcePublish.subscribeAsState(initial = null)

    DynamicListSetting(
        settingTitle = { Text(stringResource(R.string.currentSource, source?.serviceName.orEmpty())) },
        dialogTitle = { Text(stringResource(R.string.chooseASource)) },
        settingIcon = { Icon(Icons.Default.Source, null, modifier = Modifier.fillMaxSize()) },
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.done)) } },
        value = source,
        options = { genericInfo.sourceList() },
        updateValue = { service, d ->
            service?.let {
                sourcePublish.onNext(it)
                context.currentService = it.serviceName
            }
            d.value = false
        }
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.view_source_in_browser)) },
        settingIcon = { Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier
            .alpha(animateFloatAsState(targetValue = if (source != null) 1f else 0f).value)
            .clickable(
                enabled = source != null,
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                source?.baseUrl?.let {
                    context.openInCustomChromeBrowser(it) {
                        setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }
            }
    )

    val activity = LocalActivity.current

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.viewTranslationModels)) },
        settingIcon = { Icon(Icons.Default.Language, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = {
                //TODO: Change this into its own screen
                vm.getModels {
                    ListBottomSheet(
                        title = context.getString(R.string.chooseModelToDelete),
                        list = vm.translationModels.toList(),
                        onClick = { item -> vm.deleteModel(item) }
                    ) {
                        ListBottomSheetItemModel(
                            primaryText = it.language,
                            overlineText = try {
                                Locale.forLanguageTag(it.language).displayLanguage
                            } catch (e: Exception) {
                                null
                            }
                        )
                    }.show(activity.supportFragmentManager, "sourceChooser")
                }
            }
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

    val theme by context.themeSetting.collectAsState(initial = "System")

    ListSetting(
        settingTitle = { Text(stringResource(R.string.theme_choice_title)) },
        dialogIcon = { Icon(Icons.Default.SettingsBrightness, null) },
        settingIcon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.fillMaxSize()) },
        dialogTitle = { Text(stringResource(R.string.choose_a_theme)) },
        summaryValue = { Text(theme) },
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.cancel)) } },
        value = theme,
        options = listOf("System", "Light", "Dark"),
        updateValue = { it, d ->
            d.value = false
            scope.launch { context.updatePref(THEME_SETTING, it) }
            when (it) {
                "System" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                "Light" -> AppCompatDelegate.MODE_NIGHT_NO
                "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> null
            }?.let(AppCompatDelegate::setDefaultNightMode)
        }
    )

    val shareChapter by context.shareChapter.collectAsState(initial = true)

    SwitchSetting(
        settingTitle = { Text(stringResource(R.string.share_chapters)) },
        settingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.fillMaxSize()) },
        value = shareChapter,
        updateValue = { scope.launch { context.updatePref(SHARE_CHAPTER, it) } }
    )

    val showAllScreen by context.showAll.collectAsState(initial = true)

    SwitchSetting(
        settingTitle = { Text(stringResource(R.string.show_all_screen)) },
        settingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.fillMaxSize()) },
        value = showAllScreen,
        updateValue = { scope.launch { context.updatePref(SHOW_ALL, it) } }
    )

    var sliderValue by remember { mutableStateOf(runBlocking { context.historySave.first().toFloat() }) }

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

@ExperimentalMaterialApi
@Composable
private fun PlaySettings(context: Context, scope: CoroutineScope, customSettings: (@Composable () -> Unit)?) {
    CategorySetting { Text(stringResource(R.string.playSettings)) }

    var sliderValue by remember { mutableStateOf(runBlocking { context.batteryPercent.first().toFloat() }) }

    SliderSetting(
        sliderValue = sliderValue,
        settingTitle = { Text(stringResource(R.string.battery_alert_percentage)) },
        settingSummary = { Text(stringResource(R.string.battery_default)) },
        settingIcon = { Icon(Icons.Default.BatteryAlert, null) },
        range = 1f..100f,
        updateValue = {
            sliderValue = it
            scope.launch { context.updatePref(BATTERY_PERCENT, sliderValue.toInt()) }
        }
    )

    customSettings?.invoke()
}

@ExperimentalMaterialApi
@Composable
private fun InfoSettings(context: Context, usedLibraryClick: () -> Unit) {
    CategorySetting(settingTitle = { Text(stringResource(R.string.more_info_category)) })

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
        ) { context.openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/releases/latest") }
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.join_discord)) },
        settingIcon = { Icon(painterResource(R.drawable.ic_baseline_discord_24), null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() }
        ) { context.openInCustomChromeBrowser("https://discord.gg/MhhHMWqryg") }
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.support)) },
        summaryValue = { Text(stringResource(R.string.support_summary)) },
        settingIcon = { Icon(Icons.Default.AttachMoney, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() }
        ) { context.openInCustomChromeBrowser("https://ko-fi.com/V7V3D3JI") }
    )

}

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun <T> DynamicListSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    summaryValue: (@Composable () -> Unit)? = null,
    settingTitle: @Composable () -> Unit,
    dialogTitle: @Composable () -> Unit,
    dialogIcon: (@Composable () -> Unit)? = null,
    cancelText: (@Composable (MutableState<Boolean>) -> Unit)? = null,
    confirmText: @Composable (MutableState<Boolean>) -> Unit,
    radioButtonColors: RadioButtonColors = RadioButtonDefaults.colors(),
    value: T,
    options: () -> List<T>,
    viewText: (T) -> String = { it.toString() },
    updateValue: (T, MutableState<Boolean>) -> Unit
) {
    val dialogPopup = remember { mutableStateOf(false) }

    if (dialogPopup.value) {

        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { dialogPopup.value = false },
            title = dialogTitle,
            icon = dialogIcon,
            text = {
                LazyColumn {
                    items(options()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = rememberRipple(),
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { updateValue(it, dialogPopup) }
                                .border(0.dp, Color.Transparent, RoundedCornerShape(20.dp))
                        ) {
                            RadioButton(
                                selected = it == value,
                                onClick = { updateValue(it, dialogPopup) },
                                modifier = Modifier.padding(8.dp),
                                colors = radioButtonColors
                            )
                            Text(
                                viewText(it),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = { confirmText(dialogPopup) },
            dismissButton = cancelText?.let { { it(dialogPopup) } }
        )

    }

    PreferenceSetting(
        settingTitle = settingTitle,
        summaryValue = summaryValue,
        settingIcon = settingIcon,
        modifier = Modifier
            .clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { dialogPopup.value = true }
            .then(modifier)
    )
}