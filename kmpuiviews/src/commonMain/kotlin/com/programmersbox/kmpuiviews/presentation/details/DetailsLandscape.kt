package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kmpalette.palette.graphics.Palette
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.LocalCustomListDao
import com.programmersbox.kmpuiviews.utils.LocalItemDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.added_to_list
import otakuworld.kmpuiviews.generated.resources.already_in_list
import otakuworld.kmpuiviews.generated.resources.for_later

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun DetailsViewLandscape(
    info: KmpInfoModel,
    isSaved: Boolean,
    shareChapter: Boolean,
    chapters: List<ChapterWatched>,
    isFavorite: Boolean,
    onFavoriteClick: (Boolean) -> Unit,
    markAs: (KmpChapterModel, Boolean) -> Unit,
    description: String,
    onTranslateDescription: (MutableState<Boolean>) -> Unit,
    showDownloadButton: () -> Boolean,
    canNotify: Boolean,
    notifyAction: () -> Unit,
    onPaletteSet: (Palette) -> Unit,
) {
    val dao = LocalItemDao.current
    val listDao = LocalCustomListDao.current
    val genericInfo = koinInject<KmpGenericInfo>()
    val navController = LocalNavActions.current

    var reverseChapters by remember { mutableStateOf(false) }

    val hostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberDrawerState(DrawerValue.Closed)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    BackHandler(scaffoldState.isOpen) {
        scope.launch {
            try {
                scaffoldState.close()
            } catch (_: Exception) {
                navController.popBackStack()
            }
        }
    }

    var showLists by remember { mutableStateOf(false) }

    AddToList(
        showLists = showLists,
        showListsChange = { showLists = it },
        info = info,
        listDao = listDao,
        hostState = hostState,
        scope = scope,
    )

    ModalNavigationDrawer(
        drawerState = scaffoldState,
        drawerContent = {
            ModalDrawerSheet {
                MarkAsScreen(
                    drawerState = scaffoldState,
                    info = info,
                    chapters = chapters,
                    markAs = markAs
                )
            }
        }
    ) {
        OtakuScaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.zIndex(2f),
                    scrollBehavior = scrollBehavior,
                    title = { Text(info.title) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        DetailActions(
                            genericInfo = genericInfo,
                            scaffoldState = scaffoldState,
                            navController = navController,
                            scope = scope,
                            info = info,
                            isSaved = isSaved,
                            dao = dao,
                            isFavorite = isFavorite,
                            canNotify = canNotify,
                            notifyAction = notifyAction,
                            onReverseChaptersClick = { reverseChapters = !reverseChapters },
                            onShowLists = { showLists = true },
                            addToForLater = {
                                scope.launch {
                                    val result = AppConfig.forLaterUuid?.let {
                                        listDao.addToList(
                                            it,
                                            info.title,
                                            info.description,
                                            info.url,
                                            info.imageUrl,
                                            info.source.serviceName
                                        )
                                    } ?: false

                                    hostState.showSnackbar(
                                        getString(
                                            if (result) {
                                                Res.string.added_to_list
                                            } else {
                                                Res.string.already_in_list
                                            },
                                            getString(Res.string.for_later)
                                        ),
                                        withDismissAction = true
                                    )
                                }
                            }
                        )
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState) { data ->
                    Snackbar(snackbarData = data)
                }
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { p ->
            DetailsLandscapeContent(
                info = info,
                shareChapter = shareChapter,
                reverseChapters = reverseChapters,
                onReverse = { reverseChapters = it },
                description = description,
                onTranslateDescription = onTranslateDescription,
                chapters = chapters,
                markAs = markAs,
                isFavorite = isFavorite,
                onFavoriteClick = onFavoriteClick,
                listState = listState,
                isSaved = isSaved,
                showDownloadButton = showDownloadButton,
                canNotify = canNotify,
                notifyAction = notifyAction,
                onPaletteSet = onPaletteSet,
                scaffoldState = scaffoldState,
                modifier = Modifier.padding(p)
            )
        }
    }
}

