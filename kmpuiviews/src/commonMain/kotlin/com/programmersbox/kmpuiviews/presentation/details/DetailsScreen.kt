package com.programmersbox.kmpuiviews.presentation.details


import kotlinx.coroutines.delay
import androidx.compose.animation.core.tween
import com.materialkolor.material3.ktx.animateColorScheme
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmpalette.color
import com.materialkolor.material3.dynamicColorScheme
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.LocalNavigationAnimatedScope
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.sharedItemKey
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.DetailsChapterSwipeBehavior
import com.programmersbox.datastore.DetailsChapterSwipeBehaviorHandle
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.rememberSwatchStyle
import com.programmersbox.datastore.rememberSwatchType
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpuiviews.ChapterDownloadUiState
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.notes.DetailsNotesViewModel
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import com.programmersbox.kmpuiviews.utils.LocalItemDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.sharedcomponents.qrcode.QrCodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.hadAnUpdate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun DetailsScreen(
    windowSize: WindowSizeClass,
    dao: ItemDao = LocalItemDao.current,
    details: DetailsViewModel = koinViewModel(),
) {
    CompositionLocalProvider(
        LocalDetailsSharedItemKey provides details.itemModel?.sharedItemKey(),
        LocalDetailsHandedOffCover provides details.handedOffCover,
    ) {
        DetailsScreenInternal(
            windowSize = windowSize,
            dao = dao,
            details = details
        )
    }
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

    // Keep the state shown on open until the enter transition settles, so only one header
    // carries the shared cover/title keys while they are in flight
    val navTransition = LocalNavigationAnimatedScope.current?.transition
    // Checked by state, not isRunning: the transition has not started yet on the first frame
    val entering = navTransition != null &&
            navTransition.currentState != EnterExitState.Visible &&
            navTransition.targetState == EnterExitState.Visible
    val openingState = remember { details.currentState }
    var enterSettled by remember { mutableStateOf(false) }
    LaunchedEffect(entering) { if (!entering) enterSettled = true }

    val contentTransition = updateTransition(
        targetState = if (enterSettled) details.currentState else openingState,
        label = ""
    )

    val seedColor = details
        .palette
        ?.let(paletteSwatchType.swatch)
        ?.color
        ?.takeIf { usePalette }
        ?: MaterialTheme.colorScheme.primary
    val isDark = when (themeSetting) {
        SystemThemeMode.FollowSystem -> isSystemInDarkTheme()
        SystemThemeMode.Day -> false
        SystemThemeMode.Night -> true
    }
    val isAmoled = isAmoledMode && (!usePalette || details.palette == null)
    val schemeInputs = SchemeInputs(seedColor, isDark, isAmoled, paletteStyle)

    // Building a dynamic scheme is expensive (hundreds of ms on low-end devices), so build it off the
    // main thread and keep the current scheme until it is ready
    val currentColorScheme = MaterialTheme.colorScheme
    // Start from the scheme built before a rotation so the palette doesn't drop out and animate back in
    val cachedScheme = details.colorScheme?.takeIf { it.first == schemeInputs }?.second
    var appliedSchemeInputs by remember { mutableStateOf(schemeInputs.takeIf { cachedScheme != null }) }
    val dynamicColorScheme by produceState(cachedScheme ?: currentColorScheme, schemeInputs) {
        if (appliedSchemeInputs == schemeInputs) return@produceState
        value = withContext(Dispatchers.Default) {
            dynamicColorScheme(
                seedColor = schemeInputs.seedColor,
                isDark = schemeInputs.isDark,
                isAmoled = schemeInputs.isAmoled,
                style = schemeInputs.style
            )
        }
        details.colorScheme = schemeInputs to value
        appliedSchemeInputs = schemeInputs
    }
    // Updated state so the reveal below sees the palette arrive, not the value from when it started
    val paletteApplied by rememberUpdatedState(
        !usePalette || (details.palette != null && appliedSchemeInputs == schemeInputs)
    )

    // Build the loaded screen one stage per frame instead of all in one long frame. The palette goes
    // first, while only the cover and title are showing, so every stage fades in already in its colors
    var revealStage by rememberSaveable { mutableIntStateOf(0) }
    // Restored already revealed (rotation): show everything without replaying the fades
    val revealAnimates = remember { revealStage < DetailsRevealStage.All }
    val loaded = contentTransition.targetState is DetailState.Success
    LaunchedEffect(loaded) {
        if (!loaded || revealStage >= DetailsRevealStage.All) return@LaunchedEffect
        // Don't hold the screen for a slow cover; the palette switches in whenever it arrives
        withTimeoutOrNull(PaletteWaitMillis) { snapshotFlow { paletteApplied }.first { it } }
        while (revealStage < DetailsRevealStage.All) {
            // Each stage gets its own frame, and a short gap so the cascade reads on fast devices too
            withFrameNanos { }
            delay(RevealStageGapMillis)
            revealStage++
        }
    }

    val shareChapter by handling.rememberShareChapter()

    MaterialTheme(
        // Only the change to the cover palette animates; earlier scheme changes switch in one step since
        // animating recomposes everything that reads colors on every frame
        colorScheme = if (details.palette != null) {
            animateColorScheme(colorScheme = dynamicColorScheme, animationSpec = { tween() })
        } else {
            dynamicColorScheme
        }
    ) {
        CompositionLocalProvider(
            LocalDetailsRevealStage provides revealStage,
            LocalDetailsRevealAnimates provides revealAnimates,
        ) {
            contentTransition.AnimatedContent(
                transitionSpec = {
                    when (val state = targetState) {
                        is DetailState.Success if state.info == (initialState as? DetailState.Success)?.info -> {
                            EnterTransition.None togetherWith ExitTransition.None
                        }

                        // Both headers show the same cover; keep the loading one opaque underneath so the
                        // cover doesn't dip dark halfway through the crossfade
                        is DetailState.Success if initialState is DetailState.Loading -> {
                            fadeIn() togetherWith ExitTransition.KeepUntilTransitionsFinished
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
                when (target) {
                    is DetailState.Error -> {
                        DetailError(
                            details = details,
                            state = target
                        )
                    }

                    DetailState.Loading -> {
                        DetailLoading(
                            details = details
                        )
                    }

                    is DetailState.Success -> {
                        val infoModel = target.info

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
                            state = target,
                            windowSize = windowSize,
                            shareChapter = shareChapter,
                            showDownload = showDownload,
                            detailsActions = DetailsActions(
                                onClick = { model ->
                                    scope.launch(Dispatchers.IO) {
                                        genericInfo.chapterOnClick(model, target.info.chapters, infoModel, navActions)
                                        insertRecent()
                                        heatMapDao.upsertHeatMap()
                                    }
                                },
                                onDownload = { model ->
                                    genericInfo.downloadChapter(model, target.info.chapters, infoModel, navActions)
                                    scope.launch(Dispatchers.IO) { insertRecent() }
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
                                favoriteAction = { details.favoriteAction(target.action) },
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
                                bookmarkedChapterUrls = details.bookmarkedChapterUrls,
                                selection = ChapterSelectionActions(
                                    selectedUrls = details.selectedChapterUrls,
                                    onToggle = details::toggleSelection,
                                    onSelectAll = details::selectAllChapters,
                                    onInvert = details::invertSelection,
                                    onClear = details::clearSelection,
                                    onMarkAs = details::markChaptersAs,
                                    onRestoreRead = details::restoreReadState,
                                    onSetBookmarked = details::setBookmarked,
                                    onRestoreBookmarks = details::restoreBookmarkState,
                                    onBatchDownload = { genericInfo.batchDownloadChapters(it, infoModel) },
                                    supportsBatchDownload = genericInfo.supportsBatchDownload,
                                )
                            )
                        )
                    }
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
    val selection: ChapterSelectionActions = ChapterSelectionActions(),
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

    // Shown while the header loads its cover: the card's handed-off cover, else the saved blur hash
    val coverPlaceholder = remember(details.imageBitmap, details.blurHash) {
        details.imageBitmap?.let(::BitmapPainter) ?: details.blurHash
    }

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
                blurHash = coverPlaceholder,
                // Keep the cover handed off by the card so the palette isn't rebuilt from the same image
                onBitmapSet = { if (details.imageBitmap == null) details.imageBitmap = it },
                detailsActions = detailsActions,
                notesVm = notesVm,
                downloadStates = details.downloadStates
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
                // Keep the cover handed off by the card so the palette isn't rebuilt from the same image
                onBitmapSet = { if (details.imageBitmap == null) details.imageBitmap = it },
                blurHash = coverPlaceholder,
                detailsActions = detailsActions,
                notesVm = notesVm,
                downloadStates = details.downloadStates
            )
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
    ) {
        PlaceHolderHeader(
            paddingValues = it,
            bitmapPainter = details.blurHash,
            imageUrl = details.itemModel?.imageUrl,
            title = details.itemModel?.title,
            coverBitmap = details.handedOffCover,
        )
    }
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
    readUrls: Set<String>,
    isBookmarked: Boolean,
    showDownload: () -> Boolean,
    swipeBehavior: DetailsChapterSwipeBehaviorHandle,
    detailsActions: DetailsActions,
    downloadUiState: ChapterDownloadUiState = ChapterDownloadUiState.None,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val updatedIsRead by rememberUpdatedState(c.url in readUrls)
    val updatedAnimated = updateTransition(updatedIsRead)

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
        enableDismissFromEndToStart = !selectionMode && swipeBehavior.detailsChapterSwipeBehaviorEndToStart != DetailsChapterSwipeBehavior.Nothing,
        enableDismissFromStartToEnd = !selectionMode && swipeBehavior.detailsChapterSwipeBehaviorStartToEnd != DetailsChapterSwipeBehavior.Nothing,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            // Only build the swipe hint while a swipe is happening; every row composes this otherwise
            if (direction == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
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
            onClick = { if (selectionMode) onToggleSelection() else detailsActions.onClick(c) },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleSelection()
            },
            leadingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible = selectionMode,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        ChapterSelectionIndicator(
                            isSelected = isSelected,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    ChapterUnreadDot(
                        transition = updatedAnimated,
                        size = if (selectionMode) 6.dp else 12.dp
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
                    AnimatedVisibility(
                        visible = !selectionMode,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    }
                }
            },
            selected = if (selectionMode) isSelected else !updatedIsRead,
            colors = if (selectionMode) {
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            },
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .semantics { selected = selectionMode && isSelected }
        )
    }
}

/** How long the loaded screen waits for the cover palette before revealing without it */
private const val PaletteWaitMillis = 300L

/** Minimum time between reveal stages */
private const val RevealStageGapMillis = 70L
