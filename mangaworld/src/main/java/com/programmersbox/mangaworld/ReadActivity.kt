package com.programmersbox.mangaworld

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
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.github.piasy.biv.BigImageViewer
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.*
import com.programmersbox.mangaworld.databinding.ActivityReadBinding
import com.programmersbox.mangaworld.databinding.ReaderSettingsDialogBinding
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.Storage
import com.programmersbox.rxutils.invoke
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.BaseMainActivity
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.io.File
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.OutlinedButton as M3OutlinedButton

class ReadViewModel(
    handle: SavedStateHandle,
    context: Context,
    val genericInfo: GenericInfo,
    val headers: MutableMap<String, String> = mutableMapOf<String, String>(),
    model: Single<List<String>>? = handle
        .get<String>("currentChapter")
        ?.fromJson<ChapterModel>(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))
        ?.getChapterInfo()
        ?.map {
            headers.putAll(it.flatMap { it.headers.toList() })
            it.mapNotNull(Storage::link)
        }
        ?.subscribeOn(Schedulers.io())
        ?.observeOn(AndroidSchedulers.mainThread())
        ?.doOnError { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show() },
    isDownloaded: Boolean = handle.get<String>("downloaded")?.toBooleanStrict() ?: false,
    filePath: File? = handle.get<String>("filePath")?.let { File(it) },
    modelPath: Single<List<String>>? = if (isDownloaded && filePath != null) {
        Single.create<List<String>> {
            filePath
                .listFiles()
                ?.sortedBy { f -> f.name.split(".").first().toInt() }
                ?.fastMap(File::toUri)
                ?.fastMap(Uri::toString)
                ?.let(it::onSuccess) ?: it.onError(Throwable("Cannot find files"))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    } else {
        model
    },
) : ViewModel() {

    companion object {
        const val MangaReaderRoute =
            "mangareader?currentChapter={currentChapter}&allChapters={allChapters}&mangaTitle={mangaTitle}&mangaUrl={mangaUrl}&mangaInfoUrl={mangaInfoUrl}&downloaded={downloaded}&filePath={filePath}"

        fun navigateToMangaReader(
            navController: NavController,
            currentChapter: ChapterModel? = null,
            allChapters: List<ChapterModel>? = null,
            mangaTitle: String? = null,
            mangaUrl: String? = null,
            mangaInfoUrl: String? = null,
            downloaded: Boolean = false,
            filePath: String? = null
        ) {
            val current = Uri.encode(currentChapter?.toJson(ChapterModel::class.java to ChapterModelSerializer()))
            val all = Uri.encode(allChapters?.toJson(ChapterModel::class.java to ChapterModelSerializer()))

            navController.navigate(
                "mangareader?currentChapter=$current&allChapters=$all&mangaTitle=${mangaTitle}&mangaUrl=${mangaUrl}&mangaInfoUrl=${mangaInfoUrl}&downloaded=$downloaded&filePath=$filePath"
            )
        }
    }

    val title by lazy { handle.get<String>("mangaTitle") ?: "" }

    val ad: AdRequest by lazy { AdRequest.Builder().build() }

    val dao by lazy { ItemDatabase.getInstance(context).itemDao() }

    val disposable = CompositeDisposable()

    val list by lazy {
        handle.get<String>("allChapters")
            ?.fromJson<List<ChapterModel>>(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))
            .orEmpty()
            .also(::println)
    }

    private val mangaUrl by lazy { handle.get<String>("mangaInfoUrl") ?: "" }

    var currentChapter: Int by mutableStateOf(0)

    var batteryColor by mutableStateOf(androidx.compose.ui.graphics.Color.White)
    var batteryIcon by mutableStateOf(BatteryInformation.BatteryViewType.UNKNOWN)
    var batteryPercent by mutableStateOf(0f)

    val batteryInformation by lazy { BatteryInformation(context) }

    val pageList = mutableStateListOf<String>()
    var isLoadingPages = mutableStateOf(false)
        private set

    init {
        batteryInformation.composeSetup(
            disposable,
            androidx.compose.ui.graphics.Color.White
        ) {
            batteryColor = it.first
            batteryIcon = it.second
        }

        val url = handle.get<String>("mangaUrl") ?: ""
        currentChapter = list.indexOfFirst { l -> l.url == url }

        loadPages(modelPath)
    }

    var showInfo by mutableStateOf(false)

    fun addChapterToWatched(newChapter: Int, chapter: () -> Unit) {
        currentChapter = newChapter
        list.getOrNull(newChapter)?.let { item ->
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

    private fun loadPages(modelPath: Single<List<String>>?) {
        modelPath
            ?.doOnSubscribe {
                isLoadingPages.value = true
                pageList.clear()
            }
            ?.subscribeBy {
                pageList.addAll(it)
                isLoadingPages.value = false
            }
            ?.addTo(disposable)
    }

    fun refresh() {
        headers.clear()
        loadPages(
            list.getOrNull(currentChapter)
                ?.getChapterInfo()
                ?.map {
                    headers.putAll(it.flatMap { it.headers.toList() })
                    it.mapNotNull(Storage::link)
                }
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }

}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun ReadView() {

    LifecycleHandle(
        onStop = { BaseMainActivity.showNavBar = true },
        onDestroy = { BaseMainActivity.showNavBar = true },
        onCreate = { BaseMainActivity.showNavBar = false },
        onStart = { BaseMainActivity.showNavBar = false },
        onResume = { BaseMainActivity.showNavBar = false }
    )

    val context = LocalContext.current
    val genericInfo = LocalGenericInfo.current

    val readVm: ReadViewModel = viewModel {
        ReadViewModel(
            handle = createSavedStateHandle(),
            context = context,
            genericInfo = genericInfo
        )
    }

    DisposableEffect(LocalContext.current) {
        val batteryInfo = context.battery {
            readVm.batteryPercent = it.percent
            readVm.batteryInformation.batteryLevelAlert(it.percent)
            readVm.batteryInformation.batteryInfoItem(it)
        }
        onDispose { context.unregisterReceiver(batteryInfo) }
    }

    val scope = rememberCoroutineScope()
    val swipeState = rememberSwipeRefreshState(isRefreshing = readVm.isLoadingPages.value)

    val pages = readVm.pageList

    LaunchedEffect(readVm.pageList) { BigImageViewer.prefetch(*readVm.pageList.fastMap(Uri::parse).toTypedArray()) }

    val listState = rememberLazyListState()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    val paddingPage by context.pagePadding.collectAsState(initial = 4)
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
                Column {
                    SliderSetting(
                        scope = scope,
                        settingIcon = Icons.Default.BatteryAlert,
                        settingTitle = R.string.battery_alert_percentage,
                        settingSummary = R.string.battery_default,
                        preference = BATTERY_PERCENT,
                        initialValue = runBlocking { context.dataStore.data.first()[BATTERY_PERCENT] ?: 20 },
                        range = 1f..100f
                    )
                    Divider()
                    SliderSetting(
                        scope = scope,
                        settingIcon = Icons.Default.FormatLineSpacing,
                        settingTitle = R.string.reader_padding_between_pages,
                        settingSummary = R.string.default_padding_summary,
                        preference = PAGE_PADDING,
                        initialValue = runBlocking { context.dataStore.data.first()[PAGE_PADDING] ?: 4 },
                        range = 0f..10f
                    )
                }
            },
            confirmButton = { TextButton(onClick = { settingsPopup = false }) { Text(stringResource(R.string.ok)) } }
        )
    }

    val activity = LocalActivity.current

    fun showToast() {
        activity.runOnUiThread { Toast.makeText(context, R.string.addedChapterItem, Toast.LENGTH_SHORT).show() }
    }

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
            val sheetTopAppBarScrollState = rememberTopAppBarScrollState()
            val sheetScrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(sheetTopAppBarScrollState) }
            Scaffold(
                modifier = Modifier.nestedScroll(sheetScrollBehavior.nestedScrollConnection),
                topBar = {
                    SmallTopAppBar(
                        scrollBehavior = sheetScrollBehavior,
                        title = { Text(readVm.list.getOrNull(readVm.currentChapter)?.name.orEmpty()) },
                        actions = { PageIndicator(Modifier, currentPage + 1, pages.size) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { scaffoldState.bottomSheetState.collapse() } }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    )
                },
                bottomBar = {
                    if (BuildConfig.BUILD_TYPE == "release") {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            factory = {
                                AdView(it).apply {
                                    adSize = AdSize.BANNER
                                    adUnitId = context.getString(R.string.ad_unit_id)
                                    loadAd(readVm.ad)
                                }
                            }
                        )
                    }
                }
            ) { p ->
                if (scaffoldState.bottomSheetState.isExpanded) {
                    LazyVerticalGrid(
                        columns = adaptiveGridCell(),
                        contentPadding = p,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(pages) { i, it ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                    .border(
                                        animateDpAsState(if (currentPage == i) 5.dp else 0.dp).value,
                                        color = animateColorAsState(
                                            if (currentPage == i) M3MaterialTheme.colorScheme.primary
                                            else androidx.compose.ui.graphics.Color.Transparent
                                        ).value
                                    )
                                    .clickable {
                                        scope.launch {
                                            if (currentPage == i) scaffoldState.bottomSheetState.collapse()
                                            listState.animateScrollToItem(i)
                                        }
                                    }
                            ) {
                                GlideImage(
                                    imageModel = it,
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.Center)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    androidx.compose.ui.graphics.Color.Transparent,
                                                    androidx.compose.ui.graphics.Color.Black
                                                ),
                                                startY = 50f
                                            )
                                        )
                                ) {
                                    Text(
                                        (i + 1).toString(),
                                        style = M3MaterialTheme
                                            .typography
                                            .bodyLarge
                                            .copy(textAlign = TextAlign.Center, color = androidx.compose.ui.graphics.Color.White),
                                        maxLines = 2,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        sheetGesturesEnabled = false,
        sheetPeekHeight = 0.dp,
        drawerContent = if (readVm.list.size > 1) {
            {
                val drawerTopAppBarScrollState = rememberTopAppBarScrollState()
                val drawerScrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(drawerTopAppBarScrollState) }
                Scaffold(
                    modifier = Modifier.nestedScroll(drawerScrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            scrollBehavior = drawerScrollBehavior,
                            title = { Text(readVm.title) },
                            actions = { PageIndicator(Modifier, readVm.list.size - readVm.currentChapter, readVm.list.size) }
                        )
                    },
                    bottomBar = {
                        if (BuildConfig.BUILD_TYPE == "release") {
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                factory = {
                                    AdView(it).apply {
                                        adSize = AdSize.BANNER
                                        adUnitId = context.getString(R.string.ad_unit_id)
                                        loadAd(readVm.ad)
                                    }
                                }
                            )
                        }
                    }
                ) { p ->
                    if (scaffoldState.drawerState.isOpen) {
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
                                                    readVm.currentChapter = i
                                                    readVm.addChapterToWatched(readVm.currentChapter, ::showToast)
                                                }
                                            ) { Text(stringResource(R.string.yes)) }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showChangeChapter = false }) { Text(stringResource(R.string.no)) }
                                        }
                                    )
                                }

                                WrapHeightNavigationDrawerItem(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .padding(horizontal = 4.dp),
                                    label = { Text(c.name) },
                                    selected = readVm.currentChapter == i,
                                    onClick = { showChangeChapter = true },
                                    shape = RoundedCornerShape(8.0.dp)//MaterialTheme.shapes.medium
                                )

                                if (i < readVm.list.lastIndex) Divider()
                            }
                        }
                    }
                }
            }
        } else null
    ) {

        val showItems = readVm.showInfo || listState.isScrolledToTheEnd()

        /*val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }*/
        //val currentOffset = animateFloatAsState(targetValue = if(showInfo) 0f else scrollBehavior.offsetLimit)
        //if(showInfo) scrollBehavior.offset = currentOffset.value// else scrollBehavior.offset = currentOffset.value

        val topAppBarScrollState = rememberTopAppBarScrollState()
        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }

        val topBarHeight = 32.dp//28.dp
        val topBarHeightPx = with(LocalDensity.current) { topBarHeight.roundToPx().toFloat() }
        val topBarOffsetHeightPx = remember { mutableStateOf(0f) }

        val toolbarHeight = 64.dp
        val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.roundToPx().toFloat() }
        val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                //by scrollBehavior.nestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y

                    val newTopOffset = topBarOffsetHeightPx.value + delta
                    if (topBarOffsetHeightPx.value != newTopOffset)
                        topBarOffsetHeightPx.value = newTopOffset.coerceIn(-topBarHeightPx, 0f)

                    val newOffset = toolbarOffsetHeightPx.value + delta
                    if (toolbarOffsetHeightPx.value != newOffset)
                        toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)

                    return scrollBehavior.nestedScrollConnection.onPreScroll(available, source)//Offset.Zero
                }
            }
        }

        Scaffold(
            //TODO: This stuff will be used again once we find a way to keep the top and bottom bars out when reaching the bottom
            // and animating the top and bottom bars away
            modifier = Modifier.nestedScroll(nestedScrollConnection),
            /*topBar = {
                TopBar(
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier
                        .height(topBarHeight)
                        .align(Alignment.TopCenter)
                        .alpha(animateTopBar),
                    pages = pages,
                    currentPage = currentPage
                )
            },
            bottomBar = {
                BottomBar(
                    modifier = Modifier
                        .height(toolbarHeight)
                        .align(Alignment.BottomCenter)
                        .alpha(animateTopBar),
                    scrollBehavior = scrollBehavior,
                    onPageSelectClick = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                    onSettingsClick = { settingsPopup = true },
                    chapterChange = ::showToast
                )
            }*/
        ) { p ->
            //TODO: If/when swipe refresh gains a swipe up to refresh, make the swipe up go to the next chapter
            SwipeRefresh(
                state = swipeState,
                onRefresh = { readVm.refresh() },
                modifier = Modifier.padding(p)
            ) {
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(LocalContext.current.dpToPx(paddingPage).dp),
                        contentPadding = PaddingValues(
                            top = topBarHeight,
                            bottom = toolbarHeight
                        )
                    ) {
                        reader(pages, readVm) {
                            readVm.showInfo = !readVm.showInfo
                            if (!readVm.showInfo) {
                                toolbarOffsetHeightPx.value = -toolbarHeightPx
                                topBarOffsetHeightPx.value = -topBarHeightPx
                            }
                        }
                    }

                    val animateTopBar by animateFloatAsState(
                        if (showItems) 1f
                        else 1f - (-topBarOffsetHeightPx.value.roundToInt() / topBarHeightPx)
                    )

                    TopBar(
                        scrollBehavior = scrollBehavior,
                        modifier = Modifier
                            .height(topBarHeight)
                            .align(Alignment.TopCenter)
                            .alpha(animateTopBar)
                            .offset { IntOffset(x = 0, y = if (showItems) 0 else (topBarOffsetHeightPx.value.roundToInt())) },
                        pages = pages,
                        currentPage = currentPage,
                        vm = readVm
                    )

                    BottomBar(
                        modifier = Modifier
                            .height(toolbarHeight)
                            .align(Alignment.BottomCenter)
                            .alpha(animateTopBar)
                            .offset { IntOffset(x = 0, y = if (showItems) 0 else (-toolbarOffsetHeightPx.value.roundToInt())) },
                        scrollBehavior = scrollBehavior,
                        onPageSelectClick = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                        onSettingsClick = { settingsPopup = true },
                        chapterChange = ::showToast,
                        vm = readVm
                    )
                }
            }
        }
    }
}

