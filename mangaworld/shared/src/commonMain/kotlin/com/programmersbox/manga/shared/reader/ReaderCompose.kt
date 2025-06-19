package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.mangasettings.ImageLoaderType
import com.programmersbox.datastore.mangasettings.ReaderType
import com.programmersbox.kmpuiviews.presentation.components.OtakuPullToRefreshBox
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.kmpuiviews.utils.RecordTimeSpentDoing
import com.programmersbox.manga.shared.reader.curl.ExperimentalPageCurlApi
import com.programmersbox.manga.shared.reader.curl.PageCurl
import com.programmersbox.manga.shared.reader.curl.PageCurlState
import com.programmersbox.manga.shared.reader.curl.rememberPageCurlConfig
import com.programmersbox.manga.shared.reader.curl.rememberPageCurlState
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomableWithScroll
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalZoomableApi::class, ExperimentalPageCurlApi::class)
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun ReadView(
    mangaSettingsHandling: MangaNewSettingsHandling = koinInject(),
    viewModel: ReadViewModel = koinViewModel(),
) {
    HideNavBarWhileOnScreen()
    RecordTimeSpentDoing()

    val includeInsets by mangaSettingsHandling.rememberIncludeInsetsForReader()
    var insetsController by insetsController(includeInsets)

    val snackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()

    val pages = viewModel.pageList

    val settings = LocalSettingsHandling.current

    val showBlur by settings.rememberShowBlur()
    val isAmoledMode by settings.rememberIsAmoledMode()

    var readerType by mangaSettingsHandling.rememberReaderType()

    val userGestureAllowed by mangaSettingsHandling.rememberUserGestureEnabled()

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { pages.size + 1 }

    val listState = rememberLazyListState()
    val curlState = rememberPageCurlState(initialCurrent = 0)
    val currentPage by remember {
        derivedStateOf {
            when (readerType) {
                ReaderType.List, ReaderType.FlipPager -> listState.firstVisibleItemIndex
                ReaderType.Pager -> pagerState.currentPage
                ReaderType.CurlPager -> curlState.current
            }
        }
    }

    val paddingPage by mangaSettingsHandling.pagePadding
        .flow
        .collectAsStateWithLifecycle(initialValue = 4)

    val imageLoaderType by mangaSettingsHandling.rememberImageLoaderType()

    fun showToast() {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("Added Chapter")
        }
    }

    val listShowItems by remember { derivedStateOf { listState.isScrolledToTheEnd() && readerType == ReaderType.List } }
    val pagerShowItems by remember { derivedStateOf { pagerState.currentPage >= pages.size && readerType != ReaderType.List } }

    val listIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0 } }
    LaunchedEffect(listIndex, pagerState.currentPage, viewModel.showInfo) {
        if (viewModel.firstScroll && (listIndex > 0 || pagerState.currentPage > 0)) {
            viewModel.showInfo = false
            viewModel.firstScroll = false
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            listState.scrollToItem(it)
            runCatching { curlState.snapTo(it) }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect {
            pagerState.scrollToPage(it)
            runCatching { curlState.snapTo(it) }
        }
    }

    LaunchedEffect(curlState) {
        snapshotFlow { curlState.current }.collect {
            pagerState.scrollToPage(it)
            listState.scrollToItem(it)
        }
    }

    val showItems by remember { derivedStateOf { viewModel.showInfo || listShowItems || pagerShowItems } }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showBottomSheet by remember { mutableStateOf(false) }

    val floatingBottomBar by mangaSettingsHandling.rememberUseFloatingReaderBottomBar()

    var showFloatBar by remember { mutableStateOf(false) }

    LaunchedEffect(showItems) {
        if (includeInsets) insetsController = showItems
        if (showItems) {
            delay(250)
            showFloatBar = true
        } else {
            showFloatBar = false
        }
    }

    BackHandler(drawerState.isOpen || showBottomSheet) {
        scope.launch {
            when {
                drawerState.isOpen -> drawerState.close()
                showBottomSheet -> showBottomSheet = false
            }
        }
    }

    AddToFavoritesDialog(
        show = viewModel.addToFavorites.shouldShow,
        onDismiss = { viewModel.addToFavorites = viewModel.addToFavorites.copy(hasShown = true) },
        onAddToFavorites = viewModel::addToFavorites
    )

    var settingsPopup by remember { mutableStateOf(false) }

    if (settingsPopup) {
        SettingsSheet(
            onDismiss = { settingsPopup = false },
            mangaSettingsHandling = mangaSettingsHandling,
            readerType = readerType,
            readerTypeChange = { readerType = it },
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SheetView(
                readVm = viewModel,
                onSheetHide = { showBottomSheet = false },
                currentPage = currentPage,
                pages = pages,
                onPageChange = {
                    when (readerType) {
                        ReaderType.List, ReaderType.FlipPager -> listState.animateScrollToItem(it)
                        ReaderType.Pager -> pagerState.animateScrollToPage(it)
                        ReaderType.CurlPager -> curlState.snapTo(it)
                    }
                },
            )
        }
    }

    val hazeState = remember { HazeState() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                DrawerView(
                    readVm = viewModel,
                    showToast = ::showToast
                )
            }
        },
        gesturesEnabled = (viewModel.list.size > 1 && userGestureAllowed) || drawerState.isOpen
    ) {
        //TODO: Maybe make this an option?
        val scrollAlpha by remember {
            derivedStateOf {
                when (readerType) {
                    ReaderType.List -> {
                        if (listState.firstVisibleItemIndex == 0) {
                            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.let {
                                (it.offset / it.size.toFloat()).absoluteValue
                            } ?: 1f
                        } else {
                            1f
                        }
                    }

                    ReaderType.Pager -> 1f
                    ReaderType.FlipPager -> 1f
                    ReaderType.CurlPager -> 1f
                }
            }
        }

        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = showItems,
                    enter = slideInVertically { -it } + fadeIn(
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                    ),
                    exit = slideOutVertically { -it } + fadeOut(
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                    )
                ) {
                    ReaderTopBar(
                        currentChapter = viewModel
                            .currentChapterModel
                            ?.name
                            ?: "Ch ${viewModel.list.size - viewModel.currentChapter}",
                        onSettingsClick = { settingsPopup = true },
                        showBlur = showBlur,
                        windowInsets = if (includeInsets) TopAppBarDefaults.windowInsets else WindowInsets(0.dp),
                        modifier = Modifier.hazeEffect(hazeState, style = HazeMaterials.thin()) {
                            blurEnabled = showBlur
                            progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f, preferPerformance = true)
                            alpha = scrollAlpha
                        }
                    )
                }
            },
            bottomBar = {
                if (!floatingBottomBar) {
                    AnimatedVisibility(
                        visible = showItems,
                        enter = slideInVertically { it } + fadeIn(
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                        ),
                        exit = slideOutVertically { it } + fadeOut(
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                        )
                    ) {
                        //TODO: Can't really use key since it doesn't give the button animation
                        //key(scrollAlpha) {
                        FloatingBottomBar(
                            onPageSelectClick = { showBottomSheet = true },
                            onNextChapter = { viewModel.addChapterToWatched(--viewModel.currentChapter, ::showToast) },
                            onPreviousChapter = { viewModel.addChapterToWatched(++viewModel.currentChapter, ::showToast) },
                            onChapterShow = { scope.launch { drawerState.open() } },
                            showBlur = showBlur,
                            isAmoledMode = isAmoledMode,
                            chapterNumber = (viewModel.list.size - viewModel.currentChapter).toString(),
                            chapterCount = viewModel.list.size.toString(),
                            currentPage = currentPage,
                            pages = animateIntAsState(pages.size).value,
                            previousButtonEnabled = viewModel.currentChapter < viewModel.list.lastIndex && viewModel.list.size > 1,
                            nextButtonEnabled = viewModel.currentChapter > 0 && viewModel.list.size > 1,
                            modifier = Modifier
                                .windowInsetsPadding(if (includeInsets) NavigationBarDefaults.windowInsets else WindowInsets(0.dp))
                                .padding(16.dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .hazeEffect(hazeState, style = HazeMaterials.thin()) {
                                    //progressive = HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f, preferPerformance = true)
                                    blurEnabled = showBlur
                                    //alpha = scrollAlpha
                                }
                        )
                        //}
                    }
                }
            },
            floatingActionButton = {
                if (floatingBottomBar) {
                    AnimatedVisibility(
                        visible = showItems,
                        enter = slideInVertically { it } + fadeIn(
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                        ),
                        exit = slideOutVertically { it } + fadeOut(
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                        )
                    ) {
                        FloatingFloatingActionButton(
                            onPageSelectClick = { showBottomSheet = true },
                            onSettingsClick = { settingsPopup = true },
                            chapterChange = ::showToast,
                            onChapterShow = { scope.launch { drawerState.open() } },
                            vm = viewModel,
                            showFloatBar = showFloatBar,
                            onShowFloatBarChange = { showFloatBar = it },
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { p ->
            Box(
                modifier = if (showBlur)
                    Modifier.hazeSource(state = hazeState)
                else
                    Modifier,
            ) {
                OtakuPullToRefreshBox(
                    isRefreshing = viewModel.isLoadingPages,
                    onRefresh = viewModel::refresh,
                    paddingValues = p
                ) {
                    val spacing = dpToPx(paddingPage).dp
                    Crossfade(
                        targetState = readerType,
                        label = "",
                        modifier = if (imageLoaderType == ImageLoaderType.Panpf) {
                            Modifier
                        } else {
                            Modifier.zoomableWithScroll(
                                zoomState = rememberZoomState(),
                                enableOneFingerZoom = false,
                                onTap = { viewModel.showInfo = !viewModel.showInfo }
                            )
                        }
                    ) {
                        when (it) {
                            ReaderType.List -> {
                                ListView(
                                    listState = listState,
                                    pages = pages,
                                    readVm = viewModel,
                                    itemSpacing = spacing,
                                    paddingValues = PaddingValues(
                                        top = if (pages.isNotEmpty()) 0.dp else p.calculateTopPadding(),
                                        bottom = p.calculateBottomPadding()
                                    ).animate(),
                                    imageLoaderType = imageLoaderType,
                                )
                            }

                            ReaderType.Pager -> {
                                PagerView(
                                    pagerState = pagerState,
                                    pages = pages,
                                    vm = viewModel,
                                    itemSpacing = spacing,
                                    imageLoaderType = imageLoaderType,
                                )
                            }

                            ReaderType.FlipPager -> {
                                FlipPagerView(
                                    pagerState = pagerState,
                                    pages = pages,
                                    vm = viewModel,
                                    imageLoaderType = imageLoaderType,
                                )
                            }

                            ReaderType.CurlPager -> {
                                CurlPagerView(
                                    pagerState = curlState,
                                    pages = pages,
                                    vm = viewModel,
                                    imageLoaderType = imageLoaderType,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListView(
    listState: LazyListState,
    pages: List<String>,
    readVm: ReadViewModel,
    itemSpacing: Dp,
    paddingValues: PaddingValues,
    imageLoaderType: ImageLoaderType,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        contentPadding = paddingValues,
    ) { reader(pages, readVm, imageLoaderType) }
}

@Composable
fun PagerView(
    pagerState: PagerState,
    pages: List<String>,
    vm: ReadViewModel,
    itemSpacing: Dp,
    imageLoaderType: ImageLoaderType,
    modifier: Modifier = Modifier,
) {
    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        pageSpacing = itemSpacing,
        beyondViewportPageCount = 1,
        key = { it }
    ) { page ->
        pages.getOrNull(page)?.let {
            ChapterPage(
                chapterLink = { it },
                isDownloaded = vm.isDownloaded,
                headers = vm.headers,
                contentScale = ContentScale.Fit,
                imageLoaderType = imageLoaderType
            )
        } ?: Box(modifier = Modifier.fillMaxSize()) {
            LastPageReached(
                isLoading = vm.isLoadingPages,
                currentChapter = vm.currentChapter,
                lastChapter = vm.list.lastIndex,
                chapterName = vm.list.getOrNull(vm.currentChapter)?.name.orEmpty(),
                nextChapter = { vm.addChapterToWatched(++vm.currentChapter) {} },
                previousChapter = { vm.addChapterToWatched(--vm.currentChapter) {} },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun FlipPagerView(
    pagerState: PagerState,
    pages: List<String>,
    vm: ReadViewModel,
    imageLoaderType: ImageLoaderType,
    modifier: Modifier = Modifier,
) {
    FlipPager(
        state = pagerState,
        orientation = FlipPagerOrientation.Vertical,
        key = { it },
        modifier = modifier.fillMaxSize(),
    ) { page ->
        pages.getOrNull(page)?.let {
            ChapterPage(
                chapterLink = { it },
                isDownloaded = vm.isDownloaded,
                headers = vm.headers,
                contentScale = ContentScale.Fit,
                imageLoaderType = imageLoaderType
            )
        } ?: Box(modifier = Modifier.fillMaxSize()) {
            LastPageReached(
                isLoading = vm.isLoadingPages,
                currentChapter = vm.currentChapter,
                lastChapter = vm.list.lastIndex,
                chapterName = vm.list.getOrNull(vm.currentChapter)?.name.orEmpty(),
                nextChapter = { vm.addChapterToWatched(++vm.currentChapter) {} },
                previousChapter = { vm.addChapterToWatched(--vm.currentChapter) {} },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun CurlPagerView(
    pagerState: PageCurlState,
    pages: List<String>,
    vm: ReadViewModel,
    imageLoaderType: ImageLoaderType,
    modifier: Modifier = Modifier,
) {
    //TODO: Make go reverse
    PageCurl(
        state = pagerState,
        key = { it },
        count = pages.size.coerceAtLeast(2),
        config = rememberPageCurlConfig(
            dragForwardEnabled = true,
            dragBackwardEnabled = true,
            tapForwardEnabled = false,
            tapBackwardEnabled = false,
            backPageColor = MaterialTheme.colorScheme.surface,
            onCustomTap = { _, _ -> true }
        ),
        modifier = modifier.fillMaxSize(),
    ) { page ->
        pages.getOrNull(page)?.let {
            ChapterPage(
                chapterLink = { it },
                isDownloaded = vm.isDownloaded,
                headers = vm.headers,
                contentScale = ContentScale.Fit,
                imageLoaderType = imageLoaderType
            )
        } ?: Box(modifier = Modifier.fillMaxSize()) {
            LastPageReached(
                isLoading = vm.isLoadingPages,
                currentChapter = vm.currentChapter,
                lastChapter = vm.list.lastIndex,
                chapterName = vm.list.getOrNull(vm.currentChapter)?.name.orEmpty(),
                nextChapter = { vm.addChapterToWatched(++vm.currentChapter) {} },
                previousChapter = { vm.addChapterToWatched(--vm.currentChapter) {} },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun LazyListScope.reader(
    pages: List<String>,
    vm: ReadViewModel,
    imageLoaderType: ImageLoaderType,
) {
    itemsIndexed(
        pages,
        key = { index, it -> "$it$index" },
        contentType = { index, it -> it }
    ) { _, it ->
        ChapterPage(
            chapterLink = { it },
            isDownloaded = vm.isDownloaded,
            headers = vm.headers,
            contentScale = ContentScale.FillWidth,
            imageLoaderType = imageLoaderType
        )
    }
    item {
        LastPageReached(
            isLoading = vm.isLoadingPages,
            currentChapter = vm.currentChapter,
            lastChapter = vm.list.lastIndex,
            chapterName = vm.list.getOrNull(vm.currentChapter)?.name.orEmpty(),
            nextChapter = { vm.addChapterToWatched(++vm.currentChapter) {} },
            previousChapter = { vm.addChapterToWatched(--vm.currentChapter) {} },
        )
    }
}

@Composable
expect fun insetsController(defaultValue: Boolean): MutableState<Boolean>
