package com.programmersbox.anime.shared.videos

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
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
import com.programmersbox.kmpuiviews.presentation.components.ImageFlushListItem
import com.programmersbox.kmpuiviews.theme.Emerald
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
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
 * extraction relies on Android-only Coil/`MediaMetadataRetriever` support).
 */
@Composable
internal expect fun VideoThumbnail(path: String, modifier: Modifier)

private fun Long.toDurationText(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) "${hours.toString().padStart(2, '0')}:$mm:$ss" else "$mm:$ss"
}

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

    val items by videoLibrarySource.observeVideos().collectAsStateWithLifecycle(emptyList())

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
                    title = { Text("Downloaded Videos") },
                    actions = {
                        castButton()
                        IconButton(onClick = { scope.launch { state.bottomSheetState.expand() } }) { Icon(Icons.Default.Delete, null) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.hazeEffect(hazeState) {
                        blurEffect {
                            backgroundColor = surface
                        }
                    }
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
            itemUi = { item ->
                ImageFlushListItem(
                    leadingContent = {
                        Box {
                            val runTimeString = remember(item.duration) { item.duration.toDurationText() }

                            VideoThumbnail(
                                path = item.path,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(ComposableUtils.IMAGE_HEIGHT, ComposableUtils.IMAGE_WIDTH)
                            )

                            Text(
                                runTimeString,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color(0x99000000))
                                    .border(BorderStroke(1.dp, Color(0x00000000)), shape = RoundedCornerShape(4.dp))
                            )
                        }
                    },
                    overlineContent = {
                        if (item.lastPlayedPositionMs > 0) Text(item.lastPlayedPositionMs.toDurationText())
                    },
                    headlineContent = { Text(item.videoName) },
                    supportingContent = { Text(item.path) }
                )
            },
        ) { p, itemList ->
            if (itemList.isEmpty()) {
                EmptyState(p)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = p,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState)
                ) {
                    items(
                        items = itemList,
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
            ElevatedCard(
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
                modifier = Modifier.fillMaxSize()
            ) {
                ImageFlushListItem(
                    leadingContent = {
                        Box {
                            val runTimeString = remember(item.duration) { item.duration.toDurationText() }

                            VideoThumbnail(
                                path = item.path,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(ComposableUtils.IMAGE_HEIGHT, ComposableUtils.IMAGE_WIDTH)
                            )

                            Text(
                                runTimeString,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color(0x99000000))
                                    .border(BorderStroke(1.dp, Color(0x00000000)), shape = RoundedCornerShape(bottomEnd = 4.dp))
                            )
                        }
                    },
                    overlineContent = {
                        if (item.lastPlayedPositionMs > 0) Text(item.lastPlayedPositionMs.toDurationText())
                    },
                    headlineContent = {
                        Text(item.videoName)
                    },
                    supportingContent = {
                        Text(item.path)
                    },
                    trailingContent = {
                        var showDropDown by remember { mutableStateOf(false) }

                        val dropDownDismiss = { showDropDown = false }

                        DropdownMenu(
                            expanded = showDropDown,
                            onDismissRequest = dropDownDismiss
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    showDeleteDialog = true
                                },
                                text = { Text("Remove") }
                            )
                        }

                        IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }
                    }
                )
            }
        },
        modifier = modifier
    )
}