private fun LazyListScope.reader(pages: List<String>, vm: ReadViewModel, onClick: () -> Unit) {

    /*items(pages) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale = (scale * zoomChange).coerceAtLeast(1f)
            offset += offsetChange
        }

        Box(
            modifier = Modifier
                // add transformable to listen to multitouch transformation events
                // after offset
                .transformable(state = state)
                //TODO: For some reason this is causing the weird performance issue
                // it is a known issue: https://issuetracker.google.com/issues/204328131
                // when that gets resolved, look into adding back the nestedScrollConnection by scrollBehavior
                .combinedClickable(
                    onClick = {
                        showInfo = !showInfo
                        if (!showInfo) {
                            toolbarOffsetHeightPx.value = -toolbarHeightPx
                            topBarOffsetHeightPx.value = -topBarHeightPx
                        }
                    },
                    onDoubleClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    onLongClick = {},
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            Text(
                stringResource(R.string.doubleTapToReset),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
            val scaleAnim = animateFloatAsState(scale).value
            val (x, y) = animateOffsetAsState(targetValue = offset).value

            *//*GlideImage(
                    imageModel = it,
                    contentScale = ContentScale.FillWidth,
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = M3MaterialTheme.colorScheme.primary
                        )
                    },
                    success = {
                        val byteArray = remember {
                            val stream = ByteArrayOutputStream()
                            it.drawable?.toBitmap()?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val b = stream.toByteArray()
                            ByteArrayInputStream(b)
                        }
                        SubSampledImage(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
                                .align(Alignment.Center)
                                .clipToBounds(),
                            imageSource = rememberInputStreamImageSource(inputStream = byteArray)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
                        .align(Alignment.Center)
                        .clipToBounds()
                )*//*

                GlideImage(
                    imageModel = it,
                    contentScale = ContentScale.FillWidth,
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = M3MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
                        .align(Alignment.Center)
                        .clipToBounds()
                        .graphicsLayer(
                            scaleX = scaleAnim,
                            scaleY = scaleAnim,
                            translationX = x,
                            translationY = y
                        )
                )
            }
        }*/

    items(pages, key = { it }) { ChapterPage(it, onClick, vm.headers) }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(id = R.string.lastPage),
                style = M3MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            )
            if (vm.currentChapter <= 0) {
                Text(
                    stringResource(id = R.string.reachedLastChapter),
                    style = M3MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                )
            }
            if (BuildConfig.BUILD_TYPE == "release") {
                val context = LocalContext.current
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = {
                        AdView(it).apply {
                            adSize = AdSize.BANNER
                            adUnitId = context.getString(R.string.ad_unit_id)
                            loadAd(vm.ad)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChapterPage(chapterLink: String, openCloseOverlay: () -> Unit, headers: Map<String, String>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .requiredHeightIn(min = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        ZoomableImage(
            modifier = Modifier.fillMaxWidth(),
            painter = chapterLink,
            onClick = openCloseOverlay,
            headers = headers
        )
    }
}

@Composable
private fun ZoomableImage(
    modifier: Modifier = Modifier,
    painter: String,
    headers: Map<String, String>,
    onClick: () -> Unit = {}
) {
    var centerPoint by remember { mutableStateOf(Offset.Zero) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val scaleAnim by animateFloatAsState(
        targetValue = scale
    ) {
        if (scale == 1f) offset = Offset.Zero
    }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        scale = scale.coerceIn(1f, 5f)

        offset += offsetChange
        offset = clampOffset(centerPoint, offset, scale)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size.toSize() / 2.0f
                centerPoint = Offset(size.width, size.height)
            }
            .transformable(state)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = {
                        when {
                            scale > 2f -> {
                                scale = 1f
                            }
                            else -> {
                                scale = 3f

                                offset = (centerPoint - it) * (scale - 1)
                                offset = clampOffset(centerPoint, offset, scale)
                            }
                        }

                    }
                )
            }
    ) {
        var retryHash by remember { mutableStateOf(0) }
        val painterModel = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(painter)
                .apply { headers.forEach { addHeader(it.key, it.value) } }
                .crossfade(true)
                .size(Size.ORIGINAL)
                .setParameter("retry_hash", retryHash, null)
                .build()
        )

        if (painterModel.state is AsyncImagePainter.State.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (painterModel.state is AsyncImagePainter.State.Error) {
            Text(
                stringResource(R.string.pressToRefresh),
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { retryHash++ }
            )
        }

        Image(
            painter = painterModel,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = ComposableUtils.IMAGE_HEIGHT)
                .align(Alignment.Center)
                .clipToBounds()
                .graphicsLayer {
                    translationX = offset.x
                    translationY = offset.y

                    scaleX = scaleAnim
                    scaleY = scaleAnim
                }
        )
    }
}

