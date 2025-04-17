package com.programmersbox.uiviews.presentation.details

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmpalette.color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.rememberDynamicMaterialThemeState
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.SystemThemeMode
import com.programmersbox.uiviews.datastore.rememberSwatchStyle
import com.programmersbox.uiviews.datastore.rememberSwatchType
import com.programmersbox.uiviews.presentation.components.OtakuScaffold
import com.programmersbox.uiviews.repository.FavoritesRepository
import com.programmersbox.uiviews.theme.LocalHistoryDao
import com.programmersbox.uiviews.theme.LocalItemDao
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.findActivity
import com.programmersbox.uiviews.utils.launchCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun DetailsScreen(
    logo: NotificationLogo,
    windowSize: WindowSizeClass,
    dao: ItemDao = LocalItemDao.current,
    details: DetailsViewModel = koinViewModel(),
) {
    DetailsScreenInternal(
        logo = logo,
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
    logo: NotificationLogo,
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
        seedColor = details.palette
            ?.let(paletteSwatchType.swatch)
            ?.color
            ?.takeIf { usePalette }
            ?: MaterialTheme.colorScheme.primary,
        isDark = when (themeSetting) {
            SystemThemeMode.FollowSystem -> isSystemInDarkTheme()
            SystemThemeMode.Day -> false
            SystemThemeMode.Night -> true
            else -> isSystemInDarkTheme()
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
                        logo = logo,
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
    logo: NotificationLogo,
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
            logo = logo,
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
            logo = logo,
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
    info: InfoModel,
    chapters: List<ChapterWatched>,
    markAs: (ChapterModel, Boolean) -> Unit,
) {
    val scrollBehaviorMarkAs = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()

    OtakuScaffold(
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(stringResource(id = R.string.markAs)) },

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
    localContext: Context = LocalContext.current,
    uriHandler: UriHandler = LocalUriHandler.current,
) {
    Scaffold(
        topBar = {
            InsetSmallTopAppBar(
                modifier = Modifier.zIndex(2f),
                title = {
                    Text(
                        details.itemModel?.title.orEmpty(),
                        maxLines = 1
                    )
                },
                navigationIcon = { BackButton() },
                actions = {
                    val shareItem = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) {}
                    IconButton(
                        onClick = {
                            shareItem.launchCatching(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, details.itemModel?.url.orEmpty())
                                        putExtra(Intent.EXTRA_TITLE, details.itemModel?.title.orEmpty())
                                    },
                                    localContext.getString(R.string.share_item, details.itemModel?.title.orEmpty())
                                )
                            )
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
    localContext: Context = LocalContext.current,
    uriHandler: UriHandler = LocalUriHandler.current,
    state: DetailState.Error,
) {
    Scaffold(
        topBar = {
            InsetSmallTopAppBar(
                modifier = Modifier.zIndex(2f),
                title = {
                    Text(
                        details.itemModel?.title.orEmpty(),
                        maxLines = 1
                    )
                },
                navigationIcon = { BackButton() },
                actions = {
                    val shareItem = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) {}
                    IconButton(
                        onClick = {
                            shareItem.launchCatching(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, details.itemModel?.url.orEmpty())
                                        putExtra(Intent.EXTRA_TITLE, details.itemModel?.title.orEmpty())
                                    },
                                    localContext.getString(R.string.share_item, details.itemModel?.title.orEmpty())
                                )
                            )
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

@ExperimentalMaterial3Api
@Composable
fun ChapterItem(
    infoModel: InfoModel,
    c: ChapterModel,
    read: List<ChapterWatched>,
    chapters: List<ChapterModel>,
    shareChapter: Boolean,
    showDownload: () -> Boolean,
    markAs: (ChapterModel, Boolean) -> Unit,
) {
    val historyDao = LocalHistoryDao.current
    val favoritesRepository: FavoritesRepository = koinInject()
    val navController = LocalNavController.current
    val genericInfo = LocalGenericInfo.current
    val context = LocalContext.current
    val dataStoreHandling = koinInject<DataStoreHandling>()
    val scope = rememberCoroutineScope()

    fun insertRecent() {
        scope.launch(Dispatchers.IO) {
            if (favoritesRepository.isIncognito(infoModel.source.serviceName)) return@launch
            historyDao.insertRecentlyViewed(
                RecentModel(
                    title = infoModel.title,
                    url = infoModel.url,
                    imageUrl = infoModel.imageUrl,
                    description = infoModel.description,
                    source = infoModel.source.serviceName,
                    timestamp = System.currentTimeMillis()
                )
            )
            val save = runBlocking { dataStoreHandling.historySave.get() }
            if (save != -1) historyDao.removeOldData(save)
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = ripple(),
                interactionSource = null,
            ) { markAs(c, !read.fastAny { it.url == c.url }) },
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            if (shareChapter) {
                ListItem(
                    leadingContent = {
                        Checkbox(
                            checked = read.fastAny { it.url == c.url },
                            onCheckedChange = { b -> markAs(c, b) },
                        )
                    },
                    headlineContent = {
                        Text(
                            c.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    trailingContent = {
                        val shareItem = rememberLauncherForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) {}
                        IconButton(
                            onClick = {
                                shareItem.launchCatching(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, c.url)
                                            putExtra(Intent.EXTRA_TITLE, c.name)
                                        },
                                        context.getString(R.string.share_item, c.name)
                                    )
                                )
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
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = read.fastAny { it.url == c.url },
                        onCheckedChange = { b -> markAs(c, b) },
                    )

                    Text(
                        c.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Text(
                c.uploaded,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp)
                    .padding(4.dp)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp)
            ) {
                if (infoModel.source.canPlay) {
                    OutlinedButton(
                        onClick = {
                            genericInfo.chapterOnClick(c, chapters, infoModel, context, context.findActivity(), navController)
                            insertRecent()
                            if (!read.fastAny { it.url == c.url }) markAs(c, true)
                        },
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(horizontal = 4.dp),
                        border = BorderStroke(1.dp, LocalContentColor.current)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.PlayArrow,
                                "Play",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )
                            Text(
                                stringResource(R.string.read),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                if (infoModel.source.canDownload && showDownload()) {
                    OutlinedButton(
                        onClick = {
                            genericInfo.downloadChapter(c, chapters, infoModel, context, context.findActivity(), navController)
                            insertRecent()
                            if (!read.fastAny { it.url == c.url }) markAs(c, true)
                        },
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(horizontal = 4.dp),
                        border = BorderStroke(1.dp, LocalContentColor.current)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.Download,
                                "Download",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )
                            Text(
                                stringResource(R.string.download_chapter),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}