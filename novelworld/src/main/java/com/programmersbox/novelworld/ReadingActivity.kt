package com.programmersbox.novelworld

import android.net.Uri
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.text.HtmlCompat
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.textview.MaterialTextView
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.battery
import com.programmersbox.helpfulutils.timeTick
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.Storage
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

class ReadViewModel(
    activity: ComponentActivity,
    handle: SavedStateHandle,
    genericInfo: GenericInfo,
    model: Flow<List<String>>? = handle.get<String>("currentChapter")
        ?.fromJson<ChapterModel>(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))
        ?.getChapterInfo()
        ?.map { it.mapNotNull(Storage::link) }
) : ViewModel() {

    companion object {
        const val NovelReaderRoute =
            "novelreader?currentChapter={currentChapter}&novelTitle={novelTitle}&novelUrl={novelUrl}&novelInfoUrl={novelInfoUrl}"

        fun navigateToNovelReader(
            navController: NavController,
            currentChapter: ChapterModel,
            novelTitle: String,
            novelUrl: String,
            novelInfoUrl: String,
        ) {
            val current = Uri.encode(currentChapter.toJson(ChapterModel::class.java to ChapterModelSerializer()))

            navController.navigate(
                "novelreader?currentChapter=$current&novelTitle=${novelTitle}&novelUrl=${novelUrl}&novelInfoUrl=${novelInfoUrl}"
            ) { launchSingleTop = true }
        }
    }

    val ad by lazy { AdRequest.Builder().build() }

    private val dao by lazy { ItemDatabase.getInstance(activity).itemDao() }

    val list by lazy { ChapterList(activity, genericInfo).get().orEmpty() }

    private val novelUrl by lazy { handle.get<String>("novelInfoUrl") ?: "" }
    val title by lazy { handle.get<String>("novelTitle") ?: "" }

    var currentChapter: Int by mutableStateOf(0)

    var batteryColor by mutableStateOf(androidx.compose.ui.graphics.Color.White)
    var batteryIcon by mutableStateOf(BatteryInformation.BatteryViewType.UNKNOWN)
    var batteryPercent by mutableStateOf(0f)

    val batteryInformation by lazy { BatteryInformation(activity) }

    val pageList = mutableStateOf("")
    val isLoadingPages = mutableStateOf(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            batteryInformation.composeSetupFlow(
                androidx.compose.ui.graphics.Color.White
            ) {
                batteryColor = it.first
                batteryIcon = it.second
            }
        }

        val url = handle.get<String>("novelUrl") ?: ""
        currentChapter = list.indexOfFirst { l -> l.url == url }

        loadPages(model)
    }

    fun addChapterToWatched(newChapter: Int, chapter: () -> Unit) {
        currentChapter = newChapter
        list.getOrNull(newChapter)?.let { item ->
            ChapterWatched(item.url, item.name, novelUrl)
                .let {
                    viewModelScope.launch {
                        dao.insertChapter(it)
                        FirebaseDb.insertEpisodeWatchedFlow(it).collect()
                        withContext(Dispatchers.Main) { chapter() }
                    }
                }

            loadPages(item.getChapterInfo().mapNotNull { it.mapNotNull { it.link } })
        }
    }

    fun loadPages(modelPath: Flow<List<String>>?) {
        viewModelScope.launch {
            modelPath
                ?.onStart {
                    pageList.value = ""
                    isLoadingPages.value = true
                }
                ?.onEach {
                    pageList.value = it.firstOrNull().orEmpty()
                    isLoadingPages.value = false
                }
                ?.collect()
        }
    }

}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun NovelReader() {

    LifecycleHandle(
        onStop = { BaseMainActivity.showNavBar = true },
        onDestroy = { BaseMainActivity.showNavBar = true },
        onCreate = { BaseMainActivity.showNavBar = false },
        onStart = { BaseMainActivity.showNavBar = false },
        onResume = { BaseMainActivity.showNavBar = false }
    )

    val genericInfo = LocalGenericInfo.current
    val activity = LocalActivity.current
    val context = LocalContext.current
    val readVm: ReadViewModel = viewModel { ReadViewModel(activity, createSavedStateHandle(), genericInfo) }

    DisposableEffect(context) {
        val batteryInfo = context.battery {
            readVm.batteryPercent = it.percent
            readVm.batteryInformation.batteryLevel.tryEmit(it.percent)
            readVm.batteryInformation.batteryInfo.tryEmit(it)
        }
        onDispose { context.unregisterReceiver(batteryInfo) }
    }

    val scope = rememberCoroutineScope()
    val swipeState = rememberSwipeRefreshState(isRefreshing = readVm.isLoadingPages.value)

    var showInfo by remember { mutableStateOf(true) }

    var settingsPopup by remember { mutableStateOf(false) }

    if (settingsPopup) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            onDismissRequest = { settingsPopup = false },
            title = { Text(stringResource(R.string.settings)) },
            text = {
                Column(
                    modifier = Modifier.padding(5.dp)
                ) {
                    SliderSetting(
                        scope = scope,
                        settingIcon = Icons.Default.BatteryAlert,
                        settingTitle = R.string.battery_alert_percentage,
                        settingSummary = R.string.battery_default,
                        preference = BATTERY_PERCENT,
                        initialValue = runBlocking { context.dataStore.data.first()[BATTERY_PERCENT] ?: 20 },
                        range = 1f..100f,
                        steps = 0
                    )
                }
            },
            confirmButton = { TextButton(onClick = { settingsPopup = false }) { Text(stringResource(R.string.ok)) } }
        )
    }

    fun showToast() {
        activity.runOnUiThread { Toast.makeText(context, R.string.addedChapterItem, Toast.LENGTH_SHORT).show() }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    BackHandler(drawerState.isOpen) {
        scope.launch {
            when {
                drawerState.isOpen -> drawerState.close()
            }
        }
    }

    val contentScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val showItems = showInfo

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val topAppBarScrollState = rememberTopAppBarState()
            val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    Insets {
                        SmallTopAppBar(
                            title = { Text(readVm.title) },
                            actions = { PageIndicator(readVm.list.size - readVm.currentChapter, readVm.list.size) },
                            scrollBehavior = scrollBehavior
                        )
                    }
                },
                bottomBar = {
                    if (!BuildConfig.DEBUG) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            factory = {
                                AdView(it).apply {
                                    setAdSize(AdSize.BANNER)
                                    adUnitId = context.getString(R.string.ad_unit_id)
                                    loadAd(readVm.ad)
                                }
                            }
                        )
                    }
                }
            ) { p ->
                if (drawerState.isOpen) {
                    LazyColumn(
                        state = rememberLazyListState(readVm.currentChapter.coerceIn(0, readVm.list.lastIndex)),
                        contentPadding = p,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(readVm.list) { i, c ->

                            var showChangeChapter by remember { mutableStateOf(false) }

                            if (showChangeChapter) {
                                AlertDialog(
                                    onDismissRequest = { showChangeChapter = false },
                                    title = { Text(stringResource(R.string.changeToChapter, c.name)) },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                showChangeChapter = false
                                                readVm.addChapterToWatched(i, ::showToast)
                                            }
                                        ) { Text(stringResource(R.string.yes)) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showChangeChapter = false }) { Text(stringResource(R.string.no)) }
                                    }
                                )
                            }

                            Surface(
                                modifier = Modifier.padding(horizontal = 5.dp),
                                border = BorderStroke(
                                    1.dp,
                                    animateColorAsState(
                                        if (readVm.currentChapter == i) M3MaterialTheme.colorScheme.onSurface
                                        else M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    ).value
                                ),
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 5.dp
                            ) {
                                ListItem(
                                    headlineText = { Text(c.name) },
                                    leadingContent = if (readVm.currentChapter == i) {
                                        { Icon(Icons.Default.ArrowRight, null) }
                                    } else null,
                                    modifier = Modifier.clickable { showChangeChapter = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {

        Scaffold(
            modifier = Modifier.nestedScroll(contentScrollBehavior.nestedScrollConnection),
            topBar = {
                TopBar(
                    showItems = showItems,
                    contentScrollBehavior = contentScrollBehavior,
                    readVm = readVm
                )
            },
            bottomBar = {
                BottomBar(
                    showItems = showItems,
                    contentScrollBehavior = contentScrollBehavior,
                    readVm = readVm,
                    showToast = ::showToast,
                    settingsPopup = { settingsPopup = it }
                )
            }
        ) { p ->
            //TODO: If/when swipe refresh gains a swipe up to refresh, make the swipe up go to the next chapter
            SwipeRefresh(
                state = swipeState,
                onRefresh = {
                    readVm.loadPages(
                        readVm.list.getOrNull(readVm.currentChapter)
                            ?.getChapterInfo()
                            ?.map { it.mapNotNull(Storage::link) }
                    )
                },
                indicatorPadding = p
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(p)
                        .fillMaxSize()
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { showInfo = !showInfo }
                            ),
                        factory = { MaterialTextView(it) },
                        update = { it.text = HtmlCompat.fromHtml(readVm.pageList.value, HtmlCompat.FROM_HTML_MODE_COMPACT) }
                    )

                    if (readVm.currentChapter <= 0) {
                        Text(
                            stringResource(id = R.string.reachedLastChapter),
                            style = M3MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = M3MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally)
                        )
                    }

                    if (!BuildConfig.DEBUG) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = {
                                AdView(it).apply {
                                    setAdSize(AdSize.BANNER)
                                    adUnitId = context.getString(R.string.ad_unit_id)
                                    loadAd(readVm.ad)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    showItems: Boolean,
    contentScrollBehavior: TopAppBarScrollBehavior,
    readVm: ReadViewModel
) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = showItems,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        CenterAlignedTopAppBar(
            scrollBehavior = contentScrollBehavior,
            modifier = modifier,
            navigationIcon = {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        readVm.batteryIcon.composeIcon,
                        contentDescription = null,
                        tint = animateColorAsState(
                            if (readVm.batteryColor == androidx.compose.ui.graphics.Color.White) M3MaterialTheme.colorScheme.onSurface
                            else readVm.batteryColor
                        ).value
                    )
                    AnimatedContent(
                        targetState = readVm.batteryPercent.toInt(),
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInVertically { height -> height } + fadeIn() with
                                        slideOutVertically { height -> -height } + fadeOut()
                            } else {
                                slideInVertically { height -> -height } + fadeIn() with
                                        slideOutVertically { height -> height } + fadeOut()
                            }
                                .using(SizeTransform(clip = false))
                        }
                    ) { targetBattery ->
                        Text(
                            "$targetBattery%",
                            style = M3MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            title = {
                var time by remember { mutableStateOf(System.currentTimeMillis()) }

                DisposableEffect(context) {
                    val timeReceiver = context.timeTick { _, _ -> time = System.currentTimeMillis() }
                    onDispose { context.unregisterReceiver(timeReceiver) }
                }

                AnimatedContent(
                    targetState = time,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn() with
                                slideOutVertically { height -> -height } + fadeOut())
                            .using(SizeTransform(clip = false))
                    }
                ) { targetTime ->
                    Text(
                        DateFormat.getTimeFormat(LocalContext.current).format(targetTime).toString(),
                        style = M3MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            },
            actions = { PageIndicator(readVm.list.size - readVm.currentChapter, readVm.list.size) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    showItems: Boolean,
    contentScrollBehavior: TopAppBarScrollBehavior,
    readVm: ReadViewModel,
    showToast: () -> Unit,
    settingsPopup: (Boolean) -> Unit
) {
    AnimatedVisibility(
        visible = showItems,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut()
    ) {
        BottomAppBar(
            modifier = modifier,
            containerColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
                .containerColor(contentScrollBehavior.state.collapsedFraction).value,
            contentColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
                .titleContentColor(contentScrollBehavior.state.collapsedFraction).value
        ) {

            val prevShown = readVm.currentChapter < readVm.list.lastIndex
            val nextShown = readVm.currentChapter > 0

            AnimatedVisibility(
                visible = prevShown && readVm.list.size > 1,
                enter = expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
                PreviousButton(
                    viewModel = readVm,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .weight(
                            when {
                                prevShown && nextShown -> 8f / 3f
                                prevShown -> 4f
                                else -> 4f
                            }
                        ),
                    previousChapter = showToast
                )
            }

            GoBackButton(
                modifier = Modifier
                    .weight(
                        animateFloatAsState(
                            when {
                                prevShown && nextShown -> 8f / 3f
                                prevShown || nextShown -> 4f
                                else -> 8f
                            }
                        ).value
                    )
            )

            AnimatedVisibility(
                visible = nextShown && readVm.list.size > 1,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                NextButton(
                    viewModel = readVm,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .weight(
                            when {
                                prevShown && nextShown -> 8f / 3f
                                nextShown -> 4f
                                else -> 4f
                            }
                        ),
                    nextChapter = showToast
                )
            }

            IconButton(
                onClick = { settingsPopup(true) },
                modifier = Modifier.weight(2f)
            ) {
                Icon(
                    Icons.Default.Settings,
                    null,
                    tint = M3MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int) {
    Row {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInVertically { height -> height } + fadeIn() with
                            slideOutVertically { height -> -height } + fadeOut()
                } else {
                    slideInVertically { height -> -height } + fadeIn() with
                            slideOutVertically { height -> height } + fadeOut()
                }
                    .using(SizeTransform(clip = false))
            }
        ) { targetPage ->
            Text(
                "$targetPage",
                style = M3MaterialTheme.typography.bodyLarge,
            )
        }

        Text(
            "/$pageCount",
            style = M3MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun GoBackButton(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    OutlinedButton(
        onClick = { navController.popBackStack() },
        modifier = modifier,
        border = BorderStroke(ButtonDefaults.outlinedButtonBorder.width, M3MaterialTheme.colorScheme.primary)
    ) { Text(stringResource(id = R.string.goBack), color = M3MaterialTheme.colorScheme.primary) }
}

@Composable
private fun NextButton(viewModel: ReadViewModel, modifier: Modifier = Modifier, nextChapter: () -> Unit) {
    Button(
        onClick = { viewModel.addChapterToWatched(viewModel.currentChapter - 1, nextChapter) },
        modifier = modifier,
        border = BorderStroke(ButtonDefaults.outlinedButtonBorder.width, M3MaterialTheme.colorScheme.primary)
    ) { Text(stringResource(id = R.string.loadNextChapter)) }
}

@Composable
private fun PreviousButton(viewModel: ReadViewModel, modifier: Modifier = Modifier, previousChapter: () -> Unit) {
    TextButton(
        onClick = { viewModel.addChapterToWatched(viewModel.currentChapter + 1, previousChapter) },
        modifier = modifier
    ) { Text(stringResource(id = R.string.loadPreviousChapter)) }
}

@Composable
private fun SliderSetting(
    scope: CoroutineScope,
    settingIcon: ImageVector,
    @StringRes settingTitle: Int,
    @StringRes settingSummary: Int,
    preference: Preferences.Key<Int>,
    initialValue: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    val context = LocalContext.current
    ConstraintLayout(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        val (
            icon,
            title,
            summary,
            slider,
            value
        ) = createRefs()

        Icon(
            settingIcon,
            null,
            modifier = Modifier
                .constrainAs(icon) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .padding(8.dp)
        )

        Text(
            stringResource(settingTitle),
            style = M3MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                start.linkTo(icon.end, margin = 10.dp)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            stringResource(settingSummary),
            style = M3MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            modifier = Modifier.constrainAs(summary) {
                top.linkTo(title.bottom)
                end.linkTo(parent.end)
                start.linkTo(icon.end, margin = 10.dp)
                width = Dimension.fillToConstraints
            }
        )

        var sliderValue by remember { mutableStateOf(initialValue.toFloat()) }

        androidx.compose.material3.Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                scope.launch { context.updatePref(preference, sliderValue.toInt()) }
            },
            valueRange = range,
            steps = steps,
            modifier = Modifier.constrainAs(slider) {
                top.linkTo(summary.bottom)
                end.linkTo(value.start)
                start.linkTo(icon.end)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            sliderValue.toInt().toString(),
            style = M3MaterialTheme.typography.titleMedium,
            modifier = Modifier.constrainAs(value) {
                end.linkTo(parent.end)
                start.linkTo(slider.end)
                centerVerticallyTo(slider)
            }
        )

    }
}