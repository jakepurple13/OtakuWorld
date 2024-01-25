package com.programmersbox.uiviews.details

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.ripple
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalHistoryDao
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.animate
import com.programmersbox.uiviews.utils.components.OtakuScaffold
import com.programmersbox.uiviews.utils.findActivity
import com.programmersbox.uiviews.utils.historySave
import com.programmersbox.uiviews.utils.launchCatching
import com.programmersbox.uiviews.utils.toComposeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun DetailsScreen(
    logo: NotificationLogo,
    windowSize: WindowSizeClass,
    localContext: Context = LocalContext.current,
    dao: ItemDao = LocalItemDao.current,
    genericInfo: GenericInfo = LocalGenericInfo.current,
    details: DetailsViewModel = viewModel { DetailsViewModel(createSavedStateHandle(), genericInfo, dao = dao, context = localContext) },
) {
    val uriHandler = LocalUriHandler.current
    val showDownload by LocalSettingsHandling.current.showDownload.flow.collectAsStateWithLifecycle(initialValue = true)

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

        val shareChapter by handling.shareChapter.collectAsState(initial = true)
        var swatchInfo by remember { mutableStateOf<SwatchInfo?>(null) }

        CompositionLocalProvider(
            LocalSwatchInfo provides remember(swatchInfo) { SwatchInfoColors(swatchInfo) },
            LocalSwatchChange provides rememberUpdatedState(newValue = { it: SwatchInfo? -> swatchInfo = it }).value
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
                    showDownloadButton = showDownload
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
                    showDownloadButton = showDownload
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAsScreen(
    topBarColor: Color,
    drawerState: DrawerState,
    info: InfoModel,
    chapters: List<ChapterWatched>,
    markAs: (ChapterModel, Boolean) -> Unit,
) {
    val swatchInfo = LocalSwatchInfo.current.colors
    val scrollBehaviorMarkAs = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()

    OtakuScaffold(
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(stringResource(id = R.string.markAs), color = topBarColor) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = swatchInfo?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = swatchInfo?.rgb?.toComposeColor()?.animate()?.value?.let {
                        MaterialTheme.colorScheme.surface.surfaceColorAtElevation(1.dp, it)
                    } ?: MaterialTheme.colorScheme.applyTonalElevation(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        elevation = 1.dp
                    )
                ),
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Close, null, tint = topBarColor)
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
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) { markAs(c, !chapters.fastAny { it.url == c.url }) },
                    color = swatchInfo?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colorScheme.surface
                ) {
                    ListItem(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = ListItemDefaults.colors(
                            headlineColor = swatchInfo?.bodyColor
                                ?.toComposeColor()
                                ?.animate()?.value ?: MaterialTheme.colorScheme.onSurface,
                            containerColor = swatchInfo?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colorScheme.surface
                        ),
                        headlineContent = { Text(c.name) },
                        leadingContent = {
                            Checkbox(
                                checked = chapters.fastAny { it.url == c.url },
                                onCheckedChange = { b -> markAs(c, b) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value
                                        ?: MaterialTheme.colorScheme.secondary,
                                    uncheckedColor = swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value
                                        ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    checkmarkColor = swatchInfo?.rgb?.toComposeColor()?.animate()?.value
                                        ?: MaterialTheme.colorScheme.surface
                                )
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
    showDownload: Boolean,
    markAs: (ChapterModel, Boolean) -> Unit,
) {
    val historyDao = LocalHistoryDao.current
    val swatchInfo = LocalSwatchInfo.current.colors
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

    val interactionSource = remember { MutableInteractionSource() }

    ElevatedCard(
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = ripple(),
                interactionSource = interactionSource,
            ) { markAs(c, !read.fastAny { it.url == c.url }) },
        colors = CardDefaults.elevatedCardColors(
            containerColor = animateColorAsState(swatchInfo?.rgb?.toComposeColor() ?: MaterialTheme.colorScheme.surface, label = "").value,
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            if (shareChapter) {
                ListItem(
                    leadingContent = {
                        Checkbox(
                            checked = read.fastAny { it.url == c.url },
                            onCheckedChange = { b -> markAs(c, b) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value
                                    ?: MaterialTheme.colorScheme.secondary,
                                uncheckedColor = swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value
                                    ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                checkmarkColor = swatchInfo?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    headlineContent = {
                        Text(
                            c.name,
                            style = MaterialTheme.typography.bodyLarge
                                .let { b -> swatchInfo?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
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
                                tint = swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current
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
                        colors = CheckboxDefaults.colors(
                            checkedColor = swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value
                                ?: MaterialTheme.colorScheme.secondary,
                            uncheckedColor = swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value
                                ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            checkmarkColor = swatchInfo?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colorScheme.surface
                        )
                    )

                    Text(
                        c.name,
                        style = MaterialTheme.typography.bodyLarge
                            .let { b -> swatchInfo?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Text(
                c.uploaded,
                style = MaterialTheme.typography.titleSmall
                    .let { b -> swatchInfo?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
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
                        //colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(1.dp, swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.PlayArrow,
                                "Play",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                tint = swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value
                                    ?: MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.read),
                                style = MaterialTheme.typography.labelLarge
                                    .let { b -> swatchInfo?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                if (infoModel.source.canDownload && showDownload) {
                    OutlinedButton(
                        onClick = {
                            genericInfo.downloadChapter(c, chapters, infoModel, context, context.findActivity(), navController)
                            insertRecent()
                            if (!read.fastAny { it.url == c.url }) markAs(c, true)
                        },
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(horizontal = 4.dp),
                        //colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(1.dp, swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.Download,
                                "Download",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                tint = swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value
                                    ?: MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.download_chapter),
                                style = MaterialTheme.typography.labelLarge
                                    .let { b -> swatchInfo?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}