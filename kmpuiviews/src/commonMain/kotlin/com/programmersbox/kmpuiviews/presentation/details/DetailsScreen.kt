package com.programmersbox.kmpuiviews.presentation.details


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmpalette.color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.rememberDynamicMaterialThemeState
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.DetailsChapterSwipeBehavior
import com.programmersbox.datastore.DetailsChapterSwipeBehaviorHandle
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.rememberSwatchStyle
import com.programmersbox.datastore.rememberSwatchType
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.ChapterDownloadUiState
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.notes.DetailsNotesViewModel
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.optionsSheet
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.repository.QrCodeRepository
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import com.programmersbox.kmpuiviews.utils.LocalItemDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.hadAnUpdate
import otakuworld.kmpuiviews.generated.resources.markAs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun DetailsScreen(
    windowSize: WindowSizeClass,
    dao: ItemDao = LocalItemDao.current,
    details: DetailsViewModel = koinViewModel(),
) {
    DetailsScreenInternal(
        windowSize = windowSize,
        dao = dao,
        details = details
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class, ExperimentalTime::class
)
@Composable
private fun DetailsScreenInternal(
    //detailInfo: Screen.DetailsScreen.Details,
    windowSize: WindowSizeClass,
    dao: ItemDao = LocalItemDao.current,
    details: DetailsViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val genericInfo = koinInject<KmpGenericInfo>()
    val handling = LocalSettingsHandling.current
    val navActions = LocalNavActions.current
    val qrCodeRepository = koinInject<QrCodeRepository>()
    val historyDao = LocalHistoryDao.current
    val heatMapDao = koinInject<HeatMapDao>()
    val favoritesRepository: FavoritesRepository = koinInject()
    val dataStoreHandling = koinInject<DataStoreHandling>()
    val listDao = koinInject<ListRepository>()
    val notificationRepository = koinInject<NotificationRepository>()

    val showDownload by handling.rememberShowDownload()
    val usePalette by handling.rememberUsePalette()
    val isAmoledMode by handling.rememberIsAmoledMode()
    val themeSetting by handling.rememberSystemThemeMode()
    val paletteSwatchType by rememberSwatchType()
    val paletteStyle by rememberSwatchStyle()

    val dynamicColor = rememberDynamicMaterialThemeState(
        seedColor = details
            .palette
            ?.let(paletteSwatchType.swatch)
            ?.color
            ?.takeIf { usePalette }
            ?: MaterialTheme.colorScheme.primary,
        isDark = when (themeSetting) {
            SystemThemeMode.FollowSystem -> isSystemInDarkTheme()
            SystemThemeMode.Day -> false
            SystemThemeMode.Night -> true
        },
        isAmoled = isAmoledMode && (!usePalette || details.palette == null),
        style = paletteStyle
    )

    val shareChapter by handling.rememberShareChapter()

    DynamicMaterialTheme(
        state = dynamicColor,
        animate = true,
        animationSpec = tween()
    ) {
        AnimatedContent(
            targetState = details.currentState,
            label = "",
            transitionSpec = {
                when (val state = targetState) {
                    is DetailState.Success if state.info == (initialState as? DetailState.Success)?.info -> {
                        EnterTransition.None togetherWith ExitTransition.None
                    }

                    else -> fadeIn() togetherWith fadeOut()
                }
            },
            contentKey = {
                when (it) {
                    is DetailState.Success -> it.info
                    else -> it
                }
            }
        ) { target ->
            when (val state = target) {
                is DetailState.Error -> {
                    DetailError(
                        details = details,
                        state = state
                    )
                }

                DetailState.Loading -> {
                    DetailLoading(
                        details = details
                    )
                }

                is DetailState.Success -> {
                    val infoModel = state.info

                    suspend fun insertRecent() {
                        if (
                            favoritesRepository.isIncognito(infoModel.source.serviceName) ||
                            favoritesRepository.isIncognito(infoModel.url)
                        ) return

                        historyDao.insertRecentlyViewed(
                            RecentModel(
                                title = infoModel.title,
                                url = infoModel.url,
                                imageUrl = infoModel.imageUrl,
                                description = infoModel.description,
                                source = infoModel.source.serviceName,
                                timestamp = Clock.System.now().toEpochMilliseconds()
                            )
                        )

                        val save = dataStoreHandling.historySave.get()
                        if (save != -1) historyDao.removeOldData(save)
                    }

                    DetailContent(
                        dao = dao,
                        details = details,
                        scope = scope,
                        state = state,
                        windowSize = windowSize,
                        shareChapter = shareChapter,
                        showDownload = showDownload,
                        detailsActions = DetailsActions(
                            onClick = { model ->
                                scope.launch(Dispatchers.IO) {
                                    genericInfo.chapterOnClick(model, state.info.chapters, infoModel, navActions)
                                    insertRecent()
                                    heatMapDao.upsertHeatMap()
                                }
                            },
                            onDownload = { model ->
                                genericInfo.downloadChapter(model, state.info.chapters, infoModel, navActions)
                                scope.launch(Dispatchers.IO) { insertRecent() }
                                if (!details.chapters.fastAny { it.url == model.url }) details.markAs(model, true)
                            },
                            onDeleteDownload = { model ->
                                genericInfo.deleteDownloadedChapter(model, infoModel)
                            },
                            shareChapter = {
                                scope.launch {
                                    qrCodeRepository.shareUrl(
                                        url = it.url,
                                        title = it.name
                                    )
                                }
                            },
                            markAsRead = { model, read -> details.markAs(model, read) },
                            favoriteAction = { details.favoriteAction(state.action) },
                            notifyChange = { details.toggleNotify() },
                            globalSearch = { navActions.globalSearch(infoModel.title) },
                            addToList = { item ->
                                listDao.addToList(
                                    item.item.uuid,
                                    infoModel.title,
                                    infoModel.description,
                                    infoModel.url,
                                    infoModel.imageUrl,
                                    infoModel.source.serviceName
                                )
                            },
                            addToSaved = {
                                scope.launch(Dispatchers.IO) {
                                    dao.insertNotification(
                                        NotificationItem(
                                            id = infoModel.hashCode(),
                                            url = infoModel.url,
                                            summaryText = getString(
                                                Res.string.hadAnUpdate,
                                                infoModel.title,
                                                infoModel.chapters.firstOrNull()?.name.orEmpty()
                                            ),
                                            notiTitle = infoModel.title,
                                            imageUrl = infoModel.imageUrl,
                                            source = infoModel.source.serviceName,
                                            contentTitle = infoModel.title
                                        )
                                    )
                                }
                            },
                            removeFromSaved = {
                                scope.launch(Dispatchers.IO) {
                                    dao.getNotificationItemFlow(infoModel.url)
                                        .firstOrNull()
                                        ?.let {
                                            dao.deleteNotification(it)
                                            notificationRepository.cancelNotification(it)
                                        }
                                }
                            },
                            rereadClick = details::reread,
                            bookmarkChapter = { details.toggleBookmark(it) },
                            bookmarkedChapterUrls = details.bookmarkedChapterUrls
                        )
                    )
                }
            }
        }
    }
}

data class DetailsActions(
    val onClick: (KmpChapterModel) -> Unit,
    val onDownload: (KmpChapterModel) -> Unit,
    val onDeleteDownload: (KmpChapterModel) -> Unit,
    val shareChapter: (KmpChapterModel) -> Unit,
    val markAsRead: (KmpChapterModel, Boolean) -> Unit,
    val favoriteAction: () -> Unit,
    val notifyChange: () -> Unit,
    val globalSearch: () -> Unit,
    val addToList: suspend (CustomList) -> Boolean,
    val addToSaved: suspend () -> Unit,
    val removeFromSaved: suspend () -> Unit,
    val rereadClick: () -> Unit,
    val bookmarkChapter: (KmpChapterModel) -> Unit = {},
    val bookmarkedChapterUrls: Set<String> = emptySet(),
)

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMediaQueryApi::class
)
@Composable
private fun DetailContent(
    dao: ItemDao,
    details: DetailsViewModel,
    scope: CoroutineScope,
    state: DetailState.Success,
    windowSize: WindowSizeClass,
    shareChapter: Boolean,
    showDownload: Boolean,
    detailsActions: DetailsActions,
    notesVm: DetailsNotesViewModel = koinViewModel { parametersOf(state.info.url, state.info.title) },
) {
    val isSaved by dao
        .doesNotificationExistFlow(state.info.url)
        .collectAsStateWithLifecycle(false)

    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> {
            DetailsViewLandscape(
                info = state.info,
                isSaved = isSaved,
                shareChapter = shareChapter,
                isFavorite = state.action is DetailFavoriteAction.Remove,
                chapters = details.chapters,
                description = details.description,
                onTranslateDescription = details::translateDescription,
                showDownloadButton = { showDownload },
                canNotify = details.dbModel?.shouldCheckForUpdate == true,
                onPaletteSet = { details.palette = it },
                blurHash = details.blurHash,
                onBitmapSet = { details.imageBitmap = it },
                detailsActions = detailsActions,
                notesVm = notesVm,
            )
        }

        else -> {
            DetailsView(
                info = state.info,
                isSaved = isSaved,
                shareChapter = shareChapter,
                isFavorite = state.action is DetailFavoriteAction.Remove,
                chapters = details.chapters,
                description = details.description,
                onTranslateDescription = details::translateDescription,
                showDownloadButton = { showDownload },
                canNotify = details.dbModel?.shouldCheckForUpdate == true,
                onPaletteSet = { details.palette = it },
                onBitmapSet = { details.imageBitmap = it },
                blurHash = details.blurHash,
                detailsActions = detailsActions,
                notesVm = notesVm,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAsScreen(
    drawerState: DrawerState,
    info: KmpInfoModel,
    chapters: List<ChapterWatched>,
    markAs: (KmpChapterModel, Boolean) -> Unit,
) {
    val scrollBehaviorMarkAs = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.markAs)) },

                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                scrollBehavior = scrollBehaviorMarkAs
            )
        },
        modifier = Modifier.nestedScroll(scrollBehaviorMarkAs.nestedScrollConnection)
    ) { p ->
        LazyColumn(
            contentPadding = p,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(info.chapters) { c ->
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = null,
                            indication = ripple()
                        ) { markAs(c, !chapters.fastAny { it.url == c.url }) },
                ) {
                    ListItem(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        headlineContent = { Text(c.name) },
                        leadingContent = {
                            Checkbox(
                                checked = chapters.fastAny { it.url == c.url },
                                onCheckedChange = { b -> markAs(c, b) },
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DetailLoading(
    details: DetailsViewModel,
    uriHandler: UriHandler = LocalUriHandler.current,
) {
    val qrCodeRepository = koinInject<QrCodeRepository>()
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.zIndex(2f),
                title = {
                    Text(
                        details.itemModel?.title.orEmpty(),
                        maxLines = 1
                    )
                },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                qrCodeRepository.shareUrl(
                                    details.itemModel?.url.orEmpty(),
                                    details.itemModel?.title.orEmpty()
                                )
                            }
                        }
                    ) { Icon(Icons.Default.Share, null) }

                    IconButton(
                        onClick = {
                            details.itemModel?.url?.let { uriHandler.openUri(it) }
                        }
                    ) { Icon(Icons.Default.OpenInBrowser, null) }

                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) }
                },
            )
        }
    ) { PlaceHolderHeader(it, details.blurHash) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailError(
    details: DetailsViewModel,
    uriHandler: UriHandler = LocalUriHandler.current,
    state: DetailState.Error,
) {
    val qrCodeRepository = koinInject<QrCodeRepository>()
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.zIndex(2f),
                title = {
                    Text(
                        details.itemModel?.title.orEmpty(),
                        maxLines = 1
                    )
                },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                qrCodeRepository.shareUrl(
                                    details.itemModel?.url.orEmpty(),
                                    details.itemModel?.title.orEmpty()
                                )
                            }
                        }
                    ) { Icon(Icons.Default.Share, null) }

                    IconButton(
                        onClick = {
                            details.itemModel?.url?.let { uriHandler.openUri(it) }
                        }
                    ) { Icon(Icons.Default.OpenInBrowser, null) }

                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) }
                },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Card {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(Icons.Default.Warning, null)
                    Text("Something happened!")
                    Text(state.e.message.orEmpty())
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3Api
@Composable
fun ChapterItem(
    c: KmpChapterModel,
    read: List<ChapterWatched>,
    isBookmarked: Boolean,
    showDownload: () -> Boolean,
    swipeBehavior: DetailsChapterSwipeBehaviorHandle,
    detailsActions: DetailsActions,
    downloadUiState: ChapterDownloadUiState = ChapterDownloadUiState.None,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val hasBeenRead by remember(read) { derivedStateOf { read.fastAny { it.url == c.url } } }
    val updatedIsRead by rememberUpdatedState(hasBeenRead)
    val updatedAnimated = updateTransition(updatedIsRead)

    var options by chapterItemOptions(
        chapter = c,
        hasBeenRead = updatedIsRead,
        isBookmarked = isBookmarked,
        showDownload = showDownload,
        downloadUiState = downloadUiState,
        onOpen = { detailsActions.onClick(c) },
        downloadChapter = { detailsActions.onDownload(c) },
        deleteDownload = { detailsActions.onDeleteDownload(c) },
        markAsRead = { detailsActions.markAsRead(c, !updatedIsRead) },
        shareChapter = { detailsActions.shareChapter(c) },
        bookmarkChapter = { detailsActions.bookmarkChapter(c) }
    )

    fun swipeBehavior(behavior: DetailsChapterSwipeBehavior) {
        when (behavior) {
            DetailsChapterSwipeBehavior.MarkAsRead -> detailsActions.markAsRead(c, !updatedIsRead)
            DetailsChapterSwipeBehavior.Read -> detailsActions.onClick(c)
            DetailsChapterSwipeBehavior.Nothing -> {}
        }
    }

    val dismissState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = dismissState,
        onDismiss = { value ->
            scope.launch {
                launch {
                    when (value) {
                        SwipeToDismissBoxValue.EndToStart -> swipeBehavior(swipeBehavior.detailsChapterSwipeBehaviorEndToStart)
                        SwipeToDismissBoxValue.StartToEnd -> swipeBehavior(swipeBehavior.detailsChapterSwipeBehaviorStartToEnd)
                        SwipeToDismissBoxValue.Settled -> {}
                    }
                }
                dismissState.reset()
            }
        },
        enableDismissFromEndToStart = swipeBehavior.detailsChapterSwipeBehaviorEndToStart != DetailsChapterSwipeBehavior.Nothing,
        enableDismissFromStartToEnd = swipeBehavior.detailsChapterSwipeBehaviorStartToEnd != DetailsChapterSwipeBehavior.Nothing,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            fun iconSwipeBehavior(behavior: DetailsChapterSwipeBehavior) = when (behavior) {
                DetailsChapterSwipeBehavior.MarkAsRead -> Icons.Default.Check
                DetailsChapterSwipeBehavior.Read -> Icons.Default.PlayArrow
                DetailsChapterSwipeBehavior.Nothing -> Icons.Default.Cancel
            }

            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> iconSwipeBehavior(swipeBehavior.detailsChapterSwipeBehaviorStartToEnd)
                SwipeToDismissBoxValue.EndToStart -> iconSwipeBehavior(swipeBehavior.detailsChapterSwipeBehaviorEndToStart)
                else -> Icons.Default.Cancel
            }
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 2f,
                label = ""
            )

            fun textSwipeBehavior(behavior: DetailsChapterSwipeBehavior) = when (behavior) {
                DetailsChapterSwipeBehavior.MarkAsRead -> if (updatedIsRead) "Mark as unread" else "Mark as read"
                DetailsChapterSwipeBehavior.Read -> "Read"
                DetailsChapterSwipeBehavior.Nothing -> "Cancel"
            }

            val textIndication = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> textSwipeBehavior(swipeBehavior.detailsChapterSwipeBehaviorStartToEnd)
                SwipeToDismissBoxValue.EndToStart -> textSwipeBehavior(swipeBehavior.detailsChapterSwipeBehaviorEndToStart)
                else -> "Cancel"

            }

            Box(
                contentAlignment = alignment,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                        .padding(16.dp)
                ) {
                    Text(textIndication)
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        ListItem(
            onClick = { detailsActions.onClick(c) },
            onLongClick = { options = true },
            leadingContent = {
                updatedAnimated.AnimatedVisibility(
                    { !it },
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            },
            content = {
                Text(
                    c.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            overlineContent = c
                .uploaded
                .takeIf { it.isNotEmpty() }
                ?.let { { Text(it) } },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (downloadUiState) {
                        is ChapterDownloadUiState.Downloading -> CircularWavyProgressIndicator(
                            progress = { downloadUiState.fraction },
                            stroke = Stroke(width = 2f),
                            modifier = Modifier.size(24.dp),
                        )

                        ChapterDownloadUiState.Queued -> CircularWavyProgressIndicator(
                            stroke = Stroke(width = 2f),
                            modifier = Modifier.size(24.dp),
                        )

                        ChapterDownloadUiState.Downloaded -> Icon(
                            Icons.Default.DownloadDone,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )

                        ChapterDownloadUiState.None -> {}
                    }
                    IconButton(onClick = { detailsActions.bookmarkChapter(c) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark chapter",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { detailsActions.shareChapter(c) }) {
                        Icon(Icons.Default.Share, null)
                    }
                }
            },
            selected = !updatedIsRead,
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
        )
    }
}

@ExperimentalMaterial3ExpressiveApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chapterItemOptions(
    chapter: KmpChapterModel,
    hasBeenRead: Boolean,
    isBookmarked: Boolean,
    showDownload: () -> Boolean,
    downloadUiState: ChapterDownloadUiState,
    onOpen: () -> Unit,
    downloadChapter: () -> Unit,
    deleteDownload: () -> Unit,
    markAsRead: () -> Unit,
    shareChapter: () -> Unit,
    bookmarkChapter: () -> Unit,
) = optionsSheet(
    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
) {

    val colors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )

    val canDownload = chapter.source.canDownload && showDownload()
    val isDownloaded = downloadUiState == ChapterDownloadUiState.Downloaded
    val totalCount = if (canDownload) 5 else 4

    ListItem(
        headlineContent = { Text(chapter.name) },
    )

    SegmentedListItem(
        onClick = {
            dismiss()
            onOpen()
        },
        content = { Text("Read") },
        colors = colors,
        shapes = ListItemDefaults.segmentedShapes(
            index = 0,
            count = totalCount
        )
    )

    if (canDownload) {
        SegmentedListItem(
            onClick = {
                dismiss()
                if (isDownloaded) deleteDownload() else downloadChapter()
            },
            content = { Text(if (isDownloaded) "Delete" else "Download") },
            colors = colors,
            shapes = ListItemDefaults.segmentedShapes(
                index = 1,
                count = totalCount
            )
        )
    }

    SegmentedListItem(
        content = { Text("Mark as read") },
        colors = colors,
        trailingContent = { Checkbox(checked = hasBeenRead, onCheckedChange = null) },
        checked = hasBeenRead,
        onCheckedChange = { markAsRead() },
        shapes = ListItemDefaults.segmentedShapes(
            index = if (canDownload) 2 else 1,
            count = totalCount
        )
    )

    SegmentedListItem(
        onClick = {
            dismiss()
            bookmarkChapter()
        },
        content = { Text(if (isBookmarked) "Remove bookmark" else "Bookmark") },
        leadingContent = {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = null,
            )
        },
        colors = colors,
        shapes = ListItemDefaults.segmentedShapes(
            index = if (canDownload) 3 else 2,
            count = totalCount
        )
    )

    SegmentedListItem(
        onClick = {
            dismiss()
            shareChapter()
        },
        content = { Text("Share") },
        colors = colors,
        shapes = ListItemDefaults.segmentedShapes(
            index = if (canDownload) 4 else 3,
            count = totalCount
        )
    )
}