package com.programmersbox.novel.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.presentation.components.OtakuPullToRefreshBox
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.kmpuiviews.utils.RecordTimeSpentDoing
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun NovelReadView(
    viewModel: ReadViewModel = koinViewModel(),
) {
    HideNavBarWhileOnScreen()
    RecordTimeSpentDoing()

    var insetsController by insetsController(true)

    val snackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()

    val settings = LocalSettingsHandling.current

    val showBlur by settings.rememberShowBlur()
    val isAmoledMode by settings.rememberIsAmoledMode()

    fun showToast() {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("Added Chapter")
        }
    }

    val showItems by remember { derivedStateOf { viewModel.showInfo } }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showBottomSheet by remember { mutableStateOf(false) }

    var showFloatBar by remember { mutableStateOf(false) }

    LaunchedEffect(showItems) {
        insetsController = showItems
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
        gesturesEnabled = (viewModel.list.size > 1) || drawerState.isOpen
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
                        currentChapter = viewModel
                            .currentChapterModel
                            ?.name
                            ?: "Ch ${viewModel.list.size - viewModel.currentChapter}",
                        onSettingsClick = { },
                        showBlur = showBlur,
                        windowInsets = TopAppBarDefaults.windowInsets,
                        modifier = Modifier.hazeEffect(hazeState, style = HazeMaterials.thin()) {
                            blurEnabled = showBlur
                            progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f, preferPerformance = true)
                            alpha = 1f
                        }
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
                        currentPage = 1,
                        pages = 1,
                        previousButtonEnabled = viewModel.currentChapter < viewModel.list.lastIndex && viewModel.list.size > 1,
                        nextButtonEnabled = viewModel.currentChapter > 0 && viewModel.list.size > 1,
                        modifier = Modifier
                            .windowInsetsPadding(NavigationBarDefaults.windowInsets)
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
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize()
                    ) {
                        //FIXME: Fix this
                        Text(
                            viewModel.pageList,//AnnotatedString.fromHtml(viewModel.pageList),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = null,
                                    onClick = { viewModel.showInfo = !viewModel.showInfo }
                                ),
                        )

                        if (viewModel.currentChapter <= 0) {
                            Text(
                                "Last Chapter Reached",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
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
}

@Composable
expect fun insetsController(defaultValue: Boolean): MutableState<Boolean>

@Composable
internal fun AddToFavoritesDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onAddToFavorites: () -> Unit,
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add to Favorites?") },
            text = {
                Text("You have read a few chapters and seem to have some interest in this manga. Would you like to add it to your favorites?")
            },
            confirmButton = {
                TextButton(
                    onClick = onAddToFavorites
                ) {
                    Icon(Icons.Default.FavoriteBorder, null)
                    Spacer(Modifier.size(4.dp))
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) { Text("Cancel") }
            }
        )
    }
}