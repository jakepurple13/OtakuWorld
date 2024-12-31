package com.programmersbox.mangaworld.reader.compose

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.mangasettings.ImageLoaderType
import com.programmersbox.mangasettings.ReaderType
import com.programmersbox.mangaworld.MangaSettingsHandling
import com.programmersbox.mangaworld.R
import com.programmersbox.uiviews.presentation.components.OtakuPullToRefreshBox
import com.programmersbox.uiviews.utils.HideSystemBarsWhileOnScreen
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.PageCurl
import eu.wewox.pagecurl.page.PageCurlState
import eu.wewox.pagecurl.page.rememberPageCurlState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalPageCurlApi::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun ReadView(
    context: Context = LocalContext.current,
    mangaSettingsHandling: MangaSettingsHandling = koinInject(),
    readVm: ReadViewModel = koinViewModel(),
) {
    HideSystemBarsWhileOnScreen()

    val snackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()

    val pages = readVm.pageList

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
                else -> 0
            }
        }
    }

    val paddingPage by mangaSettingsHandling.pagePadding
        .flow
        .collectAsStateWithLifecycle(initialValue = 4)

    val imageLoaderType by mangaSettingsHandling.rememberImageLoaderType()
    var startAction by mangaSettingsHandling.rememberPlayingStartAction()
    var middleAction by mangaSettingsHandling.rememberPlayingMiddleAction()

    fun showToast() {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(context.getString(R.string.addedChapterItem))
        }
    }

    val listShowItems by remember { derivedStateOf { listState.isScrolledToTheEnd() && readerType == ReaderType.List } }
    val pagerShowItems by remember { derivedStateOf { pagerState.currentPage >= pages.size && readerType != ReaderType.List } }

    val listIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0 } }
    LaunchedEffect(listIndex, pagerState.currentPage, readVm.showInfo) {
        if (readVm.firstScroll && (listIndex > 0 || pagerState.currentPage > 0)) {
            readVm.showInfo = false
            readVm.firstScroll = false
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

    val showItems by remember { derivedStateOf { readVm.showInfo || listShowItems || pagerShowItems } }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showBottomSheet by remember { mutableStateOf(false) }

    val floatingBottomBar by mangaSettingsHandling.rememberUseFloatingReaderBottomBar()

    var showFloatBar by remember { mutableStateOf(false) }

    LaunchedEffect(showItems) {
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
        show = readVm.addToFavorites.shouldShow,
        onDismiss = { readVm.addToFavorites = readVm.addToFavorites.copy(hasShown = true) },
        onAddToFavorites = readVm::addToFavorites
    )

    var settingsPopup by remember { mutableStateOf(false) }

    if (settingsPopup) {
        SettingsSheet(
            onDismiss = { settingsPopup = false },
            mangaSettingsHandling = mangaSettingsHandling,
            readerType = readerType,
            readerTypeChange = { readerType = it },
            startAction = startAction,
            onStartActionChange = { startAction = it },
            middleAction = middleAction,
            onMiddleActionChange = { middleAction = it }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SheetView(
                readVm = readVm,
                onSheetHide = { showBottomSheet = false },
                currentPage = currentPage,
                pages = pages,
                onPageChange = {
                    when (readerType) {
                        ReaderType.List, ReaderType.FlipPager -> listState.animateScrollToItem(it)
                        ReaderType.Pager -> pagerState.animateScrollToPage(it)
                        ReaderType.CurlPager -> curlState.snapTo(it)
                        else -> {}
                    }
                },
            )
        }
    }

    val hazeState = remember { HazeState() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerView(
                    readVm = readVm,
                    showToast = ::showToast
                )
            }
        },
        gesturesEnabled = (readVm.list.size > 1 && userGestureAllowed) || drawerState.isOpen
    ) {
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
                        pages = pages,
                        currentPage = currentPage,
                        currentChapter = readVm
                            .currentChapterModel
                            ?.name
                            ?: "Ch ${readVm.list.size - readVm.currentChapter}",
                        playingStartAction = startAction,
                        playingMiddleAction = middleAction,
                        showBlur = showBlur,
                        modifier = if (showBlur) Modifier.hazeChild(hazeState, style = HazeMaterials.thin()) {
                            progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f, preferPerformance = true)
                        } else Modifier
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showItems,
                    enter = slideInVertically { it } + fadeIn(
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                    ),
                    exit = slideOutVertically { it } + fadeOut(
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                    )
                ) {
                    if (floatingBottomBar) {
                        FloatingBottomBar(
                            onPageSelectClick = { showBottomSheet = true },
                            onSettingsClick = { settingsPopup = true },
                            chapterChange = ::showToast,
                            onChapterShow = { scope.launch { drawerState.open() } },
                            vm = readVm,
                            showFloatBar = showFloatBar,
                            onShowFloatBarChange = { showFloatBar = it },
                        )
                    } else {
                        BottomBar(
                            onPageSelectClick = { showBottomSheet = true },
                            onSettingsClick = { settingsPopup = true },
                            chapterChange = ::showToast,
                            onChapterShow = { scope.launch { drawerState.open() } },
                            vm = readVm,
                            showBlur = showBlur,
                            isAmoledMode = isAmoledMode,
                            modifier = if (showBlur) Modifier.hazeChild(hazeState, style = HazeMaterials.thin()) {
                                //progressive = HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f, preferPerformance = true)
                            } else Modifier,
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { p ->
            Box(
                modifier = if (showBlur)
                    Modifier.haze(state = hazeState)
                else
                    Modifier,
            ) {
                OtakuPullToRefreshBox(
                    isRefreshing = readVm.isLoadingPages,
                    onRefresh = readVm::refresh,
                    paddingValues = p
                ) {
                    val spacing = LocalContext.current.dpToPx(paddingPage).dp
                    Crossfade(
                        targetState = readerType,
                        label = "",
                        modifier = if (imageLoaderType == ImageLoaderType.Panpf) {
                            Modifier
                        } else {
                            Modifier.zoomable(
                                zoomState = rememberZoomState(),
                                enableOneFingerZoom = false,
                                onTap = { readVm.showInfo = !readVm.showInfo }
                            )
                        }
                    ) {
                        when (it) {
                            ReaderType.List -> {
                                ListView(
                                    listState = listState,
                                    pages = pages,
                                    readVm = readVm,
                                    itemSpacing = spacing,
                                    paddingValues = PaddingValues(bottom = p.calculateBottomPadding()),
                                    imageLoaderType = imageLoaderType,
                                ) { readVm.showInfo = !readVm.showInfo }
                            }

                            ReaderType.Pager -> {
                                PagerView(
                                    pagerState = pagerState,
                                    pages = pages,
                                    vm = readVm,
                                    itemSpacing = spacing,
                                    imageLoaderType = imageLoaderType,
                                ) { readVm.showInfo = !readVm.showInfo }
                            }

                            ReaderType.FlipPager -> {
                                FlipPagerView(
                                    pagerState = pagerState,
                                    pages = pages,
                                    vm = readVm,
                                    imageLoaderType = imageLoaderType,
                                ) { readVm.showInfo = !readVm.showInfo }
                            }

                            ReaderType.CurlPager -> {
                                CurlPagerView(
                                    pagerState = curlState,
                                    pages = pages,
                                    vm = readVm,
                                    imageLoaderType = imageLoaderType,
                                ) { readVm.showInfo = !readVm.showInfo }
                            }

                            else -> {}
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
    onClick: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        contentPadding = paddingValues,
    ) { reader(pages, readVm, onClick, imageLoaderType) }
}

@Composable
fun PagerView(
    pagerState: PagerState,
    pages: List<String>,
    vm: ReadViewModel,
    itemSpacing: Dp,
    imageLoaderType: ImageLoaderType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
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
                openCloseOverlay = onClick,
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
    onClick: () -> Unit,
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
                openCloseOverlay = onClick,
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
    onClick: () -> Unit,
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
                openCloseOverlay = onClick,
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
    onClick: () -> Unit,
    imageLoaderType: ImageLoaderType,
) {
    items(pages, key = { it }, contentType = { it }) {
        ChapterPage(
            chapterLink = { it },
            isDownloaded = vm.isDownloaded,
            openCloseOverlay = onClick,
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
