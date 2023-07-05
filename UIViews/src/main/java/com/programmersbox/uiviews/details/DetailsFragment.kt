package com.programmersbox.uiviews.details

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.load.model.GlideUrl
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalHistoryDao
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.animate
import com.programmersbox.uiviews.utils.currentDetailsUrl
import com.programmersbox.uiviews.utils.fadeInAnimation
import com.programmersbox.uiviews.utils.findActivity
import com.programmersbox.uiviews.utils.historySave
import com.programmersbox.uiviews.utils.navigateChromeCustomTabs
import com.programmersbox.uiviews.utils.scaleRotateOffsetReset
import com.programmersbox.uiviews.utils.toComposeColor
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.palette.PalettePlugin
import com.skydoves.landscapist.placeholder.placeholder.PlaceholderPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.material3.contentColorFor as m3ContentColorFor

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
    details: DetailsViewModel = viewModel { DetailsViewModel(createSavedStateHandle(), genericInfo, dao = dao, context = localContext) }
) {
    val navController = LocalNavController.current

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
                        IconButton(
                            onClick = {
                                localContext.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, details.itemModel?.url.orEmpty())
                                    putExtra(Intent.EXTRA_TITLE, details.itemModel?.title.orEmpty())
                                }, localContext.getString(R.string.share_item, details.itemModel?.title.orEmpty())))
                            }
                        ) { Icon(Icons.Default.Share, null) }

                        IconButton(
                            onClick = {
                                details.itemModel?.url?.let {
                                    navController.navigateChromeCustomTabs(it)
                                }
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

        val systemUiController = rememberSystemUiController()
        val statusBar = Color.Transparent
        val statusBarColor = swatchInfo?.rgb?.toComposeColor()?.animate()

        var c by remember { mutableStateOf(statusBar) }
        val ac by animateColorAsState(c, label = "")

        LaunchedEffect(ac) { systemUiController.setStatusBarColor(Color.Transparent, darkIcons = ac.luminance() > 0.5f) }

        SideEffect { currentDetailsUrl = details.itemModel!!.url }

        val lifecycleOwner = LocalLifecycleOwner.current

        // If `lifecycleOwner` changes, dispose and reset the effect
        DisposableEffect(lifecycleOwner, swatchInfo?.rgb) {
            // Create an observer that triggers our remembered callbacks
            // for sending analytics events
            val observer = LifecycleEventObserver { _, event ->
                c = when (event) {
                    Lifecycle.Event.ON_CREATE -> statusBarColor?.value ?: statusBar
                    Lifecycle.Event.ON_START -> statusBarColor?.value ?: statusBar
                    Lifecycle.Event.ON_RESUME -> statusBarColor?.value ?: statusBar
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> statusBar
                    Lifecycle.Event.ON_ANY -> statusBarColor?.value ?: statusBar
                }
            }

            // Add the observer to the lifecycle
            lifecycleOwner.lifecycle.addObserver(observer)

            // When the effect leaves the Composition, remove the observer
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

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
                    onTranslateDescription = details::translateDescription
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
                    onTranslateDescription = details::translateDescription
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
    markAs: (ChapterModel, Boolean) -> Unit
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
                            indication = rememberRipple()
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
    markAs: (ChapterModel, Boolean) -> Unit
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
                indication = rememberRipple(),
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
                        IconButton(
                            onClick = {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, c.url)
                                    putExtra(Intent.EXTRA_TITLE, c.name)
                                }, context.getString(R.string.share_item, c.name)))
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
                                    ?: MaterialTheme.colorScheme.onSurface.copy(alpha = LocalContentAlpha.current)
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

                if (infoModel.source.canDownload) {
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
                                    ?: MaterialTheme.colorScheme.onSurface.copy(alpha = LocalContentAlpha.current)
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

@OptIn(ExperimentalLayoutApi::class)
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
internal fun DetailsHeader(
    model: InfoModel,
    logo: Any?,
    isFavorite: Boolean,
    favoriteClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    possibleDescription: @Composable () -> Unit = {},
) {
    val swatchChange = LocalSwatchChange.current
    val swatchInfo = LocalSwatchInfo.current.colors
    val surface = MaterialTheme.colorScheme.surface
    val imageUrl = remember {
        try {
            GlideUrl(model.imageUrl) { model.extras.map { it.key to it.value.toString() }.toMap() }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            val b = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(b).drawColor(surface.toArgb())
            b
        }
    }

    var imagePopup by remember { mutableStateOf(false) }

    if (imagePopup) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { imagePopup = false },
            title = { Text(model.title, modifier = Modifier.padding(4.dp)) },
            text = {
                GlideImage(
                    imageModel = { imageUrl },
                    imageOptions = ImageOptions(contentScale = ContentScale.Fit),
                    previewPlaceholder = R.drawable.ic_baseline_battery_alert_24,
                    modifier = Modifier
                        .scaleRotateOffsetReset()
                        .defaultMinSize(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                )
            },
            confirmButton = { TextButton(onClick = { imagePopup = false }) { Text(stringResource(R.string.done)) } }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .animateContentSize()
    ) {
        GlideImage(
            imageModel = { imageUrl },
            imageOptions = ImageOptions(contentScale = ContentScale.Crop),
            modifier = Modifier.matchParentSize(),
            previewPlaceholder = R.drawable.ic_baseline_battery_alert_24
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    ColorUtils
                        .setAlphaComponent(
                            ColorUtils.blendARGB(
                                MaterialTheme.colorScheme.surface.toArgb(),
                                swatchInfo?.rgb ?: Color.Transparent.toArgb(),
                                0.25f
                            ),
                            200
                        )
                        .toComposeColor()
                        .animate().value
                )
        )

        Column(
            modifier = Modifier
                .padding(4.dp)
                .animateContentSize()
        ) {
            Row {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(4.dp)
                ) {
                    val latestSwatch by rememberUpdatedState(newValue = swatchInfo)
                    GlideImage(
                        imageModel = { imageUrl },
                        imageOptions = ImageOptions(contentScale = ContentScale.Fit),
                        component = rememberImageComponent {
                            +PalettePlugin { p ->
                                if (latestSwatch == null) {
                                    swatchChange(p.vibrantSwatch?.let { s -> SwatchInfo(s.rgb, s.titleTextColor, s.bodyTextColor) })
                                }
                            }
                            +PlaceholderPlugin.Loading(logo)
                            +PlaceholderPlugin.Failure(logo)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .combinedClickable(
                                onClick = {},
                                onDoubleClick = { imagePopup = true }
                            )
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        model.source.serviceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    var descriptionVisibility by remember { mutableStateOf(false) }

                    Text(
                        model.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple()
                            ) { descriptionVisibility = !descriptionVisibility }
                            .fillMaxWidth(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple()
                            ) { favoriteClick(isFavorite) }
                            .semantics(true) {}
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = swatchInfo?.rgb?.toComposeColor()?.animate()?.value
                                ?: MaterialTheme.colorScheme.onSurface.copy(alpha = LocalContentAlpha.current),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Crossfade(targetState = isFavorite, label = "") { target ->
                            Text(
                                stringResource(if (target) R.string.removeFromFavorites else R.string.addToFavorites),
                                style = MaterialTheme.typography.headlineSmall,
                                fontSize = 20.sp,
                                modifier = Modifier.align(Alignment.CenterVertically),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        stringResource(R.string.chapter_count, model.chapters.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    /*if(model.alternativeNames.isNotEmpty()) {
                        Text(
                            stringResource(R.string.alternateNames, model.alternativeNames.joinToString(", ")),
                            maxLines = if (descriptionVisibility) Int.MAX_VALUE else 2,
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { descriptionVisibility = !descriptionVisibility }
                        )
                    }*/

                    /*
                    var descriptionVisibility by remember { mutableStateOf(false) }
                    Text(
                        model.description,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { descriptionVisibility = !descriptionVisibility },
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 2,
                        style = MaterialTheme.typography.body2,
                    )*/

                }
            }

            FlowRow(
                //mainAxisSpacing = 4.dp,
                //crossAxisSpacing = 2.dp,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                model.genres.forEach {
                    AssistChip(
                        onClick = {},
                        modifier = Modifier.fadeInAnimation(),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = (swatchInfo?.rgb?.toComposeColor() ?: MaterialTheme.colorScheme.onSurface)
                                .animate().value,
                            labelColor = (swatchInfo?.bodyColor?.toComposeColor()?.copy(1f) ?: MaterialTheme.colorScheme.surface)
                                .animate().value
                        ),
                        label = { Text(it) }
                    )
                }
            }
            possibleDescription()
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun PlaceHolderHeader(paddingValues: PaddingValues) {

    val placeholderModifier = Modifier.placeholder(
        true,
        color = m3ContentColorFor(backgroundColor = MaterialTheme.colorScheme.surface)
            .copy(0.1f)
            .compositeOver(MaterialTheme.colorScheme.surface),
        highlight = PlaceholderHighlight.shimmer(MaterialTheme.colorScheme.surface.copy(alpha = .75f))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        Row(modifier = Modifier.padding(4.dp)) {

            Card(
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(4.dp)
            ) {
                Image(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .then(placeholderModifier)
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                )
            }

            Column(
                modifier = Modifier.padding(start = 4.dp)
            ) {

                Row(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .then(placeholderModifier)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) { Text("") }

                Row(
                    modifier = Modifier
                        .then(placeholderModifier)
                        .semantics(true) {}
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                ) {

                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        stringResource(R.string.addToFavorites),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Text(
                    "Otaku".repeat(50),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .then(placeholderModifier),
                    maxLines = 2
                )
            }
        }
    }
}

data class SwatchInfo(val rgb: Int?, val titleColor: Int?, val bodyColor: Int?)

internal data class SwatchInfoColors(val colors: SwatchInfo? = null)

internal val LocalSwatchInfo = compositionLocalOf { SwatchInfoColors() }
internal val LocalSwatchChange = staticCompositionLocalOf<(SwatchInfo?) -> Unit> { {} }