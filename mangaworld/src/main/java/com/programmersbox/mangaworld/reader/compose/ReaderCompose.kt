package com.programmersbox.mangaworld.reader.compose

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.mangasettings.ImageLoaderType
import com.programmersbox.mangaworld.ChapterHolder
import com.programmersbox.mangaworld.MangaSettingsHandling
import com.programmersbox.mangaworld.R
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.HideSystemBarsWhileOnScreen
import com.programmersbox.uiviews.utils.LocalActivity
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.components.OtakuPullToRefreshBox
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun ReadView(
    mangaReader: ReadViewModel.MangaReader,
    context: Context = LocalContext.current,
    genericInfo: GenericInfo = LocalGenericInfo.current,
    ch: ChapterHolder = koinInject(),
    mangaSettingsHandling: MangaSettingsHandling = koinInject(),
    dao: ItemDao = LocalItemDao.current,
    readVm: ReadViewModel = viewModel {
        ReadViewModel(
            mangaReader = mangaReader,
            genericInfo = genericInfo,
            chapterHolder = ch,
            dao = dao
        )
    },
) {
    HideSystemBarsWhileOnScreen()

    val scope = rememberCoroutineScope()

    val pages = readVm.pageList

    val showBlur by LocalSettingsHandling.current.rememberShowBlur()

    var listOrPager by mangaSettingsHandling.rememberListOrPager()

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { pages.size + 1 }

    val listState = rememberLazyListState()
    val currentPage by remember { derivedStateOf { if (listOrPager) listState.firstVisibleItemIndex else pagerState.currentPage } }

    val paddingPage by mangaSettingsHandling.pagePadding
        .flow
        .collectAsStateWithLifecycle(initialValue = 4)

    val imageLoaderType by mangaSettingsHandling.rememberImageLoaderType()
    var startAction by mangaSettingsHandling.rememberPlayingStartAction()
    var middleAction by mangaSettingsHandling.rememberPlayingMiddleAction()

    val activity = LocalActivity.current

    fun showToast() {
        activity.runOnUiThread { Toast.makeText(context, R.string.addedChapterItem, Toast.LENGTH_SHORT).show() }
    }

    val listShowItems by remember { derivedStateOf { listState.isScrolledToTheEnd() && listOrPager } }
    val pagerShowItems by remember { derivedStateOf { pagerState.currentPage >= pages.size && !listOrPager } }

    val listIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0 } }
    LaunchedEffect(listIndex, pagerState.currentPage, readVm.showInfo) {
        if (readVm.firstScroll && (listIndex > 0 || pagerState.currentPage > 0)) {
            readVm.showInfo = false
            readVm.firstScroll = false
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { listState.scrollToItem(it) }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { pagerState.scrollToPage(it) }
    }

    val showItems by remember { derivedStateOf { readVm.showInfo || listShowItems || pagerShowItems } }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showBottomSheet by remember { mutableStateOf(false) }

    BackHandler(drawerState.isOpen || showBottomSheet) {
        scope.launch {
            when {
                drawerState.isOpen -> drawerState.close()
                showBottomSheet -> showBottomSheet = false
            }
        }
    }

    var settingsPopup by remember { mutableStateOf(false) }

    if (settingsPopup) {
        SettingsSheet(
            onDismiss = { settingsPopup = false },
            mangaSettingsHandling = mangaSettingsHandling,
            listOrPager = listOrPager,
            listOrPagerChange = { listOrPager = it },
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
                listOrPager = listOrPager,
                pagerState = pagerState,
                listState = listState
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
        gesturesEnabled = readVm.list.size > 1
    ) {
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = showItems,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
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
                        modifier = if (showBlur) Modifier.hazeChild(hazeState) else Modifier
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showItems,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    BottomBar(
                        onPageSelectClick = { showBottomSheet = true },
                        onSettingsClick = { settingsPopup = true },
                        chapterChange = ::showToast,
                        vm = readVm,
                        showBlur = showBlur,
                        modifier = if (showBlur) Modifier.hazeChild(hazeState) else Modifier
                    )
                }
            },
        ) { p ->
            Box(
                modifier = if (showBlur)
                    Modifier.haze(
                        state = hazeState,
                        style = HazeDefaults.style(backgroundColor = MaterialTheme.colorScheme.background),
                    ) else Modifier,
            ) {
                OtakuPullToRefreshBox(
                    isRefreshing = readVm.isLoadingPages,
                    onRefresh = readVm::refresh,
                    paddingValues = p
                ) {
                    val spacing = LocalContext.current.dpToPx(paddingPage).dp
                    Crossfade(targetState = listOrPager, label = "") {
                        if (it) {
                            ListView(
                                listState = listState,
                                pages = pages,
                                readVm = readVm,
                                itemSpacing = spacing,
                                paddingValues = PaddingValues(bottom = p.calculateBottomPadding()),
                                imageLoaderType = imageLoaderType,
                            ) { readVm.showInfo = !readVm.showInfo }
                        } else {
                            PagerView(
                                pagerState = pagerState,
                                pages = pages,
                                vm = readVm,
                                itemSpacing = spacing,
                                imageLoaderType = imageLoaderType,
                            ) { readVm.showInfo = !readVm.showInfo }
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
