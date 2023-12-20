package com.programmersbox.uiviews.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.google.accompanist.adaptive.calculateDisplayFeatures
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.notifications.cancelNotification
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalActivity
import com.programmersbox.uiviews.utils.LocalCustomListDao
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.animate
import com.programmersbox.uiviews.utils.components.NormalOtakuScaffold
import com.programmersbox.uiviews.utils.components.OtakuScaffold
import com.programmersbox.uiviews.utils.toComposeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar

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
    showDownloadButton: Boolean,
) {
    val dao = LocalItemDao.current
    val listDao = LocalCustomListDao.current
    val swatchInfo = LocalSwatchInfo.current.colors
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
            } catch (e: Exception) {
                navController.popBackStack()
            }
        }
    }

    val topBarColor = swatchInfo?.bodyColor?.toComposeColor()?.animate()?.value ?: MaterialTheme.colorScheme.onSurface

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
                    topBarColor = topBarColor,
                    drawerState = scaffoldState,
                    info = info,
                    chapters = chapters,
                    markAs = markAs
                )
            }
        }
    ) {
        OtakuScaffold(
            containerColor = Color.Transparent,
            topBar = {
                InsetSmallTopAppBar(
                    modifier = Modifier.zIndex(2f),
                    colors = TopAppBarDefaults.topAppBarColors(
                        titleContentColor = topBarColor,
                        containerColor = swatchInfo?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = swatchInfo?.rgb?.toComposeColor()?.animate()?.value?.let {
                            MaterialTheme.colorScheme.surface.surfaceColorAtElevation(1.dp, it)
                        } ?: MaterialTheme.colorScheme.applyTonalElevation(
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            elevation = 1.dp
                        )
                    ),
                    scrollBehavior = scrollBehavior,
                    title = { Text(info.title) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = topBarColor)
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
                            topBarColor = topBarColor,
                            isSaved = isSaved,
                            dao = dao,
                            onReverseChaptersClick = { reverseChapters = !reverseChapters },
                            onShowLists = { showLists = true }
                        )
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState) { data ->
                    val background = swatchInfo?.rgb?.toComposeColor() ?: SnackbarDefaults.color
                    val font = swatchInfo?.titleColor?.toComposeColor() ?: MaterialTheme.colorScheme.surface
                    Snackbar(
                        containerColor = Color(ColorUtils.blendARGB(background.toArgb(), MaterialTheme.colorScheme.onSurface.toArgb(), .25f)),
                        contentColor = font,
                        snackbarData = data
                    )
                }
            },
            modifier = Modifier
                .run {
                    val b = MaterialTheme.colorScheme.background
                    val c by animateColorAsState(swatchInfo?.rgb?.toComposeColor() ?: b, label = "")
                    drawBehind { drawRect(Brush.verticalGradient(listOf(c, b))) }
                }
                .nestedScroll(scrollBehavior.nestedScrollConnection)
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
                modifier = Modifier.padding(p)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    showDownloadButton: Boolean,
    modifier: Modifier = Modifier,
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

    TwoPane(
        modifier = modifier,
        first = {
            val swatchInfo = LocalSwatchInfo.current.colors
            val topBarColor by animateColorAsState(
                swatchInfo?.bodyColor?.toComposeColor() ?: MaterialTheme.colorScheme.onSurface,
                label = ""
            )
            val b = MaterialTheme.colorScheme.surface
            val c by animateColorAsState(swatchInfo?.rgb?.toComposeColor() ?: b, label = "")
            NormalOtakuScaffold(
                bottomBar = {
                    val notificationManager = LocalContext.current.notificationManager
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
                                        notificationManager.cancelNotification(it)
                                    }
                            }
                        },
                        isSaved = isSaved,
                        windowInsets = BottomAppBarDefaults.windowInsets,
                        topBarColor = topBarColor,
                        containerColor = c,
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
                            }
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
                        possibleDescription = {
                            if (info.description.isNotEmpty()) {
                                var descriptionVisibility by remember { mutableStateOf(false) }
                                Box {
                                    val progress = remember { mutableStateOf(false) }

                                    Text(
                                        description,
                                        modifier = Modifier
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = rememberRipple(),
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
        second = {
            val swatchInfo = LocalSwatchInfo.current.colors
            LazyColumnScrollbar(
                enabled = true,
                thickness = 8.dp,
                padding = 2.dp,
                listState = listState,
                thumbColor = swatchInfo?.bodyColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                thumbSelectedColor = (swatchInfo?.bodyColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary).copy(alpha = .6f),
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxHeight(),
                    state = listState
                ) {
                    items(info.chapters.let { if (reverseChapters) it.reversed() else it }) { c ->
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
        displayFeatures = calculateDisplayFeatures(activity = LocalActivity.current),
        strategy = HorizontalTwoPaneStrategy(splitFraction = 0.5f)
    )
}