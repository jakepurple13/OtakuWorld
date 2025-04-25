package com.programmersbox.novelworld

import android.app.Activity
import android.net.Uri
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DatabaseBuilder
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.battery
import com.programmersbox.helpfulutils.timeTick
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.Storage
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.presentation.components.OtakuPullToRefreshBox
import com.programmersbox.uiviews.utils.BatteryInformation
import com.programmersbox.uiviews.utils.ChapterModelDeserializer
import com.programmersbox.uiviews.utils.ChapterModelSerializer
import com.programmersbox.uiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.uiviews.utils.HideSystemBarsWhileOnScreen
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

class ReadViewModel(
    activity: Activity?,
    handle: SavedStateHandle,
    genericInfo: GenericInfo,
    model: Flow<List<String>>? = handle.get<String>("currentChapter")
        ?.fromJson<ChapterModel>(ChapterModel::class.java to ChapterModelDeserializer())
        ?.getChapterInfo()
        ?.map { it.mapNotNull(Storage::link) },
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

    private val dao by lazy { ItemDatabase.getInstance(DatabaseBuilder(activity!!)).itemDao() }

    val list by lazy { ChapterList(activity!!, genericInfo).get().orEmpty() }

    private val novelUrl by lazy { handle.get<String>("novelInfoUrl") ?: "" }
    val title by lazy { handle.get<String>("novelTitle") ?: "" }

    var currentChapter: Int by mutableIntStateOf(0)

    var batteryColor by mutableStateOf(androidx.compose.ui.graphics.Color.White)
    var batteryIcon by mutableStateOf(BatteryInformation.BatteryViewType.UNKNOWN)
    var batteryPercent by mutableFloatStateOf(0f)

    val batteryInformation by lazy { BatteryInformation(activity!!) }

    val pageList = mutableStateOf("")
    val isLoadingPages = mutableStateOf(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            batteryInformation.composeSetupFlow(
                androidx.compose.ui.graphics.Color.White
            ) {
                batteryColor = it.first
                batteryIcon = it.second
            }.collect()
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
    ExperimentalAnimationApi::class
)
@Composable
fun NovelReader(
    activity: Activity? = LocalActivity.current,
    genericInfo: GenericInfo = LocalGenericInfo.current,
    readVm: ReadViewModel = viewModel { ReadViewModel(activity, createSavedStateHandle(), genericInfo) },
) {
    HideSystemBarsWhileOnScreen()
    HideNavBarWhileOnScreen()

    val context = LocalContext.current

    DisposableEffect(context) {
        val batteryInfo = context.battery {
            readVm.batteryPercent = it.percent
            readVm.batteryInformation.batteryLevel.tryEmit(it.percent)
            readVm.batteryInformation.batteryInfo.tryEmit(it)
        }
        onDispose { context.unregisterReceiver(batteryInfo) }
    }

    val scope = rememberCoroutineScope()

    val settingsHandling = LocalSettingsHandling.current

    var showInfo by remember { mutableStateOf(true) }

    var settingsPopup by remember { mutableStateOf(false) }
    val batteryPercent = settingsHandling.batteryPercent
    val batteryValue by batteryPercent.rememberPreference()

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
                    modifier = Modifier.padding(4.dp)
                ) {
                    SliderSetting(
                        scope = scope,
                        settingIcon = Icons.Default.BatteryAlert,
                        settingTitle = R.string.battery_alert_percentage,
                        settingSummary = R.string.battery_default,
                        preferenceUpdate = { batteryPercent.set(it) },
                        initialValue = remember { batteryValue },
                        range = 1f..100f,
                        steps = 0
                    )
                }
            },
            confirmButton = { TextButton(onClick = { settingsPopup = false }) { Text(stringResource(R.string.ok)) } }
        )
    }

    fun showToast() {
        activity?.runOnUiThread { Toast.makeText(context, R.string.addedChapterItem, Toast.LENGTH_SHORT).show() }
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
            ModalDrawerSheet {
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        InsetSmallTopAppBar(
                            title = { Text(readVm.title) },
                            actions = { PageIndicator(readVm.list.size - readVm.currentChapter, readVm.list.size) },
                            scrollBehavior = scrollBehavior
                        )
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
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        animateColorAsState(
                                            if (readVm.currentChapter == i) M3MaterialTheme.colorScheme.onSurface
                                            else M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), label = ""
                                        ).value
                                    ),
                                    shape = M3MaterialTheme.shapes.medium,
                                    tonalElevation = 4.dp
                                ) {
                                    ListItem(
                                        headlineContent = { Text(c.name) },
                                        leadingContent = if (readVm.currentChapter == i) {
                                            { Icon(Icons.AutoMirrored.Filled.ArrowRight, null) }
                                        } else null,
                                        modifier = Modifier.clickable { showChangeChapter = true }
                                    )
                                }
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
                    readVm = readVm,
                    showToast = ::showToast,
                    settingsPopup = { settingsPopup = it }
                )
            }
        ) { p ->
            OtakuPullToRefreshBox(
                isRefreshing = readVm.isLoadingPages.value,
                onRefresh = {
                    readVm.loadPages(
                        readVm.list.getOrNull(readVm.currentChapter)
                            ?.getChapterInfo()
                            ?.map { it.mapNotNull(Storage::link) }
                    )
                },
                modifier = Modifier.padding(p)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                ) {
                    Text(
                        AnnotatedString.fromHtml(readVm.pageList.value),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable(
                                indication = null,
                                interactionSource = null,
                                onClick = { showInfo = !showInfo }
                            ),
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
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TopBar(
    showItems: Boolean,
    contentScrollBehavior: TopAppBarScrollBehavior,
    readVm: ReadViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = showItems,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        CenterAlignedTopAppBar(
            scrollBehavior = contentScrollBehavior,
            windowInsets = WindowInsets(0.dp),
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
                            else readVm.batteryColor, label = ""
                        ).value
                    )
                    AnimatedContent(
                        targetState = readVm.batteryPercent.toInt(),
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                            } else {
                                slideInVertically { height -> -height } + fadeIn() togetherWith
                                        slideOutVertically { height -> height } + fadeOut()
                            }
                                .using(SizeTransform(clip = false))
                        }, label = ""
                    ) { targetBattery ->
                        Text(
                            "$targetBattery%",
                            style = M3MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            title = {
                var time by remember { mutableLongStateOf(System.currentTimeMillis()) }

                DisposableEffect(context) {
                    val timeReceiver = context.timeTick { _, _ -> time = System.currentTimeMillis() }
                    onDispose { context.unregisterReceiver(timeReceiver) }
                }

                AnimatedContent(
                    targetState = time,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut())
                            .using(SizeTransform(clip = false))
                    }, label = ""
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
    showItems: Boolean,
    readVm: ReadViewModel,
    showToast: () -> Unit,
    settingsPopup: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = showItems,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut()
    ) {
        androidx.compose.material3.BottomAppBar(
            modifier = modifier,
            windowInsets = WindowInsets(0.dp),
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
                            }, label = ""
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
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                } else {
                    slideInVertically { height -> -height } + fadeIn() togetherWith
                            slideOutVertically { height -> height } + fadeOut()
                }
                    .using(SizeTransform(clip = false))
            }, label = ""
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
    preferenceUpdate: suspend (Int) -> Unit,
    initialValue: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
) {
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
            value,
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

        var sliderValue by remember { mutableFloatStateOf(initialValue.toFloat()) }

        androidx.compose.material3.Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                scope.launch { preferenceUpdate(sliderValue.toInt()) }
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