private fun clampOffset(centerPoint: Offset, offset: Offset, scale: Float): Offset {
    val maxPosition = centerPoint * (scale - 1)

    return offset.copy(
        x = offset.x.coerceIn(-maxPosition.x, maxPosition.x),
        y = offset.y.coerceIn(-maxPosition.y, maxPosition.y)
    )
}

@ExperimentalAnimationApi
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    pages: List<String>,
    currentPage: Int,
    vm: ReadViewModel
) {
    CenterAlignedTopAppBar(
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        navigationIcon = {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    vm.batteryIcon.composeIcon,
                    contentDescription = null,
                    tint = animateColorAsState(
                        if (vm.batteryColor == androidx.compose.ui.graphics.Color.White) M3MaterialTheme.colorScheme.onSurface
                        else vm.batteryColor
                    ).value
                )
                AnimatedContent(
                    targetState = vm.batteryPercent.toInt(),
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { height -> height } + fadeIn() with slideOutVertically { height -> -height } + fadeOut()
                        } else {
                            slideInVertically { height -> -height } + fadeIn() with slideOutVertically { height -> height } + fadeOut()
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

            val activity = LocalActivity.current

            DisposableEffect(LocalContext.current) {
                val timeReceiver = activity.timeTick { _, _ -> time = System.currentTimeMillis() }
                onDispose { activity.unregisterReceiver(timeReceiver) }
            }

            AnimatedContent(
                targetState = time,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn() with slideOutVertically { height -> -height } + fadeOut())
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
        actions = {
            PageIndicator(
                Modifier
                    .padding(4.dp)
                    .align(Alignment.CenterVertically),
                currentPage + 1,
                pages.size
            )
        }
    )
}

@Composable
private fun BottomBar(
    modifier: Modifier = Modifier,
    vm: ReadViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    onPageSelectClick: () -> Unit,
    onSettingsClick: () -> Unit,
    chapterChange: () -> Unit
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
            .containerColor(scrollFraction = scrollBehavior.scrollFraction).value,
        contentColor = TopAppBarDefaults.centerAlignedTopAppBarColors()
            .titleContentColor(scrollFraction = scrollBehavior.scrollFraction).value
    ) {
        val prevShown = vm.currentChapter < vm.list.lastIndex
        val nextShown = vm.currentChapter > 0

        AnimatedVisibility(
            visible = prevShown && vm.list.size > 1,
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
                previousChapter = chapterChange,
                vm = vm
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
            visible = nextShown && vm.list.size > 1,
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
                nextChapter = chapterChange,
                vm = vm
            )
        }
        //The three buttons above will equal 8f
        //So these two need to add up to 2f
        IconButton(
            onClick = onPageSelectClick,
            modifier = Modifier.weight(1f)
        ) { Icon(Icons.Default.GridOn, null) }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.weight(1f)
        ) { Icon(Icons.Default.Settings, null) }
    }
}

