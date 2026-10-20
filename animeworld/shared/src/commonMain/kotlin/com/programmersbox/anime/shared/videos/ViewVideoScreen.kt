package com.programmersbox.anime.shared.videos

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.BottomSheetDeleteScaffold
import com.programmersbox.kmpuiviews.theme.Emerald
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.saveable.rememberSaveable
import com.programmersbox.anime.shared.downloads.DownloadQueueSource
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data object VideoViewerRoute : NavKey

/**
 * Gates [content] behind whatever platform-specific runtime permissions are needed to read the
 * local video library (Android needs a runtime permission grant; desktop needs none since it just
 * scans a configured downloads folder).
 */
@Composable
internal expect fun VideoPermissionGate(content: @Composable () -> Unit)

/**
 * Renders a thumbnail for the video at [path]. Android extracts a real video frame; other
 * platforms fall back to a generic placeholder (see the design doc for this move — video-frame
 * extraction relies on Android-only Glide/`MediaMetadataRetriever` support).
 */
@Composable
internal expect fun VideoThumbnail(path: String, modifier: Modifier)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
)
@Composable
fun ViewVideoScreen(
    isCastActive: () -> Boolean = { false },
    onCastLoad: (SharedVideoContent) -> Unit = {},
    castButton: @Composable () -> Unit = {},
    deleteDialog: @Composable (SharedVideoContent, (Boolean) -> Unit) -> Unit = { _, _ -> },
) {
    VideoPermissionGate {
        VideoLoad(
            isCastActive = isCastActive,
            onCastLoad = onCastLoad,
            castButton = castButton,
            deleteDialog = deleteDialog,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
private fun VideoLoad(
    isCastActive: () -> Boolean,
    onCastLoad: (SharedVideoContent) -> Unit,
    castButton: @Composable () -> Unit,
    deleteDialog: @Composable (SharedVideoContent, (Boolean) -> Unit) -> Unit,
) {
    val videoLibrarySource = koinInject<VideoLibrarySource>()

    val hazeState = remember { HazeState() }

    val downloadQueueSource = koinInject<DownloadQueueSource>()

    val items by remember(videoLibrarySource) { videoLibrarySource.observeVideos() }.collectAsStateWithLifecycle(emptyList())
    val queue by remember(downloadQueueSource) { downloadQueueSource.observe() }.collectAsStateWithLifecycle(emptyList())

    var otherExpanded by rememberSaveable { mutableStateOf(false) }

    val state = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    BackHandler(state.bottomSheetState.currentValue == SheetValue.Expanded) {
        scope.launch { state.bottomSheetState.partialExpand() }
    }

    val surface = MaterialTheme.colorScheme.surface

    var itemToDelete by remember { mutableStateOf<SharedVideoContent?>(null) }

    itemToDelete?.let { pending ->
        deleteDialog(pending) { confirmed ->
            if (confirmed) videoLibrarySource.delete(pending)
            itemToDelete = null
        }
    }

    Box(
        modifier = Modifier.padding(bottom = LocalNavHostPadding.current.calculateBottomPadding())
    ) {
        BottomSheetDeleteScaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { BackButton() },
                    title = { Text("Downloads") },
                    actions = {
                        castButton()
                        IconButton(onClick = { scope.launch { state.bottomSheetState.expand() } }) { Icon(Icons.Default.Delete, null) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.hazeBlur(
                        HazeInput.Sources(hazeState),
                        style = HazeBlurStyle {
                            backgroundColor(surface)
                        }
                    )
                )
            },
            containerColor = Color.Transparent,
            state = state,
            listOfItems = items,
            multipleTitle = "Delete",
            deleteTitle = { it.videoName },
            onRemove = { itemToDelete = it },
            customSingleRemoveDialog = {
                itemToDelete = it
                false
            },
            onMultipleRemove = { downloadedItems ->
                downloadedItems.forEach { videoLibrarySource.delete(it) }
                downloadedItems.clear()
            },
            itemUi = { item -> VideoRowContent(item = item) },
        ) { p, itemList ->
            if (itemList.isEmpty() && queue.isEmpty()) {
                EmptyState(p)
            } else {
                val (downloaded, other) = remember(itemList) { itemList.partition { it.isDownload } }
                LazyColumn(
                    contentPadding = p,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState)
                ) {
                    if (queue.isNotEmpty()) {
                        item(key = "header_downloading") {
                            SectionHeader("Downloading · ${queue.size}", Modifier.animateItem())
                        }
                        items(
                            items = queue,
                            key = { "queue_${it.key}" }
                        ) {
                            QueuedDownloadRow(
                                item = it,
                                onCancel = { downloadQueueSource.cancel(it.key) },
                                onRetry = { downloadQueueSource.retry(it.key) },
                                onRemove = { downloadQueueSource.remove(it.key) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    item(key = "header_downloaded") {
                        SectionHeader("Downloaded · ${downloaded.size}", Modifier.animateItem())
                    }
                    items(
                        items = downloaded,
                        key = { it.path }
                    ) {
                        VideoContentView(
                            item = it,
                            isCastActive = isCastActive,
                            onCastLoad = onCastLoad,
                            deleteDialog = deleteDialog,
                            modifier = Modifier.animateItem()
                        )
                    }

                    if (other.isNotEmpty()) {
                        item(key = "header_other") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clickable { otherExpanded = !otherExpanded }
                                    .padding(end = 16.dp)
                            ) {
                                SectionHeader(
                                    "Other videos on this phone · ${other.size}",
                                    Modifier.weight(1f)
                                )
                                Icon(
                                    if (otherExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (otherExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (otherExpanded) {
                            items(
                                items = other,
                                key = { it.path }
                            ) {
                                VideoContentView(
                                    item = it,
                                    isCastActive = isCastActive,
                                    onCastLoad = onCastLoad,
                                    deleteDialog = deleteDialog,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "Download a Video",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                val navController = LocalNavActions.current

                Button(
                    onClick = { navController.popBackStack(Screen.RecentScreen, false) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 4.dp)
                ) { Text(text = "GO DOWNLOAD") }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@Composable
private fun VideoContentView(
    item: SharedVideoContent,
    isCastActive: () -> Boolean,
    onCastLoad: (SharedVideoContent) -> Unit,
    deleteDialog: @Composable (SharedVideoContent, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoLibrarySource = koinInject<VideoLibrarySource>()

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        deleteDialog(item) { confirmed ->
            showDeleteDialog = false
            if (confirmed) videoLibrarySource.delete(item)
        }
    }

    val navController = LocalNavActions.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                if (isCastActive()) {
                    onCastLoad(item)
                } else {
                    navController.navigate(
                        VideoScreen(
                            showPath = item.path,
                            showName = item.videoName,
                            downloadOrStream = true,
                            referer = ""
                        )
                    )
                }
            } else if (it == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                    SwipeToDismissBoxValue.StartToEnd -> Emerald
                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                }, label = ""
            )
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.PlayArrow
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> Icons.Default.Pages
            }
            val scale by animateFloatAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f, label = "")

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.scale(scale)
                )
            }
        },
        content = {
            VideoRow(
                item = item,
                onClick = {
                    if (isCastActive()) {
                        onCastLoad(item)
                    } else {
                        navController.navigate(
                            VideoScreen(
                                showPath = item.path,
                                showName = item.videoName,
                                downloadOrStream = true,
                                referer = ""
                            )
                        )
                    }
                },
                onRemove = { showDeleteDialog = true },
            )
        },
        modifier = modifier
    )
}
