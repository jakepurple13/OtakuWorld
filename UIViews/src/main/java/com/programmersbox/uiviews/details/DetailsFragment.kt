package com.programmersbox.uiviews.details

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmpalette.color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.rememberDynamicMaterialThemeState
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.SystemThemeMode
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalHistoryDao
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.components.OtakuScaffold
import com.programmersbox.uiviews.utils.findActivity
import com.programmersbox.uiviews.utils.historySave
import com.programmersbox.uiviews.utils.launchCatching
import com.programmersbox.uiviews.utils.rememberSwatchStyle
import com.programmersbox.uiviews.utils.rememberSwatchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
fun DetailsScreen(
    detailInfo: Screen.DetailsScreen.Details,
    logo: NotificationLogo,
    windowSize: WindowSizeClass,
    localContext: Context = LocalContext.current,
    dao: ItemDao = LocalItemDao.current,
    genericInfo: GenericInfo = LocalGenericInfo.current,
    details: DetailsViewModel = viewModel {
        DetailsViewModel(
            details = detailInfo,
            handle = createSavedStateHandle(),
            genericInfo = genericInfo,
            dao = dao,
            context = localContext
        )
    },
) {
    DetailsScreen(
        logo = logo,
        windowSize = windowSize,
        localContext = localContext,
        dao = dao,
        genericInfo = genericInfo,
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
fun DetailsScreen(
    //detailInfo: Screen.DetailsScreen.Details,
    logo: NotificationLogo,
    windowSize: WindowSizeClass,
    localContext: Context = LocalContext.current,
    dao: ItemDao = LocalItemDao.current,
    genericInfo: GenericInfo = LocalGenericInfo.current,
    details: DetailsViewModel = viewModel {
        DetailsViewModel(
            //details = detailInfo,
            handle = createSavedStateHandle(),
            genericInfo = genericInfo,
            dao = dao,
            context = localContext
        )
    },
) {
    val uriHandler = LocalUriHandler.current
    val showDownload by LocalSettingsHandling.current.rememberShowDownload()
    val scope = rememberCoroutineScope()

    if (details.info == null) {
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
        ) { PlaceHolderHeader(it) }
    } else if (details.info != null) {

        val handling = LocalSettingsHandling.current

        val isSaved by dao.doesNotificationExistFlow(details.itemModel!!.url).collectAsStateWithLifecycle(false)

        val usePalette by handling.rememberUsePalette()

        val isAmoledMode by handling.rememberIsAmoledMode()
        val themeSetting by handling.systemThemeMode.collectAsStateWithLifecycle(SystemThemeMode.FollowSystem)
        val paletteSwatchType by rememberSwatchType()
        val paletteStyle by rememberSwatchStyle()

        val dynamicColor = rememberDynamicMaterialThemeState(
            seedColor = details.palette?.let(paletteSwatchType.swatch)?.color?.takeIf { usePalette } ?: MaterialTheme.colorScheme.primary,
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
            animate = true
        ) {
            if (windowSize.widthSizeClass == WindowWidthSizeClass.Expanded) {
                DetailsViewLandscape(
                    info = details.info!!,
                    isSaved = isSaved,
                    shareChapter = shareChapter,
                    logo = logo,
                    isFavorite = details.favoriteListener,
                    onFavoriteClick = { b -> if (b) details.removeItem() else details.addItem() },
                    chapters = details.chapters,
                    markAs = details::markAs,
                    description = details.description,
                    onTranslateDescription = details::translateDescription,
                    showDownloadButton = { showDownload },
                    canNotify = details.dbModel?.shouldCheckForUpdate ?: false,
                    notifyAction = { scope.launch { details.toggleNotify() } },
                    onPaletteSet = { details.palette = it }
                )
            } else {
                DetailsView(
                    info = details.info!!,
                    isSaved = isSaved,
                    shareChapter = shareChapter,
                    logo = logo,
                    isFavorite = details.favoriteListener,
                    onFavoriteClick = { b -> if (b) details.removeItem() else details.addItem() },
                    chapters = details.chapters,
                    markAs = details::markAs,
                    description = details.description,
                    onTranslateDescription = details::translateDescription,
                    showDownloadButton = { showDownload },
                    canNotify = details.dbModel?.shouldCheckForUpdate ?: false,
                    notifyAction = { scope.launch { details.toggleNotify() } },
                    onPaletteSet = { details.palette = it }
                )
            }
        }
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
    val navController = LocalNavController.current
    val genericInfo = LocalGenericInfo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun insertRecent() {
        scope.launch(Dispatchers.IO) {
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
            val save = runBlocking { context.historySave.first() }
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
                                tint = MaterialTheme.colorScheme.onSurface
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