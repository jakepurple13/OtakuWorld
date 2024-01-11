@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.programmersbox.uiviews.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDownCircle
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
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.rotate
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
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.notifications.cancelNotification
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalCustomListDao
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.animate
import com.programmersbox.uiviews.utils.components.OtakuScaffold
import com.programmersbox.uiviews.utils.components.minus
import com.programmersbox.uiviews.utils.toComposeColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import me.tatarka.compose.collapsable.CollapsableColumn
import me.tatarka.compose.collapsable.rememberCollapsableTopBehavior
import my.nanihadesuka.compose.InternalLazyColumnScrollbar

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun DetailsView(
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
    val hazeState = remember { HazeState() }
    val dao = LocalItemDao.current
    val swatchInfo = LocalSwatchInfo.current.colors
    val genericInfo = LocalGenericInfo.current
    val navController = LocalNavController.current
    var reverseChapters by remember { mutableStateOf(false) }

    val hostState = remember { SnackbarHostState() }

    val listState = rememberLazyListState()

    val listDao = LocalCustomListDao.current

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberDrawerState(DrawerValue.Closed)

    val context = LocalContext.current

    BackHandler(scaffoldState.isOpen) {
        scope.launch {
            try {
                when {
                    scaffoldState.isOpen -> scaffoldState.close()
                    else -> navController.popBackStack()
                }
            } catch (e: Exception) {
                navController.popBackStack()
            }
        }
    }

    val topBarColor by animateColorAsState(
        swatchInfo?.bodyColor?.toComposeColor() ?: MaterialTheme.colorScheme.onSurface,
        label = ""
    )

    val bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

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
                    drawerState = scaffoldState,
                    info = info,
                    chapters = chapters,
                    markAs = markAs
                )
            }
        }
    ) {
        val b = MaterialTheme.colorScheme.background
        val c by animateColorAsState(swatchInfo?.rgb?.toComposeColor() ?: b, label = "")

        val collapsableBehavior = rememberCollapsableTopBehavior(
            enterAlways = false
        )

        OtakuScaffold(
            containerColor = Color.Transparent,
            topBar = {
                CollapsableColumn(
                    behavior = collapsableBehavior
                ) {
                    InsetSmallTopAppBar(
                        modifier = Modifier
                            .zIndex(2f)
                            .hazeChild(hazeState),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = topBarColor
                        ),
                        title = {
                            Text(
                                info.title,
                                modifier = Modifier.basicMarquee()
                            )
                        },
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

                    DetailsHeader(
                        model = info,
                        logo = painterResource(id = logo.notificationId),
                        isFavorite = isFavorite,
                        favoriteClick = onFavoriteClick,
                        modifier = Modifier.collapse()
                    )
                }
            },
            bottomBar = {
                val notificationManager = LocalContext.current.notificationManager
                DetailBottomBar(
                    navController = navController,
                    onShowLists = { showLists = true },
                    info = info,
                    customActions = {
                        val expanded by remember { derivedStateOf { collapsableBehavior.state.collapsedFraction >= 0.5f } }

                        ToolTipWrapper(
                            info = { Text("${if (expanded) "Show" else "Hide"} Details") }
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (expanded) collapsableBehavior.state.animateExpand()
                                        else collapsableBehavior.state.animateCollapse()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.ArrowDropDownCircle,
                                    modifier = Modifier.rotate(180 * (1 - collapsableBehavior.state.collapsedFraction)),
                                    contentDescription = if (expanded) "Expand" else "Collapse"
                                )
                            }
                        }
                    },
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
                    bottomAppBarScrollBehavior = bottomAppBarScrollBehavior,
                    topBarColor = topBarColor,
                    isFavorite = isFavorite,
                    onFavoriteClick = onFavoriteClick,
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
                        .hazeChild(hazeState)
                )
            },
            snackbarHost = {
                SnackbarHost(hostState) { data ->
                    val background = swatchInfo?.rgb?.toComposeColor() ?: SnackbarDefaults.color
                    val font = swatchInfo?.titleColor?.toComposeColor() ?: MaterialTheme.colorScheme.surface
                    Snackbar(
                        containerColor = Color(
                            ColorUtils.blendARGB(
                                background.toArgb(),
                                MaterialTheme.colorScheme.onSurface.toArgb(),
                                .25f
                            )
                        ),
                        contentColor = font,
                        snackbarData = data
                    )
                }
            },
            modifier = Modifier
                .drawBehind { drawRect(Brush.verticalGradient(listOf(c, b))) }
                .nestedScroll(collapsableBehavior.nestedScrollConnection)
                .nestedScroll(bottomAppBarScrollBehavior.nestedScrollConnection)
        ) { p ->
            val modifiedPaddingValues = p - LocalNavHostPadding.current
            var descriptionVisibility by remember { mutableStateOf(false) }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                state = listState,
                contentPadding = modifiedPaddingValues,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .haze(
                        hazeState,
                        backgroundColor = swatchInfo?.rgb
                            ?.toComposeColor()
                            ?.animate()?.value ?: MaterialTheme.colorScheme.surface
                    ),
            ) {
                if (info.description.isNotEmpty()) {
                    item {
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
                                maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
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
            Box(Modifier.padding(modifiedPaddingValues)) {
                InternalLazyColumnScrollbar(
                    thickness = 8.dp,
                    padding = 2.dp,
                    listState = listState,
                    thumbColor = swatchInfo?.bodyColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                    thumbSelectedColor = (swatchInfo?.bodyColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary).copy(alpha = .6f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolTipWrapper(
    info: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    tooltipState: TooltipState = rememberTooltipState(),
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { RichTooltip { info() } },
        state = tooltipState,
        modifier = modifier,
        content = content
    )
}