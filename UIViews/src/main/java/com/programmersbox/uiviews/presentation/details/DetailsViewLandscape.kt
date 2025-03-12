package com.programmersbox.uiviews.presentation.details

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.kmpalette.palette.graphics.Palette
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.uiviews.presentation.components.OtakuScaffold
import com.programmersbox.uiviews.presentation.lists.calculateStandardPaneScaffoldDirective
import com.programmersbox.uiviews.repository.NotificationRepository
import com.programmersbox.uiviews.theme.LocalCustomListDao
import com.programmersbox.uiviews.theme.LocalItemDao
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.utils.NotificationLogo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun DetailsViewLandscape(
    info: InfoModel,
    isSaved: Boolean,
    shareChapter: Boolean,
    chapters: List<ChapterWatched>,
    isFavorite: Boolean,
    onFavoriteClick: (Boolean) -> Unit,
    markAs: (ChapterModel, Boolean) -> Unit,
    logo: NotificationLogo,
    description: String,
    onTranslateDescription: (MutableState<Boolean>) -> Unit,
    showDownloadButton: () -> Boolean,
    canNotify: Boolean,
    notifyAction: () -> Unit,
    onPaletteSet: (Palette) -> Unit,
) {
    val dao = LocalItemDao.current
    val listDao = LocalCustomListDao.current
    val genericInfo = LocalGenericInfo.current
    val navController = LocalNavController.current
    val context = LocalContext.current

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
        context = context
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
                InsetSmallTopAppBar(
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
                            context = context,
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
                                    val result = OtakuApp.forLaterUuid?.let {
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
                                        context.getString(
                                            if (result) {
                                                R.string.added_to_list
                                            } else {
                                                R.string.already_in_list
                                            },
                                            context.getString(R.string.for_later)
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
                logo = logo,
                reverseChapters = reverseChapters,
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
                modifier = Modifier.padding(p)
            )
        }
    }
}

@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
private fun DetailsLandscapeContent(
    info: InfoModel,
    shareChapter: Boolean,
    isFavorite: Boolean,
    onFavoriteClick: (Boolean) -> Unit,
    isSaved: Boolean,
    markAs: (ChapterModel, Boolean) -> Unit,
    description: String,
    onTranslateDescription: (MutableState<Boolean>) -> Unit,
    chapters: List<ChapterWatched>,
    logo: NotificationLogo,
    reverseChapters: Boolean,
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
        context = LocalContext.current
    )

    val windowSize = with(LocalDensity.current) {
        currentWindowSize().toSize().toDpSize()
    }
    val windowSizeClass = remember(windowSize) { WindowSizeClass.calculateFromSize(windowSize) }

    val state = rememberListDetailPaneScaffoldNavigator<Int>(
        scaffoldDirective = calculateStandardPaneScaffoldDirective(
            currentWindowAdaptiveInfo(),
            windowSizeClass = windowSizeClass
        )
    )

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
                    semanticsProperties = state.defaultDragHandleSemantics()
                )
            )
        },
        listPane = {
            val b = MaterialTheme.colorScheme.surface
            val c = MaterialTheme.colorScheme.primary
            NormalOtakuScaffold(
                bottomBar = {
                    DetailBottomBar(
                        navController = LocalNavController.current,
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
                        containerColor = c,
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
                        logo = painterResource(id = logo.notificationId),
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
                                        modifier = Modifier
                                            .combinedClickable(
                                                interactionSource = null,
                                                indication = ripple(),
                                                onClick = { descriptionVisibility = !descriptionVisibility },
                                                onLongClick = { onTranslateDescription(progress) }
                                            )
                                            .padding(horizontal = 4.dp)
                                            .fillMaxWidth()
                                            .animateContentSize(),
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
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
            LazyColumnScrollbar(
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
            }
        },
        modifier = modifier,
    )
}