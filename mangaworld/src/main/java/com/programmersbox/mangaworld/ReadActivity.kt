package com.programmersbox.mangaworld

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.github.piasy.biv.BigImageViewer
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.helpfulutils.*
import com.programmersbox.mangaworld.databinding.ActivityReadBinding
import com.programmersbox.mangaworld.databinding.ReaderSettingsDialogBinding
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.Storage
import com.programmersbox.rxutils.invoke
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.io.File
import kotlin.math.roundToInt

class ReadActivityCompose : ComponentActivity() {

    private val genericInfo by inject<GenericInfo>()
    private val disposable = CompositeDisposable()
    private val dao by lazy { ItemDatabase.getInstance(this).itemDao() }

    private val list by lazy {
        intent.getStringExtra("allChapters")
            ?.fromJson<List<ChapterModel>>(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))
            .orEmpty().also(::println)
    }

    private val mangaUrl by lazy { intent.getStringExtra("mangaInfoUrl") ?: "" }

    private var currentChapter: Int by mutableStateOf(0)

    private val model by lazy {
        intent.getStringExtra("currentChapter")
            ?.fromJson<ChapterModel>(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))
            ?.getChapterInfo()
            ?.map { it.mapNotNull(Storage::link) }
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.doOnError { Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show() }
    }

    private val isDownloaded by lazy { intent.getBooleanExtra("downloaded", false) }
    private val filePath by lazy { intent.getSerializableExtra("filePath") as? File }

    private val title by lazy { intent.getStringExtra("mangaTitle") ?: "" }

    private var batteryInfo: BroadcastReceiver? = null
    private var timeInfo: BroadcastReceiver? = null

    private val batteryInformation by lazy { BatteryInformation(this) }

    private var batteryColor by mutableStateOf(androidx.compose.ui.graphics.Color.White)
    private var batteryIcon by mutableStateOf(BatteryInformation.BatteryViewType.UNKNOWN)
    private var batteryPercent by mutableStateOf(0f)

    private var time by mutableStateOf(System.currentTimeMillis())

    private val pageList = mutableStateListOf<String>()

    private val ad by lazy { AdRequest.Builder().build() }

    @ExperimentalMaterialApi
    @ExperimentalComposeUiApi
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalPagerApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("mangaUrl") ?: ""
        currentChapter = list.indexOfFirst { l -> l.url == url }

        batteryInformation.composeSetup(
            disposable,
            androidx.compose.ui.graphics.Color.White
        ) {
            batteryColor = it.first
            batteryIcon = it.second
        }

        batteryInfo = battery {
            batteryPercent = it.percent
            batteryInformation.batteryLevelAlert(it.percent)
            batteryInformation.batteryInfoItem(it)
        }

        timeInfo = timeTick { _, _ -> time = System.currentTimeMillis() }

        enableImmersiveMode()

        loadPages(model)

        setContent {
            MdcTheme {

                val scope = rememberCoroutineScope()
                val swipeState = rememberSwipeRefreshState(isRefreshing = false)

                val pages = pageList

                BigImageViewer.prefetch(*pages.map(Uri::parse).toTypedArray())

                val listState = rememberLazyListState()
                val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex } }
                var showInfo by remember { mutableStateOf(false) }

                val paddingPage by pagePadding.collectAsState(initial = 4)
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
                                    initialValue = runBlocking { dataStore.data.first()[BATTERY_PERCENT] ?: 20 },
                                    range = 1f..100f,
                                    steps = 0
                                )
                                Divider()
                                SliderSetting(
                                    scope = scope,
                                    settingIcon = Icons.Default.FormatLineSpacing,
                                    settingTitle = R.string.reader_padding_between_pages,
                                    settingSummary = R.string.default_padding_summary,
                                    preference = PAGE_PADDING,
                                    initialValue = runBlocking { dataStore.data.first()[PAGE_PADDING] ?: 4 },
                                    range = 0f..10f,
                                    steps = 0
                                )
                            }
                        },
                        confirmButton = { TextButton(onClick = { settingsPopup = false }) { Text(stringResource(R.string.ok)) } }
                    )
                }

                fun showToast() {
                    runOnUiThread { Toast.makeText(this, R.string.addedChapterItem, Toast.LENGTH_SHORT).show() }
                }

                val showItems = showInfo || listState.isScrolledToTheEnd()

                //56 is default FabSize
                //56 is the bottom app bar size
                //16 is the scaffold padding
                val fabHeight = 72.dp
                val fabHeightPx = with(LocalDensity.current) { fabHeight.roundToPx().toFloat() }
                val fabOffsetHeightPx = remember { mutableStateOf(0f) }

                val animateFab by animateIntAsState(if (showItems) 0 else (-fabOffsetHeightPx.value.roundToInt()))

                val scaffoldState = rememberBottomSheetScaffoldState()

                BackHandler(scaffoldState.bottomSheetState.isExpanded || scaffoldState.drawerState.isOpen) {
                    scope.launch {
                        when {
                            scaffoldState.bottomSheetState.isExpanded -> scaffoldState.bottomSheetState.collapse()
                            scaffoldState.drawerState.isOpen -> scaffoldState.drawerState.close()
                        }
                    }
                }

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetContent = {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(list.getOrNull(currentChapter)?.name.orEmpty()) },
                                    actions = { Text("${currentPage + 1}/${pages.size}") },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { scaffoldState.bottomSheetState.collapse() } }) {
                                            Icon(Icons.Default.Close, null)
                                        }
                                    }
                                )
                            }
                        ) { p ->
                            if (scaffoldState.bottomSheetState.isExpanded) {
                                LazyVerticalGrid(
                                    cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
                                    //verticalArrangement = Arrangement.spacedBy(4.dp),
                                    //horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = p
                                ) {
                                    itemsIndexed(pages) { i, it ->
                                        Box {
                                            GlideImage(
                                                imageModel = it,
                                                contentScale = ContentScale.Crop,
                                                loading = { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.Center)
                                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                                    .border(
                                                        animateDpAsState(if (currentPage == i) 5.dp else 0.dp).value,
                                                        color = animateColorAsState(
                                                            if (currentPage == i) MaterialTheme.colors.primaryVariant
                                                            else androidx.compose.ui.graphics.Color.Transparent
                                                        ).value
                                                    )
                                                    .clickable {
                                                        scope.launch {
                                                            if (currentPage == i) scaffoldState.bottomSheetState.collapse()
                                                            listState.animateScrollToItem(i)
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    sheetGesturesEnabled = false,
                    sheetPeekHeight = 0.dp,
                    drawerContent = if (list.size > 1) {
                        {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text(title) },
                                        actions = { Text("${list.size - currentChapter}/${list.size}") }
                                    )
                                }
                            ) { p ->
                                if (scaffoldState.drawerState.isOpen) {
                                    LazyColumn(
                                        state = rememberLazyListState(currentChapter.coerceIn(0, list.lastIndex)),
                                        contentPadding = p,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        itemsIndexed(list) { i, c ->

                                            var showChangeChapter by remember { mutableStateOf(false) }

                                            if (showChangeChapter) {
                                                AlertDialog(
                                                    onDismissRequest = { showChangeChapter = false },
                                                    title = { Text(stringResource(R.string.changeToChapter, c.name)) },
                                                    confirmButton = {
                                                        TextButton(
                                                            onClick = {
                                                                showChangeChapter = false
                                                                currentChapter = i
                                                                loadPages(
                                                                    list.getOrNull(currentChapter)
                                                                        ?.getChapterInfo()
                                                                        ?.map { it.mapNotNull(Storage::link) }
                                                                        ?.subscribeOn(Schedulers.io())
                                                                        ?.observeOn(AndroidSchedulers.mainThread()),
                                                                    swipeState
                                                                )
                                                            }
                                                        ) { Text(stringResource(R.string.yes)) }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { showChangeChapter = false }) { Text(stringResource(R.string.no)) }
                                                    }
                                                )
                                            }

                                            Card(
                                                modifier = Modifier.padding(horizontal = 5.dp),
                                                border = BorderStroke(
                                                    1.dp,
                                                    animateColorAsState(
                                                        if (currentChapter == i) MaterialTheme.colors.onSurface
                                                        else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                                    ).value
                                                )
                                            ) {
                                                ListItem(
                                                    text = { Text(c.name) },
                                                    icon = if (currentChapter == i) {
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
                    } else null
                ) {

                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                                modifier = Modifier
                                    .padding(bottom = 56.dp)
                                    .offset { IntOffset(x = animateFab, y = 0) }
                            ) { Icon(Icons.Default.VerticalAlignTop, null) }
                        },
                        floatingActionButtonPosition = FabPosition.End
                    ) { p ->
                        //TODO: If/when swipe refresh gains a swipe up to refresh, make the swipe up go to the next chapter
                        SwipeRefresh(
                            state = swipeState,
                            onRefresh = {
                                loadPages(
                                    list.getOrNull(currentChapter)
                                        ?.getChapterInfo()
                                        ?.map { it.mapNotNull(Storage::link) }
                                        ?.subscribeOn(Schedulers.io())
                                        ?.observeOn(AndroidSchedulers.mainThread()),
                                    swipeState
                                )
                            },
                            modifier = Modifier.padding(p)
                        ) {

                            val topBarHeight = 28.dp
                            val topBarHeightPx = with(LocalDensity.current) { topBarHeight.roundToPx().toFloat() }
                            val topBarOffsetHeightPx = remember { mutableStateOf(0f) }

                            val toolbarHeight = 56.dp
                            val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.roundToPx().toFloat() }
                            val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }
                            val nestedScrollConnection = remember {
                                object : NestedScrollConnection {
                                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                        val delta = available.y

                                        val newFabOffset = fabOffsetHeightPx.value + delta
                                        fabOffsetHeightPx.value = newFabOffset.coerceIn(-fabHeightPx, 0f)

                                        val newOffset = toolbarOffsetHeightPx.value + delta
                                        toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)

                                        val newTopOffset = topBarOffsetHeightPx.value + delta
                                        topBarOffsetHeightPx.value = newTopOffset.coerceIn(-topBarHeightPx, 0f)
                                        return Offset.Zero
                                    }
                                }
                            }

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .nestedScroll(nestedScrollConnection)
                            ) {
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(dpToPx(paddingPage).dp),
                                    contentPadding = PaddingValues(
                                        top = topBarHeight,
                                        bottom = toolbarHeight
                                    )
                                ) {

                                    items(pages) {
                                        Box {
                                            GlideImage(
                                                imageModel = it,
                                                contentScale = ContentScale.FillWidth,
                                                loading = { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
                                                    .align(Alignment.Center)
                                                    .scaleRotateOffset(
                                                        canRotate = false,
                                                        onClick = {
                                                            showInfo = !showInfo
                                                            if (!showInfo) {
                                                                toolbarOffsetHeightPx.value = -toolbarHeightPx
                                                                topBarOffsetHeightPx.value = -topBarHeightPx
                                                                fabOffsetHeightPx.value = -fabHeightPx
                                                            }
                                                        },
                                                        onLongClick = { scope.launch { scaffoldState.bottomSheetState.expand() } }
                                                    )
                                            )
                                        }
                                    }

                                    item {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (currentChapter <= 0) {
                                                Text(
                                                    stringResource(id = R.string.reachedLastChapter),
                                                    style = MaterialTheme.typography.h6,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.CenterHorizontally)
                                                )
                                            }
                                            GoBackButton(modifier = Modifier.fillMaxWidth())
                                            AndroidView(
                                                modifier = Modifier.fillMaxWidth(),
                                                factory = {
                                                    AdView(it).apply {
                                                        adSize = AdSize.BANNER
                                                        adUnitId = getString(R.string.ad_unit_id)
                                                        loadAd(ad)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                val animateTopBar by animateIntAsState(if (showItems) 0 else (topBarOffsetHeightPx.value.roundToInt()))

                                TopAppBar(
                                    modifier = Modifier
                                        .height(topBarHeight)
                                        .align(Alignment.TopCenter)
                                        .alpha(1f - (-animateTopBar / topBarHeightPx))
                                        .offset { IntOffset(x = 0, y = animateTopBar) }
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                    ) {
                                        Row {
                                            Icon(
                                                batteryIcon.composeIcon,
                                                contentDescription = null,
                                                tint = animateColorAsState(batteryColor).value
                                            )
                                            Text("${batteryPercent.toInt()}%", style = MaterialTheme.typography.body1)
                                        }

                                        Text(
                                            DateFormat.format("HH:mm a", time).toString(),
                                            style = MaterialTheme.typography.body1
                                        )

                                        Text(
                                            "${currentPage + 1}/${pages.size}",
                                            style = MaterialTheme.typography.body1
                                        )
                                    }
                                }

                                val animateBar by animateIntAsState(if (showItems) 0 else (-toolbarOffsetHeightPx.value.roundToInt()))

                                BottomAppBar(
                                    modifier = Modifier
                                        .height(toolbarHeight)
                                        .align(Alignment.BottomCenter)
                                        .alpha(1f - (animateBar / toolbarHeightPx))
                                        .offset { IntOffset(x = 0, y = animateBar) }
                                ) {

                                    val prevShown = currentChapter < list.lastIndex
                                    val nextShown = currentChapter > 0

                                    AnimatedVisibility(
                                        visible = prevShown && list.size > 1,
                                        enter = expandHorizontally(expandFrom = Alignment.Start),
                                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
                                    ) {
                                        PreviousButton(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .weight(
                                                    when {
                                                        prevShown && nextShown -> 8f / 3f
                                                        prevShown -> 4f
                                                        else -> 4f
                                                    }
                                                ),
                                            previousChapter = ::showToast
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
                                        visible = nextShown && list.size > 1,
                                        enter = expandHorizontally(),
                                        exit = shrinkHorizontally()
                                    ) {
                                        NextButton(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .weight(
                                                    when {
                                                        prevShown && nextShown -> 8f / 3f
                                                        nextShown -> 4f
                                                        else -> 4f
                                                    }
                                                ),
                                            nextChapter = ::showToast
                                        )
                                    }
                                    //The three buttons above will equal 8f
                                    //So these two need to add up to 2f
                                    IconButton(
                                        onClick = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                                        modifier = Modifier.weight(1f)
                                    ) { Icon(Icons.Default.GridOn, null) }

                                    IconButton(
                                        onClick = { settingsPopup = true },
                                        modifier = Modifier.weight(1f)
                                    ) { Icon(Icons.Default.Settings, null) }
                                }

                                if (pages.isEmpty()) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(p)
                                            .fillMaxSize()
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    private fun loadPages(modelPath: Single<List<String>>?, swipeRefreshState: SwipeRefreshState? = null) {
        if (isDownloaded && filePath != null) {
            Single.create<List<String>> {
                filePath
                    ?.listFiles()
                    ?.sortedBy { f -> f.name.split(".").first().toInt() }
                    ?.fastMap(File::toUri)
                    ?.fastMap(Uri::toString)
                    ?.let(it::onSuccess) ?: it.onError(Throwable("Cannot find files"))
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        } else {
            modelPath
        }
            ?.doOnSubscribe { pageList.clear() }
            ?.subscribeBy {
                pageList.addAll(it)
                swipeRefreshState?.isRefreshing = false
            }
            ?.addTo(disposable)
    }

    private fun Context.dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun LazyListState.isScrolledToTheEnd() = layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1

    @Composable
    private fun GoBackButton(modifier: Modifier = Modifier) {
        OutlinedButton(
            onClick = { finish() },
            modifier = modifier,
            border = BorderStroke(ButtonDefaults.OutlinedBorderSize, MaterialTheme.colors.primary)
        ) { Text(stringResource(id = R.string.goBack), style = MaterialTheme.typography.button, color = MaterialTheme.colors.primary) }
    }

    private fun addChapterToWatched(chapterNum: Int, chapter: () -> Unit) {
        list.getOrNull(chapterNum)?.let { item ->
            ChapterWatched(item.url, item.name, mangaUrl)
                .let {
                    Completable.mergeArray(
                        FirebaseDb.insertEpisodeWatched(it),
                        dao.insertChapter(it)
                    )
                }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(chapter)
                .addTo(disposable)

            item
                .getChapterInfo()
                .map { it.mapNotNull(Storage::link) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { pageList.clear() }
                .subscribeBy { pages: List<String> -> pageList.addAll(pages) }
                .addTo(disposable)
        }
    }

    @Composable
    private fun NextButton(modifier: Modifier = Modifier, nextChapter: () -> Unit) {
        Button(
            onClick = { addChapterToWatched(--currentChapter, nextChapter) },
            modifier = modifier,
            border = BorderStroke(ButtonDefaults.OutlinedBorderSize, MaterialTheme.colors.primary)
        ) { Text(stringResource(id = R.string.loadNextChapter), style = MaterialTheme.typography.button) }
    }

    @Composable
    private fun PreviousButton(modifier: Modifier = Modifier, previousChapter: () -> Unit) {
        TextButton(
            onClick = { addChapterToWatched(++currentChapter, previousChapter) },
            modifier = modifier
        ) { Text(stringResource(id = R.string.loadPreviousChapter), style = MaterialTheme.typography.button) }
    }

    @Composable
    fun SliderSetting(
        scope: CoroutineScope,
        settingIcon: ImageVector,
        @StringRes settingTitle: Int,
        @StringRes settingSummary: Int,
        preference: Preferences.Key<Int>,
        initialValue: Int,
        range: ClosedFloatingPointRange<Float>,
        steps: Int = 0
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
                style = MaterialTheme.typography.body1,
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
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Start,
                modifier = Modifier.constrainAs(summary) {
                    top.linkTo(title.bottom)
                    end.linkTo(parent.end)
                    start.linkTo(icon.end, margin = 10.dp)
                    width = Dimension.fillToConstraints
                }
            )

            var sliderValue by remember { mutableStateOf(initialValue.toFloat()) }

            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    scope.launch { updatePref(preference, sliderValue.toInt()) }
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
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.constrainAs(value) {
                    end.linkTo(parent.end)
                    start.linkTo(slider.end)
                    centerVerticallyTo(slider)
                }
            )

        }
    }

    @ExperimentalFoundationApi
    @Composable
    private fun Modifier.scaleRotateOffset(
        canScale: Boolean = true,
        canRotate: Boolean = true,
        canOffset: Boolean = true,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {}
    ): Modifier {
        var scale by remember { mutableStateOf(1f) }
        var rotation by remember { mutableStateOf(0f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            if (canScale) scale *= zoomChange
            if (canRotate) rotation += rotationChange
            if (canOffset) offset += offsetChange
        }
        return graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            rotationZ = rotation,
            translationX = offset.x,
            translationY = offset.y
        )
            // add transformable to listen to multitouch transformation events
            // after offset
            .transformable(state = state)
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = {
                    ValueAnimator
                        .ofFloat(scale, 1f)
                        .apply { addUpdateListener { scale = it.animatedValue as Float } }
                        .start()
                    //scale = 1f
                    ValueAnimator
                        .ofFloat(rotation, 0f)
                        .apply { addUpdateListener { rotation = it.animatedValue as Float } }
                        .start()
                    //rotation = 0f

                    offset = Offset.Zero
                },
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryInfo)
        unregisterReceiver(timeInfo)
        disposable.dispose()
    }
}

class ReadActivity : AppCompatActivity() {
    private val disposable = CompositeDisposable()
    private var model: ChapterModel? = null
    private var mangaTitle: String? = null
    private var isDownloaded = false
    private val loader by lazy { Glide.with(this) }
    /*private val adapter by lazy {
        loader.let {
            PageAdapter(
                fullRequest = it
                    .asDrawable()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop(),
                thumbRequest = it
                    .asDrawable()
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .transition(withCrossFade()),
                context = this@ReadActivity,
                dataList = mutableListOf()
            ) { image ->
                requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) { p ->
                    if (p.isGranted) saveImage("${mangaTitle}_${model?.name}_${image.toUri().lastPathSegment}", image)
                }
            }
        }
    }*/


    private val genericInfo by inject<GenericInfo>()

    private fun View.slideUp() {
        val layoutParams = this.layoutParams
        if (layoutParams is CoordinatorLayout.LayoutParams) {
            @Suppress("UNCHECKED_CAST")
            (layoutParams.behavior as? CustomHideBottomViewOnScrollBehavior<View>)?.slideUp(this)
        }
    }

    private fun View.slideDown() {
        val layoutParams = this.layoutParams
        if (layoutParams is CoordinatorLayout.LayoutParams) {
            @Suppress("UNCHECKED_CAST")
            (layoutParams.behavior as? CustomHideBottomViewOnScrollBehavior<View>)?.slideDown(this)
        }
    }

    private val sliderMenu by lazy {
        val layoutParams = binding.bottomMenu.layoutParams
        if (layoutParams is CoordinatorLayout.LayoutParams) {
            @Suppress("UNCHECKED_CAST")
            layoutParams.behavior as? CustomHideBottomViewOnScrollBehavior<RelativeLayout>
        } else null
    }

    private val fab by lazy {
        val layoutParams = binding.scrollToTopManga.layoutParams
        if (layoutParams is CoordinatorLayout.LayoutParams) {
            @Suppress("UNCHECKED_CAST")
            layoutParams.behavior as? CustomHideBottomViewOnScrollBehavior<FloatingActionButton>
        } else null
    }

    private var menuToggle = false

    private val adapter2: PageAdapter by lazy {
        loader.let {
            val list = intent.getStringExtra("allChapters")
                ?.fromJson<List<ChapterModel>>(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))
                .orEmpty().also(::println)
            //intent.getObjectExtra<List<ChapterModel>>("allChapters") ?: emptyList()
            val url = intent.getStringExtra("mangaUrl") ?: ""
            val mangaUrl = intent.getStringExtra("mangaInfoUrl") ?: ""
            PageAdapter(
                disposable = disposable,
                fullRequest = it
                    .asDrawable()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop(),
                thumbRequest = it
                    .asDrawable()
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .transition(withCrossFade()),
                activity = this,
                dataList = mutableListOf(),
                onTap = {
                    menuToggle = !menuToggle
                    if (sliderMenu?.isShowing?.not() ?: menuToggle) binding.bottomMenu.slideUp() else binding.bottomMenu.slideDown()
                    if (fab?.isShowing?.not() ?: menuToggle) binding.scrollToTopManga.slideUp() else binding.scrollToTopManga.slideDown()
                },
                coordinatorLayout = binding.readLayout,
                chapterModels = list,
                currentChapter = list.indexOfFirst { l -> l.url == url },
                mangaUrl = mangaUrl,
                loadNewPages = this::loadPages
            )
        }
    }

    private var batteryInfo: BroadcastReceiver? = null

    private val batteryInformation by lazy { BatteryInformation(this) }

    private lateinit var binding: ActivityReadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*MobileAds.initialize(this) {}
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(listOf("BCF3E346AED658CDCCB1DDAEE8D84845")).build())*/

        enableImmersiveMode()

        //adViewing.loadAd(AdRequest.Builder().build())

        /*window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                println("Visible")
                titleManga.animate().alpha(1f).withStartAction { titleManga.visible() }.start()
            } else {
                println("Invisible")
                titleManga.animate().alpha(0f).withEndAction { titleManga.invisible() }.start()
            }
        }*/

        infoSetup()
        readerSetup()
    }

    private fun readerSetup() {
        val preloader: RecyclerViewPreloader<String> = RecyclerViewPreloader(loader, adapter2, ViewPreloadSizeProvider(), 10)
        val readView = binding.readView
        readView.addOnScrollListener(preloader)
        readView.setItemViewCacheSize(0)

        readView.adapter = adapter2

        readView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val l = recyclerView.layoutManager as LinearLayoutManager
                val image = l.findLastVisibleItemPosition()
                if (image > -1) {
                    val total = l.itemCount
                    binding.pageCount.text = String.format("%d/%d", image + 1, total)
                    binding.pageChoice.value = (image + 1).toFloat()
                    if (image + 1 == total) sliderMenu?.slideDown(binding.bottomMenu)
                }
            }
        })

        //readView.setRecyclerListener { (it as? PageHolder)?.image?.ssiv?.recycle() }

        /*val models = intent.getObjectExtra<List<ChapterModel>>("allChapters")
        val url = intent.getStringExtra("mangaUrl")
        var currentIndex = models?.indexOfFirst { it.url == url }
        var currentModel = currentIndex?.let { models?.getOrNull(it) }*/

        mangaTitle = intent.getStringExtra("mangaTitle")
        model = intent.getStringExtra("currentChapter")
            ?.fromJson<ChapterModel>(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))

        isDownloaded = intent.getBooleanExtra("downloaded", false)
        val file = intent.getSerializableExtra("filePath") as? File
        if (isDownloaded && file != null) loadFileImages(file)
        else loadPages(model)

        binding.readRefresh.setOnRefreshListener {
            binding.readRefresh.isRefreshing = false
            adapter2.reloadChapter()
        }

        binding.scrollToTopManga.setOnClickListener { binding.readView.smoothScrollToPosition(0) }
        binding.pageChoice.addOnChangeListener { _, value, fromUser ->
            if (fromUser) binding.readView.scrollToPosition(value.toInt() - 1)
        }

        var decor = VerticalSpaceItemDecoration(4)
        binding.readView.addItemDecoration(decor)
        lifecycleScope.launch {
            pagePadding
                .flowWithLifecycle(lifecycle)
                .collect {
                    runOnUiThread {
                        binding.readView.removeItemDecoration(decor)
                        decor = VerticalSpaceItemDecoration(it)
                        binding.readView.addItemDecoration(decor)
                    }
                }
        }

        binding.readerSettings.setOnClickListener {

            val readerBinding = ReaderSettingsDialogBinding.inflate(layoutInflater)

            val padding = runBlocking { dataStore.data.first()[PAGE_PADDING] ?: 4 }
            readerBinding.pagePaddingSlider.value = padding.toFloat()
            readerBinding.sliderValue.text = padding.toString()
            readerBinding.pagePaddingSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    lifecycleScope.launch(Dispatchers.IO) { updatePref(PAGE_PADDING, value.toInt()) }
                }
                readerBinding.sliderValue.text = value.toInt().toString()
            }

            val battery = runBlocking { dataStore.data.first()[BATTERY_PERCENT] ?: 20 }
            readerBinding.batterySlider.value = battery.toFloat()
            readerBinding.batterySliderValue.text = battery.toString()
            readerBinding.batterySlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    lifecycleScope.launch(Dispatchers.IO) { updatePref(BATTERY_PERCENT, value.toInt()) }
                }
                readerBinding.batterySliderValue.text = value.toInt().toString()
            }

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings)
                .setView(readerBinding.root)
                .setPositiveButton(R.string.ok) { d, _ -> d.dismiss() }
                .show()
        }
    }

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            if (parent.getChildAdapterPosition(view) != parent.adapter!!.itemCount - 1) {
                outRect.bottom = view.context.dpToPx(verticalSpaceHeight)
                outRect.top = view.context.dpToPx(verticalSpaceHeight)
            }
        }

        private fun Context.dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    }

    private fun loadFileImages(file: File) {
        println(file.absolutePath)
        Single.create<List<String>> {
            file.listFiles()
                ?.sortedBy { f -> f.name.split(".").first().toInt() }
                ?.fastMap(File::toUri)
                ?.fastMap(Uri::toString)
                ?.let(it::onSuccess) ?: it.onError(Throwable("Cannot find files"))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                BigImageViewer.prefetch(*it.fastMap(Uri::parse).toTypedArray())
                binding.readLoading
                    .animate()
                    .alpha(0f)
                    .withEndAction { binding.readLoading.gone() }
                    .start()
                adapter2.setListNotify(it)

                binding.pageChoice.valueTo = try {
                    val f = it.size.toFloat() + 1
                    if (f == 1f) 2f else f
                } catch (e: Exception) {
                    2f
                }
            }
            .addTo(disposable)
    }

    private fun loadPages(model: ChapterModel?) {
        Glide.get(this).clearMemory()
        binding.readLoading
            .animate()
            .withStartAction { binding.readLoading.visible() }
            .alpha(1f)
            .start()
        adapter2.setListNotify(emptyList())
        model?.getChapterInfo()
            ?.map { it.mapNotNull(Storage::link) }
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.doOnError { Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show() }
            ?.subscribeBy { pages: List<String> ->
                BigImageViewer.prefetch(*pages.fastMap(Uri::parse).toTypedArray())
                binding.readLoading
                    .animate()
                    .alpha(0f)
                    .withEndAction { binding.readLoading.gone() }
                    .start()
                adapter2.setListNotify(pages)
                binding.pageChoice.valueTo = try {
                    pages.size.toFloat() + 1
                } catch (e: Exception) {
                    2f
                }
                //adapter.addItems(pages)
                //binding.readView.layoutManager!!.scrollToPosition(model.url.let { defaultSharedPref.getInt(it, 0) })
            }
            ?.addTo(disposable)
    }

    private fun infoSetup() {
        batterySetup()
    }

    @SuppressLint("SetTextI18n")
    private fun batterySetup() {
        val normalBatteryColor = colorFromTheme(R.attr.colorOnBackground, Color.WHITE)

        binding.batteryInformation.startDrawable = IconicsDrawable(this, GoogleMaterial.Icon.gmd_battery_std).apply {
            colorInt = normalBatteryColor
            sizePx = binding.batteryInformation.textSize.roundToInt()
        }

        batteryInformation.setup(
            disposable = disposable,
            size = binding.batteryInformation.textSize.roundToInt(),
            normalBatteryColor = normalBatteryColor
        ) {
            it.second.colorInt = it.first
            binding.batteryInformation.startDrawable = it.second
            binding.batteryInformation.setTextColor(it.first)
            binding.batteryInformation.startDrawable?.setTint(it.first)
        }

        batteryInfo = battery {
            binding.batteryInformation.text = "${it.percent.toInt()}%"
            batteryInformation.batteryLevelAlert(it.percent)
            batteryInformation.batteryInfoItem(it)
        }
    }

    private fun saveCurrentChapterSpot() {
        /*model?.let {
            defaultSharedPref.edit().apply {
                val currentItem = (readView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                if (currentItem >= adapter.dataList.size - 2) remove(it.url)
                else putInt(it.url, currentItem)
            }.apply()
        }*/
    }

    override fun onPause() {
        //saveCurrentChapterSpot()
        //adViewing.pause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Glide.get(this).clearMemory()
        unregisterReceiver(batteryInfo)
        disposable.dispose()
    }

}