@Composable
@ExperimentalMaterial3Api
private fun WrapHeightNavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = CircleShape,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Surface(
        shape = shape,
        color = colors.containerColor(selected).value,
        modifier = modifier
            .heightIn(min = 56.dp)
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                interactionSource = interactionSource,
                role = Role.Tab,
                indication = null
            )
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                val iconColor = colors.iconColor(selected).value
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f)) {
                val labelColor = colors.textColor(selected).value
                CompositionLocalProvider(LocalContentColor provides labelColor, content = label)
            }
            if (badge != null) {
                Spacer(Modifier.width(12.dp))
                val badgeColor = colors.badgeColor(selected).value
                CompositionLocalProvider(LocalContentColor provides badgeColor, content = badge)
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun PageIndicator(modifier: Modifier = Modifier, currentPage: Int, pageCount: Int) {
    Row(modifier = modifier) {
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

private fun Context.dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

private fun LazyListState.isScrolledToTheEnd() = layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1

@Composable
private fun GoBackButton(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    M3OutlinedButton(
        onClick = { navController.popBackStack() },
        modifier = modifier,
        border = BorderStroke(androidx.compose.material.ButtonDefaults.OutlinedBorderSize, M3MaterialTheme.colorScheme.primary)
    ) { Text(stringResource(id = R.string.goBack), style = M3MaterialTheme.typography.labelLarge, color = M3MaterialTheme.colorScheme.primary) }
}

@Composable
private fun NextButton(modifier: Modifier = Modifier, vm: ReadViewModel, nextChapter: () -> Unit) {
    Button(
        onClick = { vm.addChapterToWatched(--vm.currentChapter, nextChapter) },
        modifier = modifier
    ) { Text(stringResource(id = R.string.loadNextChapter)) }
}

@Composable
private fun PreviousButton(modifier: Modifier = Modifier, vm: ReadViewModel, previousChapter: () -> Unit) {
    TextButton(
        onClick = { vm.addChapterToWatched(++vm.currentChapter, previousChapter) },
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
    range: ClosedFloatingPointRange<Float>
) {
    var sliderValue by remember { mutableStateOf(initialValue.toFloat()) }

    val activity = LocalActivity.current

    SliderSetting(
        sliderValue = sliderValue,
        settingTitle = { Text(stringResource(id = settingTitle)) },
        settingSummary = { Text(stringResource(id = settingSummary)) },
        range = range,
        updateValue = {
            sliderValue = it
            scope.launch { activity.updatePref(preference, sliderValue.toInt()) }
        },
        settingIcon = { Icon(settingIcon, null) }
    )
}

class ReadActivity : AppCompatActivity() {
    private val disposable = CompositeDisposable()
    private var model: ChapterModel? = null
    private var mangaTitle: String? = null
    private var isDownloaded = false
    private val loader by lazy { Glide.with(this) }
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

        enableImmersiveMode()

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
                .onEach {
                    runOnUiThread {
                        binding.readView.removeItemDecoration(decor)
                        decor = VerticalSpaceItemDecoration(it)
                        binding.readView.addItemDecoration(decor)
                    }
                }
                .collect()
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

    override fun onDestroy() {
        super.onDestroy()
        Glide.get(this).clearMemory()
        unregisterReceiver(batteryInfo)
        disposable.dispose()
    }

}