@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3ExpressiveApi::class
)
@Composable
private fun DetailsLandscapeContent(
    info: KmpInfoModel,
    shareChapter: Boolean,
    isFavorite: Boolean,
    onFavoriteClick: (Boolean) -> Unit,
    isSaved: Boolean,
    markAs: (KmpChapterModel, Boolean) -> Unit,
    description: String,
    onTranslateDescription: (MutableState<Boolean>) -> Unit,
    chapters: List<ChapterWatched>,
    reverseChapters: Boolean,
    onReverse: (Boolean) -> Unit,
    scaffoldState: DrawerState,
    listState: LazyListState,
    showDownloadButton: () -> Boolean,
    canNotify: Boolean,
    notifyAction: () -> Unit,
    onPaletteSet: (Palette) -> Unit,
    modifier: Modifier = Modifier,
    notificationRepository: NotificationRepository = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val dao = LocalItemDao.current
    var showLists by remember { mutableStateOf(false) }

    AddToList(
        showLists = showLists,
        showListsChange = { showLists = it },
        info = info,
        listDao = LocalCustomListDao.current,
        hostState = null,
        scope = scope,
    )

    val state = rememberListDetailPaneScaffoldNavigator<Int>()

    ListDetailPaneScaffold(
        directive = state.scaffoldDirective,
        value = state.scaffoldValue,
        paneExpansionState = rememberPaneExpansionState(keyProvider = state.scaffoldValue),
        paneExpansionDragHandle = { state ->
            val interactionSource = remember { MutableInteractionSource() }
            VerticalDragHandle(
                interactionSource = interactionSource,
                modifier = Modifier.paneExpansionDraggable(
                    state = state,
                    minTouchTargetSize = LocalMinimumInteractiveComponentSize.current,
                    interactionSource = interactionSource,
                )
            )
        },
        listPane = {
            val b = MaterialTheme.colorScheme.surface
            val c = MaterialTheme.colorScheme.primary
            NormalOtakuScaffold(
                bottomBar = {
                    DetailBottomBar(
                        navController = LocalNavActions.current,
                        onShowLists = { showLists = true },
                        info = info,
                        customActions = {},
                        removeFromSaved = {
                            scope.launch(Dispatchers.IO) {
                                dao.getNotificationItemFlow(info.url)
                                    .firstOrNull()
                                    ?.let {
                                        dao.deleteNotification(it)
                                        notificationRepository.cancelNotification(it)
                                    }
                            }
                        },
                        isSaved = isSaved,
                        canNotify = canNotify,
                        notifyAction = notifyAction,
                        modifier = Modifier
                            .padding(LocalNavHostPadding.current)
                            .drawWithCache {
                                onDrawBehind {
                                    drawLine(
                                        b,
                                        Offset(0f, 8f),
                                        Offset(size.width, 8f),
                                        4 * density
                                    )
                                }
                            },
                        isFavorite = isFavorite,
                        onFavoriteClick = onFavoriteClick,
                        windowInsets = BottomAppBarDefaults.windowInsets
                    )
                },
                contentWindowInsets = WindowInsets.navigationBars,
                containerColor = Color.Transparent,
                modifier = Modifier.drawBehind { drawRect(Brush.verticalGradient(listOf(c, b))) }
            ) {
                Column(
                    modifier = Modifier
                        .padding(it)
                        .verticalScroll(rememberScrollState())
                ) {
                    DetailsHeader(
                        model = info,
                        isFavorite = isFavorite,
                        favoriteClick = onFavoriteClick,
                        onPaletteSet = onPaletteSet,
                        possibleDescription = {
                            if (info.description.isNotEmpty()) {
                                var descriptionVisibility by remember { mutableStateOf(false) }
                                Box {
                                    val progress = remember { mutableStateOf(false) }

                                    Text(
                                        description,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .combinedClickable(
                                                interactionSource = null,
                                                indication = ripple(),
                                                onClick = { descriptionVisibility = !descriptionVisibility },
                                                onLongClick = { onTranslateDescription(progress) }
                                            )
                                            .padding(horizontal = 4.dp)
                                            .fillMaxWidth()
                                            .animateContentSize()
                                    )

                                    if (progress.value) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
        detailPane = {
            val listOfChapters = remember(reverseChapters) {
                info.chapters.let { if (reverseChapters) it.reversed() else it }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxHeight(),
                state = listState
            ) {
                stickyHeader {
                    ButtonGroup(
                        overflowIndicator = { menuState ->
                            FilledIconButton(
                                onClick = {
                                    if (menuState.isExpanded) {
                                        menuState.dismiss()
                                    } else {
                                        menuState.show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Localized description",
                                )
                            }
                        },
                    ) {
                        clickableItem(
                            onClick = {
                                scope.launch {
                                    listState.animateScrollToItem(
                                        index = listOfChapters
                                            .indexOfFirst { r -> chapters.any { it.url == r.url } }
                                            .let { if (it == -1) listOfChapters.lastIndex else it },
                                    )
                                }
                            },
                            icon = { Icon(Icons.Default.SwipeDown, null) },
                            label = "Last Read"
                        )

                        toggleableItem(
                            checked = reverseChapters,
                            onCheckedChange = onReverse,
                            icon = { Icon(Icons.AutoMirrored.Filled.Sort, null) },
                            label = "Reverse"
                        )

                        clickableItem(
                            onClick = {
                                scope.launch {
                                    listState.animateScrollToItem(index = 0)
                                }
                            },
                            icon = { Icon(Icons.Default.VerticalAlignTop, null) },
                            label = "Top"
                        )

                        clickableItem(
                            onClick = { scope.launch { scaffoldState.open() } },
                            icon = { Icon(Icons.Default.Check, null) },
                            label = "Mark As..."
                        )
                    }
                }
                items(listOfChapters) { c ->
                    ChapterItem(
                        infoModel = info,
                        c = c,
                        read = chapters,
                        chapters = info.chapters,
                        shareChapter = shareChapter,
                        markAs = markAs,
                        showDownload = showDownloadButton
                    )
                }
            }
            /*LazyColumnScrollbar(
                state = listState,
                settings = ScrollbarSettings.Default.copy(
                    thumbThickness = 8.dp,
                    scrollbarPadding = 2.dp,
                    thumbUnselectedColor = MaterialTheme.colorScheme.primary,
                    thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = .6f),
                ),
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxHeight(),
                    state = listState
                ) {
                    items(listOfChapters) { c ->
                        ChapterItem(
                            infoModel = info,
                            c = c,
                            read = chapters,
                            chapters = info.chapters,
                            shareChapter = shareChapter,
                            markAs = markAs,
                            showDownload = showDownloadButton
                        )
                    }
                }
            }*/
        },
        modifier = modifier,
    )
}