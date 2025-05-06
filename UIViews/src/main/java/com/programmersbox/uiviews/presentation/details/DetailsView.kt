package com.programmersbox.uiviews.presentation.details

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import com.kmpalette.palette.graphics.Palette
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.ToolTipWrapper
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.utils.LocalCustomListDao
import com.programmersbox.kmpuiviews.utils.LocalItemDao
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.isScrollingUp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import me.tatarka.compose.collapsable.CollapsableColumn
import me.tatarka.compose.collapsable.rememberCollapsableTopBehavior
import my.nanihadesuka.compose.InternalLazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun DetailsView(
    info: KmpInfoModel,
    isSaved: Boolean,
    shareChapter: Boolean,
    chapters: List<ChapterWatched>,
    isFavorite: Boolean,
    onFavoriteClick: (Boolean) -> Unit,
    canNotify: Boolean,
    notifyAction: () -> Unit,
    markAs: (KmpChapterModel, Boolean) -> Unit,
    logo: NotificationLogo,
    description: String,
    onTranslateDescription: (MutableState<Boolean>) -> Unit,
    showDownloadButton: () -> Boolean,
    onPaletteSet: (Palette) -> Unit,
    blurHash: BitmapPainter?,
    onBitmapSet: (Bitmap) -> Unit,
    notificationRepository: NotificationRepository = koinInject(),
) {
    val hazeState = remember { HazeState() }
    val dao = LocalItemDao.current
    val genericInfo = LocalGenericInfo.current
    val navController = LocalNavController.current
    var reverseChapters by remember { mutableStateOf(false) }

    val settings = LocalSettingsHandling.current
    val showBlur by settings.rememberShowBlur()

    val hostState = remember { SnackbarHostState() }

    val listState = rememberLazyListState()

    val fabVisible = listState.isScrollingUp()//remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    val listDao = LocalCustomListDao.current

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberDrawerState(DrawerValue.Closed)

    val context = LocalContext.current

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(scaffoldState.isOpen) {
        scope.launch {
            try {
                if (scaffoldState.isOpen) scaffoldState.close()
                else navController.popBackStack()
            } catch (e: Exception) {
                navController.popBackStack()
            }
        }
    }

    //val bottomAppBarScrollBehavior = LocalBottomAppBarScrollBehavior.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
                    drawerState = scaffoldState,
                    info = info,
                    chapters = chapters,
                    markAs = markAs
                )
            }
        }
    ) {

        val collapsableBehavior = rememberCollapsableTopBehavior(
            enterAlways = false,
            canScroll = { !fabMenuExpanded }
        )

        val fabBlur = Modifier.blur(
            animateDpAsState(
                if (fabMenuExpanded) 2.dp else 0.dp
            ).value
        )

        OtakuScaffold(
            topBar = {
                CollapsableColumn(
                    behavior = collapsableBehavior,
                    modifier = fabBlur
                ) {
                    TopAppBar(
                        modifier = Modifier
                            .zIndex(2f)
                            .let { if (showBlur) it.hazeEffect(hazeState, HazeMaterials.thin()) else it },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (showBlur)
                                Color.Transparent
                            else
                                Color.Unspecified,
                        ),
                        title = {
                            Text(
                                info.title,
                                modifier = Modifier.basicMarquee()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
                                isSaved = isSaved,
                                dao = dao,
                                isFavorite = isFavorite,
                                canNotify = canNotify,
                                notifyAction = notifyAction,
                                onReverseChaptersClick = { reverseChapters = !reverseChapters },
                                onShowLists = { showLists = true },
                                addToForLater = {
                                    scope.launch {
                                        val result = OtakuApp.forLaterUuid?.let {
                                            listDao.addToList(
                                                it,
                                                info.title,
                                                info.description,
                                                info.url,
                                                info.imageUrl,
                                                info.source.serviceName
                                            )
                                        } == true

                                        hostState.showSnackbar(
                                            context.getString(
                                                if (result) {
                                                    R.string.added_to_list
                                                } else {
                                                    R.string.already_in_list
                                                },
                                                context.getString(R.string.for_later)
                                            ),
                                            withDismissAction = true
                                        )
                                    }
                                }
                            ) {
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
                                            contentDescription = if (expanded) "Expand" else "Collapse",
                                        )
                                    }
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )

                    DetailsHeader(
                        model = info,
                        logo = painterResource(id = logo.notificationId),
                        isFavorite = isFavorite,
                        favoriteClick = onFavoriteClick,
                        onPaletteSet = onPaletteSet,
                        onBitmapSet = onBitmapSet,
                        blurHash = blurHash,
                        modifier = Modifier.collapse(),
                    )
                }
            },
            floatingActionButton = {
                DetailFloatingActionButtonMenu(
                    navController = navController,
                    isVisible = fabVisible,
                    onShowLists = { showLists = true },
                    info = info,
                    removeFromSaved = {
                        scope.launch(Dispatchers.IO) {
                            dao.getNotificationItemFlow(info.url)
                                .firstOrNull()
                                ?.let {
                                    dao.deleteNotification(it)
                                    notificationRepository.cancelNotification(it)
                                }
                        }
                    },
                    isSaved = isSaved,
                    addToSaved = {
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
                    canNotify = canNotify,
                    notifyAction = notifyAction,
                    isFavorite = isFavorite,
                    onFavoriteClick = onFavoriteClick,
                    fabMenuExpanded = fabMenuExpanded,
                    onFabMenuExpandedChange = { fabMenuExpanded = it },
                    modifier = Modifier.padding(LocalNavHostPadding.current)
                )
            },
            snackbarHost = {
                SnackbarHost(hostState) { data ->
                    val background = SnackbarDefaults.color
                    val font = SnackbarDefaults.contentColor
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
                .nestedScroll(collapsableBehavior.nestedScrollConnection)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { p ->
            val modifiedPaddingValues = p// - LocalNavHostPadding.current
            var descriptionVisibility by remember { mutableStateOf(false) }
            val listOfChapters = remember(reverseChapters) {
                info.chapters.let { if (reverseChapters) it.reversed() else it }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                state = listState,
                contentPadding = modifiedPaddingValues,
                userScrollEnabled = !fabMenuExpanded,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .let {
                        if (showBlur)
                            it.hazeSource(hazeState)
                        else
                            it
                    }
                    .then(fabBlur),
            ) {
                if (info.description.isNotEmpty()) {
                    item {
                        Box {
                            val progress = remember { mutableStateOf(false) }

                            Text(
                                description,
                                modifier = Modifier
                                    .combinedClickable(
                                        interactionSource = null,
                                        indication = ripple(),
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

                items(listOfChapters) { c ->
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
                    state = listState,
                    settings = ScrollbarSettings.Default.copy(
                        thumbThickness = 8.dp,
                        scrollbarPadding = 2.dp,
                        thumbUnselectedColor = MaterialTheme.colorScheme.primary,
                        thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = .6f),
                    ),
                )
            }
        }
    }
}