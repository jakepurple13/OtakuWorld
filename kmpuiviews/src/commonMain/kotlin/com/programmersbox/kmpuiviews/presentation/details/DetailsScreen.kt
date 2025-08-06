package com.programmersbox.kmpuiviews.presentation.details


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.rememberSwatchStyle
import com.programmersbox.datastore.rememberSwatchType
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.optionsSheet
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.QrCodeRepository
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import com.programmersbox.kmpuiviews.utils.LocalItemDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
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
    ExperimentalAnimationApi::class
)
@Composable
private fun DetailsScreenInternal(
    //detailInfo: Screen.DetailsScreen.Details,
    windowSize: WindowSizeClass,
    dao: ItemDao = LocalItemDao.current,
    details: DetailsViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val handling = LocalSettingsHandling.current

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
                    DetailContent(
                        dao = dao,
                        details = details,
                        scope = scope,
                        state = state,
                        windowSize = windowSize,
                        shareChapter = shareChapter,
                        showDownload = showDownload
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    dao: ItemDao,
    details: DetailsViewModel,
    scope: CoroutineScope,
    state: DetailState.Success,
    windowSize: WindowSizeClass,
    shareChapter: Boolean,
    showDownload: Boolean,
) {
    val isSaved by dao
        .doesNotificationExistFlow(state.info.url)
        .collectAsStateWithLifecycle(false)

    if (windowSize.widthSizeClass == WindowWidthSizeClass.Expanded) {
        DetailsViewLandscape(
            info = state.info,
            isSaved = isSaved,
            shareChapter = shareChapter,
            isFavorite = state.action is DetailFavoriteAction.Remove,
            onFavoriteClick = { details.favoriteAction(state.action) },
            chapters = details.chapters,
            markAs = details::markAs,
            description = details.description,
            onTranslateDescription = details::translateDescription,
            showDownloadButton = { showDownload },
            canNotify = details.dbModel?.shouldCheckForUpdate == true,
            notifyAction = { scope.launch { details.toggleNotify() } },
            onPaletteSet = { details.palette = it }
        )
    } else {
        DetailsView(
            info = state.info,
            isSaved = isSaved,
            shareChapter = shareChapter,
            isFavorite = state.action is DetailFavoriteAction.Remove,
            onFavoriteClick = { details.favoriteAction(state.action) },
            chapters = details.chapters,
            markAs = details::markAs,
            description = details.description,
            onTranslateDescription = details::translateDescription,
            showDownloadButton = { showDownload },
            canNotify = details.dbModel?.shouldCheckForUpdate == true,
            notifyAction = { scope.launch { details.toggleNotify() } },
            onPaletteSet = { details.palette = it },
            onBitmapSet = { details.imageBitmap = it },
            blurHash = details.blurHash
        )
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
    infoModel: KmpInfoModel,
    c: KmpChapterModel,
    read: List<ChapterWatched>,
    chapters: List<KmpChapterModel>,
    shareChapter: Boolean,
    showDownload: () -> Boolean,
    markAs: (KmpChapterModel, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val genericInfo = koinInject<KmpGenericInfo>()
    val qrCodeRepository = koinInject<QrCodeRepository>()
    val historyDao = LocalHistoryDao.current
    val heatMapDao = koinInject<HeatMapDao>()
    val favoritesRepository: FavoritesRepository = koinInject()
    val navController = LocalNavActions.current
    val dataStoreHandling = koinInject<DataStoreHandling>()
    val settingsHandling = koinInject<NewSettingsHandling>()
    val swipeBehavior by settingsHandling.detailsChapterSwipeBehavior.rememberPreference()
    val scope = rememberCoroutineScope()

    fun insertRecent() {
        scope.launch(Dispatchers.IO) {
            if (
                favoritesRepository.isIncognito(infoModel.source.serviceName) ||
                favoritesRepository.isIncognito(infoModel.url)
            ) return@launch

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
    }

    val hasBeenRead by remember(read) { derivedStateOf { read.fastAny { it.url == c.url } } }
    val updatedIsRead by rememberUpdatedState(hasBeenRead)

    fun chapterClick() {
        genericInfo.chapterOnClick(c, chapters, infoModel, navController)
        insertRecent()
        if (!updatedIsRead) markAs(c, true)
        scope.launch(Dispatchers.IO) { heatMapDao.upsertHeatMap() }
    }

    var options by chapterItemOptions(
        chapter = c,
        hasBeenRead = updatedIsRead,
        showDownload = showDownload,
        onOpen = ::chapterClick,
        downloadChapter = {
            genericInfo.downloadChapter(c, chapters, infoModel, navController)
            insertRecent()
            if (!updatedIsRead) markAs(c, true)
        },
        markAsRead = { markAs(c, !updatedIsRead) },
        shareChapter = {
            scope.launch {
                qrCodeRepository.shareUrl(
                    url = c.url,
                    title = c.name
                )
            }
        }
    )

    fun swipeBehavior(behavior: DetailsChapterSwipeBehavior) {
        when (behavior) {
            DetailsChapterSwipeBehavior.MarkAsRead -> markAs(c, !updatedIsRead)
            DetailsChapterSwipeBehavior.Read -> chapterClick()
            DetailsChapterSwipeBehavior.Nothing -> {}
        }
    }

    val dismissState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = dismissState,
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> swipeBehavior(swipeBehavior.detailsChapterSwipeBehaviorEndToStart)
                SwipeToDismissBoxValue.StartToEnd -> swipeBehavior(swipeBehavior.detailsChapterSwipeBehaviorStartToEnd)
                SwipeToDismissBoxValue.Settled -> {}
            }
            dismissState.reset()
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
                DetailsChapterSwipeBehavior.MarkAsRead -> "Mark as read"
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
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
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    indication = ripple(),
                    interactionSource = null,
                    onLongClick = { options = true },
                    onClick = {
                        //markAs(c, !updatedIsRead)
                        chapterClick()
                    }
                ),
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                //if (shareChapter) {
                ListItem(
                    leadingContent = {
                        Checkbox(
                            checked = updatedIsRead,
                            onCheckedChange = { b -> markAs(c, b) },
                        )
                    },
                    headlineContent = {
                        Text(
                            c.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    overlineContent = { Text(c.uploaded) },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    qrCodeRepository.shareUrl(
                                        url = c.url,
                                        title = c.name
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                null,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                )
                /*} else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = updatedIsRead,
                            onCheckedChange = { b -> markAs(c, b) },
                        )

                        Text(
                            c.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }*/

                /*Text(
                    c.uploaded,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp)
                        .padding(4.dp)
                )*/

                /*Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp)
                ) {
                    if (infoModel.source.canPlay) {
                        OutlinedButton(
                            onClick = ::chapterClick,
                            shapes = ButtonDefaults.shapes(),
                            border = BorderStroke(1.dp, LocalContentColor.current),
                            modifier = Modifier
                                .weight(1f, true)
                                .padding(horizontal = 4.dp)
                        ) {
                            Column {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    "Play",
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )
                                Text(
                                    stringResource(Res.string.read),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }

                    if (infoModel.source.canDownload && showDownload()) {
                        OutlinedButton(
                            onClick = {
                                genericInfo.downloadChapter(c, chapters, infoModel, navController)
                                insertRecent()
                                if (!updatedIsRead) markAs(c, true)
                            },
                            border = BorderStroke(1.dp, LocalContentColor.current),
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .weight(1f, true)
                                .padding(horizontal = 4.dp)
                        ) {
                            Column {
                                Icon(
                                    Icons.Default.Download,
                                    "Download",
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )
                                Text(
                                    stringResource(Res.string.download_chapter),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }*/
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chapterItemOptions(
    chapter: KmpChapterModel,
    hasBeenRead: Boolean,
    showDownload: () -> Boolean,
    onOpen: () -> Unit,
    downloadChapter: () -> Unit,
    markAsRead: () -> Unit,
    shareChapter: () -> Unit,
) = optionsSheet {

    ListItem(
        headlineContent = { Text(chapter.name) },
    )

    HorizontalDivider()

    OptionsItem(
        title = "Read",
        onClick = {
            dismiss()
            onOpen()
        }
    )

    if (chapter.source.canDownload && showDownload()) {
        OptionsItem(
            title = "Download",
            onClick = {
                dismiss()
                downloadChapter()
            }
        )
    }

    OptionsItem(
        title = "Mark as read",
        onClick = markAsRead,
        trailingContent = {
            Checkbox(
                checked = hasBeenRead,
                onCheckedChange = { markAsRead() }
            )
        }
    )

    OptionsItem(
        title = "Share",
        onClick = {
            dismiss()
            shareChapter()
        }
    )
}