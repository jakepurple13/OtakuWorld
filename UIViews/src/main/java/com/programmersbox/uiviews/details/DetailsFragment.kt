package com.programmersbox.uiviews.details

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bumptech.glide.load.model.GlideUrl
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.programmersbox.favoritesdatabase.*
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.palette.PalettePlugin
import com.skydoves.landscapist.placeholder.placeholder.PlaceholderPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import my.nanihadesuka.compose.LazyColumnScrollbar
import kotlin.math.ln
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.contentColorFor as m3ContentColorFor

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun DetailsScreen(
    navController: NavController,
    genericInfo: GenericInfo,
    logo: NotificationLogo,
    dao: ItemDao,
    historyDao: HistoryDao,
    windowSize: WindowSize
) {
    val localContext = LocalContext.current
    val details: DetailsViewModel = viewModel { DetailsViewModel(createSavedStateHandle(), genericInfo, dao = dao, context = localContext) }

    if (details.info == null) {
        Scaffold(
            topBar = {
                InsetSmallTopAppBar(
                    modifier = Modifier.zIndex(2f),
                    title = { Text(details.itemModel?.title.orEmpty()) },
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

        val isSaved by dao.doesNotificationExistFlow(details.itemModel!!.url).collectAsState(initial = false)

        val shareChapter by localContext.shareChapter.collectAsState(initial = true)
        val swatchInfo = remember { mutableStateOf<SwatchInfo?>(null) }

        val systemUiController = rememberSystemUiController()
        val statusBar = Color.Transparent
        val statusBarColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()

        var c by remember { mutableStateOf(statusBar) }
        val ac by animateColorAsState(c)

        LaunchedEffect(ac) { systemUiController.setStatusBarColor(Color.Transparent, darkIcons = ac.luminance() > 0.5f) }

        SideEffect { currentDetailsUrl = details.itemModel!!.url }

        val lifecycleOwner = LocalLifecycleOwner.current

        // If `lifecycleOwner` changes, dispose and reset the effect
        DisposableEffect(lifecycleOwner, swatchInfo.value?.rgb) {
            // Create an observer that triggers our remembered callbacks
            // for sending analytics events
            val observer = LifecycleEventObserver { _, event ->
                c = when (event) {
                    Lifecycle.Event.ON_CREATE -> statusBarColor?.value ?: statusBar
                    Lifecycle.Event.ON_START -> statusBarColor?.value ?: statusBar
                    Lifecycle.Event.ON_RESUME -> statusBarColor?.value ?: statusBar
                    Lifecycle.Event.ON_PAUSE -> statusBarColor?.value ?: statusBar
                    Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> statusBar
                    Lifecycle.Event.ON_ANY -> statusBarColor?.value ?: statusBar
                }
            }

            // Add the observer to the lifecycle
            lifecycleOwner.lifecycle.addObserver(observer)

            // When the effect leaves the Composition, remove the observer
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val orientation = LocalConfiguration.current.orientation

        if (
            windowSize == WindowSize.Medium ||
            windowSize == WindowSize.Expanded ||
            orientation == Configuration.ORIENTATION_LANDSCAPE
        ) {
            DetailsViewLandscape(
                details.info!!,
                isSaved,
                shareChapter,
                swatchInfo,
                navController,
                dao,
                historyDao,
                details,
                genericInfo,
                logo
            )
        } else {
            DetailsView(
                details.info!!,
                isSaved,
                shareChapter,
                swatchInfo,
                navController,
                dao,
                historyDao,
                details,
                genericInfo,
                logo
            )
        }
    }
}

@Composable
private fun Color.animate() = animateColorAsState(this)

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun DetailsViewLandscape(
    info: InfoModel,
    isSaved: Boolean,
    shareChapter: Boolean,
    swatchInfo: MutableState<SwatchInfo?>,
    navController: NavController,
    dao: ItemDao,
    historyDao: HistoryDao,
    vm: DetailsViewModel,
    genericInfo: GenericInfo,
    logo: NotificationLogo
) {
    val context = LocalContext.current

    var reverseChapters by remember { mutableStateOf(false) }

    val hostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberDrawerState(DrawerValue.Closed)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    BackHandler(scaffoldState.isOpen) {
        scope.launch {
            try {
                scaffoldState.close()
            } catch (e: Exception) {
                navController.popBackStack()
            }
        }
    }

    val topBarColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
        ?: M3MaterialTheme.colorScheme.onSurface

    ModalNavigationDrawer(
        drawerState = scaffoldState,
        drawerContent = {
            ModalDrawerSheet {
                MarkAsScreen(
                    topBarColor = topBarColor,
                    swatchInfo = swatchInfo,
                    drawerState = scaffoldState,
                    info = info,
                    vm = vm
                )
            }
        }
    ) {
        OtakuScaffold(
            containerColor = Color.Transparent,
            topBar = {
                InsetSmallTopAppBar(
                    modifier = Modifier.zIndex(2f),
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        titleContentColor = topBarColor,
                        containerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value?.let {
                            M3MaterialTheme.colorScheme.surface.surfaceColorAtElevation(1.dp, it)
                        } ?: M3MaterialTheme.colorScheme.applyTonalElevation(
                            backgroundColor = M3MaterialTheme.colorScheme.surface,
                            elevation = 1.dp
                        )
                    ),
                    scrollBehavior = scrollBehavior,
                    title = { Text(info.title) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = topBarColor)
                        }
                    },
                    actions = {
                        var showDropDown by remember { mutableStateOf(false) }

                        val dropDownDismiss = { showDropDown = false }

                        DropdownMenu(
                            expanded = showDropDown,
                            onDismissRequest = dropDownDismiss,
                        ) {

                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    scope.launch { scaffoldState.open() }
                                },
                                text = { Text(stringResource(id = R.string.markAs)) },
                                leadingIcon = { Icon(Icons.Default.Check, null) }
                            )

                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    navController.navigateChromeCustomTabs(info.url)
                                },
                                text = { Text(stringResource(id = R.string.fallback_menu_item_open_in_browser)) },
                                leadingIcon = { Icon(Icons.Default.OpenInBrowser, null) }
                            )

                            if (!isSaved) {
                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        scope.launch(Dispatchers.IO) {
                                            dao.insertNotification(
                                                NotificationItem(
                                                    id = info.hashCode(),
                                                    url = info.url,
                                                    summaryText = context
                                                        .getString(
                                                            R.string.hadAnUpdate,
                                                            info.title,
                                                            info.chapters.firstOrNull()?.name.orEmpty()
                                                        ),
                                                    notiTitle = info.title,
                                                    imageUrl = info.imageUrl,
                                                    source = info.source.serviceName,
                                                    contentTitle = info.title
                                                )
                                            )
                                        }
                                    },
                                    text = { Text(stringResource(id = R.string.save_for_later)) },
                                    leadingIcon = { Icon(Icons.Default.Save, null) }
                                )
                            } else {
                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        scope.launch(Dispatchers.IO) {
                                            dao.getNotificationItemFlow(info.url)
                                                .firstOrNull()
                                                ?.let { dao.deleteNotification(it) }
                                        }
                                    },
                                    text = { Text(stringResource(R.string.removeNotification)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                                )
                            }

                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    Screen.GlobalSearchScreen.navigate(navController, info.title)
                                },
                                text = { Text(stringResource(id = R.string.global_search_by_name)) },
                                leadingIcon = { Icon(Icons.Default.Search, null) }
                            )

                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    reverseChapters = !reverseChapters
                                },
                                text = { Text(stringResource(id = R.string.reverseOrder)) },
                                leadingIcon = { Icon(Icons.Default.Sort, null) }
                            )
                        }

                        IconButton(
                            onClick = {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, info.url)
                                    putExtra(Intent.EXTRA_TITLE, info.title)
                                }, context.getString(R.string.share_item, info.title)))
                            }
                        ) { Icon(Icons.Default.Share, null, tint = topBarColor) }

                        genericInfo.DetailActions(infoModel = info, tint = topBarColor)

                        IconButton(onClick = { showDropDown = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = topBarColor)
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState) { data ->
                    val background = swatchInfo.value?.rgb?.toComposeColor() ?: SnackbarDefaults.color
                    val font = swatchInfo.value?.titleColor?.toComposeColor() ?: M3MaterialTheme.colorScheme.surface
                    Snackbar(
                        containerColor = Color(ColorUtils.blendARGB(background.toArgb(), M3MaterialTheme.colorScheme.onSurface.toArgb(), .25f)),
                        contentColor = font,
                        snackbarData = data
                    )
                }
            },
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            swatchInfo.value?.rgb
                                ?.toComposeColor()
                                ?.animate()?.value ?: M3MaterialTheme.colorScheme.background,
                            M3MaterialTheme.colorScheme.background
                        )
                    )
                )
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { p ->
            DetailsLandscapeContent(
                p = p,
                info = info,
                shareChapter = shareChapter,
                swatchInfo = swatchInfo,
                navController = navController,
                historyDao = historyDao,
                vm = vm,
                genericInfo = genericInfo,
                logo = logo,
                reverseChapters = reverseChapters
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DetailsLandscapeContent(
    p: PaddingValues,
    info: InfoModel,
    shareChapter: Boolean,
    swatchInfo: MutableState<SwatchInfo?>,
    navController: NavController,
    historyDao: HistoryDao,
    vm: DetailsViewModel,
    genericInfo: GenericInfo,
    logo: NotificationLogo,
    reverseChapters: Boolean
) {
    Row(
        modifier = Modifier.padding(p)
    ) {

        DetailsHeader(
            modifier = Modifier.weight(1f),
            model = info,
            logo = painterResource(id = logo.notificationId),
            isFavorite = vm.favoriteListener,
            swatchInfo = swatchInfo
        ) { b -> if (b) vm.removeItem() else vm.addItem() }

        val listState = rememberLazyListState()

        var descriptionVisibility by remember { mutableStateOf(false) }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(vertical = 5.dp),
            state = listState
        ) {

            if (info.description.isNotEmpty()) {
                item {
                    Text(
                        info.description,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple()
                            ) { descriptionVisibility = !descriptionVisibility }
                            .padding(horizontal = 5.dp)
                            //.fillMaxWidth()
                            .animateContentSize(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                        style = M3MaterialTheme.typography.bodyMedium,
                        color = M3MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(info.chapters.let { if (reverseChapters) it.reversed() else it }) { c ->
                ChapterItem(
                    infoModel = info,
                    c = c,
                    read = vm.chapters,
                    chapters = info.chapters,
                    swatchInfo = swatchInfo,
                    shareChapter = shareChapter,
                    historyDao = historyDao,
                    vm = vm,
                    genericInfo = genericInfo,
                    navController = navController,
                )
            }
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun DetailsView(
    info: InfoModel,
    isSaved: Boolean,
    shareChapter: Boolean,
    swatchInfo: MutableState<SwatchInfo?>,
    navController: NavController,
    dao: ItemDao,
    historyDao: HistoryDao,
    vm: DetailsViewModel,
    genericInfo: GenericInfo,
    logo: NotificationLogo
) {

    var reverseChapters by remember { mutableStateOf(false) }

    val hostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberDrawerState(DrawerValue.Closed)

    val context = LocalContext.current

    BackHandler(scaffoldState.isOpen) {
        scope.launch {
            try {
                scaffoldState.close()
            } catch (e: Exception) {
                navController.popBackStack()
            }
        }
    }

    val topBarColor by animateColorAsState(swatchInfo.value?.bodyColor?.toComposeColor() ?: M3MaterialTheme.colorScheme.onSurface)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    ModalNavigationDrawer(
        drawerState = scaffoldState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(
                    topStart = 0.0.dp,
                    topEnd = 8.0.dp,
                    bottomEnd = 8.0.dp,
                    bottomStart = 0.0.dp
                ),
                windowInsets = WindowInsets(0.dp)
            ) {
                MarkAsScreen(
                    topBarColor = topBarColor,
                    swatchInfo = swatchInfo,
                    drawerState = scaffoldState,
                    info = info,
                    vm = vm
                )
            }
        }
    ) {
        val b = M3MaterialTheme.colorScheme.background
        val c by animateColorAsState(swatchInfo.value?.rgb?.toComposeColor() ?: b)

        OtakuScaffold(
            containerColor = Color.Transparent,
            topBar = {
                InsetSmallTopAppBar(
                    modifier = Modifier.zIndex(2f),
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        titleContentColor = topBarColor,
                        containerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value?.let {
                            M3MaterialTheme.colorScheme.surface.surfaceColorAtElevation(1.dp, it)
                        } ?: M3MaterialTheme.colorScheme.applyTonalElevation(
                            backgroundColor = M3MaterialTheme.colorScheme.surface,
                            elevation = 1.dp
                        )
                    ),
                    scrollBehavior = scrollBehavior,
                    title = { Text(info.title) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = topBarColor)
                        }
                    },
                    actions = {
                        var showDropDown by remember { mutableStateOf(false) }

                        val dropDownDismiss = { showDropDown = false }

                        DropdownMenu(
                            expanded = showDropDown,
                            onDismissRequest = dropDownDismiss,
                        ) {

                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    scope.launch { scaffoldState.open() }
                                },
                                text = { Text(stringResource(id = R.string.markAs)) },
                                leadingIcon = { Icon(Icons.Default.Check, null) }
                            )

                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    navController.navigateChromeCustomTabs(info.url)
                                },
                                text = { Text(stringResource(id = R.string.fallback_menu_item_open_in_browser)) },
                                leadingIcon = { Icon(Icons.Default.OpenInBrowser, null) }
                            )

                            if (!isSaved) {
                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        scope.launch(Dispatchers.IO) {
                                            dao.insertNotification(
                                                NotificationItem(
                                                    id = info.hashCode(),
                                                    url = info.url,
                                                    summaryText = context
                                                        .getString(
                                                            R.string.hadAnUpdate,
                                                            info.title,
                                                            info.chapters.firstOrNull()?.name.orEmpty()
                                                        ),
                                                    notiTitle = info.title,
                                                    imageUrl = info.imageUrl,
                                                    source = info.source.serviceName,
                                                    contentTitle = info.title
                                                )
                                            )
                                        }
                                    },
                                    text = { Text(stringResource(id = R.string.save_for_later)) },
                                    leadingIcon = { Icon(Icons.Default.Save, null) }
                                )
                            } else {
                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        scope.launch(Dispatchers.IO) {
                                            dao.getNotificationItemFlow(info.url)
                                                .firstOrNull()
                                                ?.let { dao.deleteNotification(it) }
                                        }
                                    },
                                    text = { Text(stringResource(R.string.removeNotification)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                                )
                            }

                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    Screen.GlobalSearchScreen.navigate(navController, info.title)
                                },
                                text = { Text(stringResource(id = R.string.global_search_by_name)) },
                                leadingIcon = { Icon(Icons.Default.Search, null) }
                            )

                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    reverseChapters = !reverseChapters
                                },
                                text = { Text(stringResource(id = R.string.reverseOrder)) },
                                leadingIcon = { Icon(Icons.Default.Sort, null) }
                            )
                        }

                        IconButton(
                            onClick = {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, info.url)
                                    putExtra(Intent.EXTRA_TITLE, info.title)
                                }, context.getString(R.string.share_item, info.title)))
                            }
                        ) { Icon(Icons.Default.Share, null, tint = topBarColor) }

                        genericInfo.DetailActions(infoModel = info, tint = topBarColor)

                        IconButton(onClick = { showDropDown = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = topBarColor)
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState) { data ->
                    val background = swatchInfo.value?.rgb?.toComposeColor() ?: SnackbarDefaults.color
                    val font = swatchInfo.value?.titleColor?.toComposeColor() ?: M3MaterialTheme.colorScheme.surface
                    Snackbar(
                        containerColor = Color(ColorUtils.blendARGB(background.toArgb(), M3MaterialTheme.colorScheme.onSurface.toArgb(), .25f)),
                        contentColor = font,
                        snackbarData = data
                    )
                }
            },
            modifier = Modifier
                .drawBehind { drawRect(Brush.verticalGradient(listOf(c, b))) }
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { p ->

            val header: @Composable () -> Unit = {
                DetailsHeader(
                    model = info,
                    logo = painterResource(id = logo.notificationId),
                    isFavorite = vm.favoriteListener,
                    swatchInfo = swatchInfo
                ) { b -> if (b) vm.removeItem() else vm.addItem() }
            }

            val state = rememberCollapsingToolbarScaffoldState()

            CollapsingToolbarScaffold(
                modifier = Modifier.padding(p),
                state = state,
                scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
                toolbar = { header() }
            ) {
                val listState = rememberLazyListState()

                LazyColumnScrollbar(
                    enabled = true,
                    thickness = 8.dp,
                    padding = 2.dp,
                    listState = listState,
                    thumbColor = swatchInfo.value?.bodyColor?.toComposeColor() ?: M3MaterialTheme.colorScheme.primary,
                    thumbSelectedColor = (swatchInfo.value?.bodyColor?.toComposeColor() ?: M3MaterialTheme.colorScheme.primary).copy(alpha = .6f),
                ) {
                    var descriptionVisibility by remember { mutableStateOf(false) }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 5.dp),
                        state = listState
                    ) {

                        if (info.description.isNotEmpty()) {
                            item {
                                Box {
                                    val progress = remember { mutableStateOf(false) }

                                    Text(
                                        vm.description,
                                        modifier = Modifier
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = rememberRipple(),
                                                onClick = { descriptionVisibility = !descriptionVisibility },
                                                onLongClick = { vm.translateDescription(progress) }
                                            )
                                            .padding(horizontal = 5.dp)
                                            .fillMaxWidth()
                                            .animateContentSize(),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                                        style = M3MaterialTheme.typography.bodyMedium,
                                        color = M3MaterialTheme.colorScheme.onSurface
                                    )

                                    if (progress.value) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }

                        items(info.chapters.let { if (reverseChapters) it.reversed() else it }) { c ->
                            ChapterItem(
                                infoModel = info,
                                c = c,
                                read = vm.chapters,
                                chapters = info.chapters,
                                swatchInfo = swatchInfo,
                                shareChapter = shareChapter,
                                historyDao = historyDao,
                                vm = vm,
                                genericInfo = genericInfo,
                                navController = navController,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAsScreen(
    topBarColor: Color,
    swatchInfo: MutableState<SwatchInfo?>,
    drawerState: DrawerState,
    info: InfoModel,
    vm: DetailsViewModel
) {
    val scrollBehaviorMarkAs = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()

    OtakuScaffold(
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(stringResource(id = R.string.markAs), color = topBarColor) },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value?.let {
                        M3MaterialTheme.colorScheme.surface.surfaceColorAtElevation(1.dp, it)
                    } ?: M3MaterialTheme.colorScheme.applyTonalElevation(
                        backgroundColor = M3MaterialTheme.colorScheme.surface,
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
                    tonalElevation = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple()
                        ) { vm.markAs(c, !vm.chapters.fastAny { it.url == c.url }) },
                    color = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                ) {
                    ListItem(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = ListItemDefaults.colors(
                            headlineColor = swatchInfo.value
                                ?.bodyColor
                                ?.toComposeColor()
                                ?.animate()?.value ?: M3MaterialTheme.colorScheme.onSurface,
                            containerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                        ),
                        headlineText = { Text(c.name) },
                        leadingContent = {
                            Checkbox(
                                checked = vm.chapters.fastAny { it.url == c.url },
                                onCheckedChange = { b -> vm.markAs(c, b) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                        ?: M3MaterialTheme.colorScheme.secondary,
                                    uncheckedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                        ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    checkmarkColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value
                                        ?: M3MaterialTheme.colorScheme.surface
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
private fun ChapterItem(
    infoModel: InfoModel,
    c: ChapterModel,
    read: List<ChapterWatched>,
    chapters: List<ChapterModel>,
    swatchInfo: MutableState<SwatchInfo?>,
    shareChapter: Boolean,
    historyDao: HistoryDao,
    vm: DetailsViewModel,
    genericInfo: GenericInfo,
    navController: NavController,
) {
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
            ) { vm.markAs(c, !read.fastAny { it.url == c.url }) },
        colors = CardDefaults.elevatedCardColors(
            containerColor = animateColorAsState(swatchInfo.value?.rgb?.toComposeColor() ?: M3MaterialTheme.colorScheme.surface).value,
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            if (shareChapter) {
                ConstraintLayout(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                ) {
                    val (checkbox, text, share) = createRefs()

                    Checkbox(
                        checked = read.fastAny { it.url == c.url },
                        onCheckedChange = { b -> vm.markAs(c, b) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                ?: M3MaterialTheme.colorScheme.secondary,
                            uncheckedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            checkmarkColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.constrainAs(checkbox) {
                            start.linkTo(parent.start)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        }
                    )

                    Text(
                        c.name,
                        style = M3MaterialTheme.typography.bodyLarge
                            .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .constrainAs(text) {
                                start.linkTo(checkbox.end)
                                end.linkTo(share.start)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                width = Dimension.fillToConstraints
                            }
                    )

                    IconButton(
                        modifier = Modifier
                            .padding(5.dp)
                            .constrainAs(share) {
                                end.linkTo(parent.end)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                            },
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
                            tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = read.fastAny { it.url == c.url },
                        onCheckedChange = { b -> vm.markAs(c, b) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                ?: M3MaterialTheme.colorScheme.secondary,
                            uncheckedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            checkmarkColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                        )
                    )

                    Text(
                        c.name,
                        style = M3MaterialTheme.typography.bodyLarge
                            .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                        modifier = Modifier.padding(start = 5.dp)
                    )
                }
            }

            Text(
                c.uploaded,
                style = M3MaterialTheme.typography.titleSmall
                    .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(5.dp)
            )

            val activity = LocalActivity.current

            Row {
                if (infoModel.source.canPlay) {
                    OutlinedButton(
                        onClick = {
                            genericInfo.chapterOnClick(c, chapters, infoModel, context, activity, navController)
                            insertRecent()
                            if (!read.fastAny { it.url == c.url }) vm.markAs(c, true)
                        },
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(horizontal = 5.dp),
                        //colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(1.dp, swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.PlayArrow,
                                "Play",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                    ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = LocalContentAlpha.current)
                            )
                            Text(
                                stringResource(R.string.read),
                                style = M3MaterialTheme.typography.labelLarge
                                    .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                if (infoModel.source.canDownload) {
                    OutlinedButton(
                        onClick = {
                            genericInfo.downloadChapter(c, chapters, infoModel, context, activity, navController)
                            insertRecent()
                            if (!read.fastAny { it.url == c.url }) vm.markAs(c, true)
                        },
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(horizontal = 5.dp),
                        //colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(1.dp, swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.Download,
                                "Download",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                    ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = LocalContentAlpha.current)
                            )
                            Text(
                                stringResource(R.string.download_chapter),
                                style = M3MaterialTheme.typography.labelLarge
                                    .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
private fun DetailsHeader(
    modifier: Modifier = Modifier,
    model: InfoModel,
    logo: Any?,
    isFavorite: Boolean,
    swatchInfo: MutableState<SwatchInfo?>,
    favoriteClick: (Boolean) -> Unit
) {

    val imageUrl = remember { GlideUrl(model.imageUrl) { model.extras.map { it.key to it.value.toString() }.toMap() } }

    var imagePopup by remember { mutableStateOf(false) }

    if (imagePopup) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { imagePopup = false },
            title = { Text(model.title, modifier = Modifier.padding(5.dp)) },
            text = {
                GlideImage(
                    imageModel = { imageUrl },
                    imageOptions = ImageOptions(contentScale = ContentScale.Fit),
                    modifier = Modifier
                        .scaleRotateOffsetReset()
                        .defaultMinSize(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                )
            },
            confirmButton = { TextButton(onClick = { imagePopup = false }) { Text(stringResource(R.string.done)) } }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
            .then(modifier)
    ) {
        GlideImage(
            imageModel = { imageUrl },
            imageOptions = ImageOptions(contentScale = ContentScale.Crop),
            modifier = Modifier.matchParentSize(),
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    ColorUtils
                        .setAlphaComponent(
                            ColorUtils.blendARGB(
                                M3MaterialTheme.colorScheme.surface.toArgb(),
                                swatchInfo.value?.rgb ?: Color.Transparent.toArgb(),
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
                .padding(5.dp)
                .animateContentSize()
        ) {
            Row {
                Surface(
                    shape = M3MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(5.dp)
                ) {
                    GlideImage(
                        imageModel = { imageUrl },
                        imageOptions = ImageOptions(contentScale = ContentScale.Fit),
                        component = rememberImageComponent {
                            +PalettePlugin { p ->
                                swatchInfo.value = p.vibrantSwatch?.let { s -> SwatchInfo(s.rgb, s.titleTextColor, s.bodyTextColor) }
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
                        style = M3MaterialTheme.typography.labelSmall,
                        color = M3MaterialTheme.colorScheme.onSurface
                    )

                    var descriptionVisibility by remember { mutableStateOf(false) }

                    Text(
                        model.title,
                        style = M3MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple()
                            ) { descriptionVisibility = !descriptionVisibility }
                            .fillMaxWidth(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                        color = M3MaterialTheme.colorScheme.onSurface
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
                            tint = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value
                                ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = LocalContentAlpha.current),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Crossfade(targetState = isFavorite) { target ->
                            Text(
                                stringResource(if (target) R.string.removeFromFavorites else R.string.addToFavorites),
                                style = M3MaterialTheme.typography.headlineSmall,
                                fontSize = 20.sp,
                                modifier = Modifier.align(Alignment.CenterVertically),
                                color = M3MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        stringResource(R.string.chapter_count, model.chapters.size),
                        style = M3MaterialTheme.typography.bodyMedium,
                        color = M3MaterialTheme.colorScheme.onSurface
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
                mainAxisSpacing = 4.dp,
                crossAxisSpacing = 2.dp,
            ) {
                model.genres.forEach {
                    AssistChip(
                        onClick = {},
                        modifier = Modifier.fadeInAnimation(),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = (swatchInfo.value?.rgb?.toComposeColor() ?: M3MaterialTheme.colorScheme.onSurface)
                                .animate().value,
                            labelColor = (swatchInfo.value?.bodyColor?.toComposeColor()?.copy(1f) ?: M3MaterialTheme.colorScheme.surface)
                                .animate().value
                        ),
                        label = { Text(it) }
                    )
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun PlaceHolderHeader(paddingValues: PaddingValues) {

    val placeholderColor = m3ContentColorFor(backgroundColor = M3MaterialTheme.colorScheme.surface)
        .copy(0.1f)
        .compositeOver(M3MaterialTheme.colorScheme.surface)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        Row(modifier = Modifier.padding(5.dp)) {

            Card(
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.padding(5.dp)
            ) {
                Image(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .placeholder(true, color = placeholderColor)
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                )
            }

            Column(
                modifier = Modifier.padding(start = 5.dp)
            ) {

                Row(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .placeholder(true, color = placeholderColor)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) { Text("") }

                Row(
                    modifier = Modifier
                        .placeholder(true, color = placeholderColor)
                        .semantics(true) {}
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                ) {

                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        stringResource(R.string.addToFavorites),
                        style = M3MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Text(
                    "Otaku".repeat(50),
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                        .placeholder(true, color = placeholderColor),
                    maxLines = 2
                )

            }

        }
    }
}

/**
 * Returns the new background [Color] to use, representing the original background [color] with an
 * overlay corresponding to [elevation] applied. The overlay will only be applied to
 * [ColorScheme.surface].
 */
private fun ColorScheme.applyTonalElevation(backgroundColor: Color, elevation: Dp): Color {
    return if (backgroundColor == surface) {
        surfaceColorAtElevation(elevation)
    } else {
        backgroundColor
    }
}

/**
 * Returns the [ColorScheme.surface] color with an alpha of the [ColorScheme.primary] color overlaid
 * on top of it.
 * Computes the surface tonal color at different elevation levels e.g. surface1 through surface5.
 *
 * @param elevation Elevation value used to compute alpha of the color overlay layer.
 */
private fun ColorScheme.surfaceColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return primary.copy(alpha = alpha).compositeOver(surface)
}

private fun Color.surfaceColorAtElevation(
    elevation: Dp,
    surface: Color
): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return copy(alpha = alpha).compositeOver(surface)
}