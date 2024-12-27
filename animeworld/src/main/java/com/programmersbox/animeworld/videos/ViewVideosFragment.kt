@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.programmersbox.animeworld.videos

import android.Manifest
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.mediarouter.app.MediaRouteButton
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.programmersbox.animeworld.MainActivity
import com.programmersbox.animeworld.R
import com.programmersbox.animeworld.SlideToDeleteDialog
import com.programmersbox.animeworld.VideoContent
import com.programmersbox.animeworld.navigateToVideoPlayer
import com.programmersbox.helpfulutils.stringForTime
import com.programmersbox.uiviews.presentation.Screen
import com.programmersbox.uiviews.presentation.components.BottomSheetDeleteScaffold
import com.programmersbox.uiviews.presentation.components.CoilGradientImage
import com.programmersbox.uiviews.presentation.components.ImageFlushListItem
import com.programmersbox.uiviews.presentation.components.PermissionRequest
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.Emerald
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
)
@Composable
fun ViewVideoScreen() {
    PermissionRequest(
        if (Build.VERSION.SDK_INT >= 33)
            listOf(Manifest.permission.READ_MEDIA_VIDEO)
        else listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    ) {
        val context = LocalContext.current
        val viewModel: ViewVideoViewModel = viewModel { ViewVideoViewModel(context) }
        VideoLoad(viewModel)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
private fun VideoLoad(viewModel: ViewVideoViewModel) {

    val hazeState = remember { HazeState() }
    val context = LocalContext.current

    val coilImageLoader = remember {
        ImageLoader(context).newBuilder()
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    val items = viewModel.videos

    val state = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    BackHandler(state.bottomSheetState.currentValue == SheetValue.Expanded) {
        scope.launch { state.bottomSheetState.partialExpand() }
    }

    val surface = MaterialTheme.colorScheme.surface

    var itemToDelete by remember { mutableStateOf<VideoContent?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    itemToDelete?.let { SlideToDeleteDialog(showDialog = showDialog, onDialogDismiss = { showDialog = it }, video = it) }

    Box(
        modifier = Modifier.padding(bottom = LocalNavHostPadding.current.calculateBottomPadding())
    ) {
        BottomSheetDeleteScaffold(
            topBar = {
                InsetSmallTopAppBar(
                    navigationIcon = { BackButton() },
                    title = { Text(stringResource(R.string.downloaded_videos)) },
                    actions = {
                        AndroidView(
                            factory = { context ->
                                MediaRouteButton(context).apply {
                                    MainActivity.cast.showIntroductoryOverlay(this)
                                    MainActivity.cast.setMediaRouteMenu(context, this)
                                }
                            }
                        )
                        IconButton(onClick = { scope.launch { state.bottomSheetState.expand() } }) { Icon(Icons.Default.Delete, null) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.hazeChild(hazeState) {
                        backgroundColor = surface
                    }
                )
            },
            containerColor = Color.Transparent,
            state = state,
            listOfItems = items,
            multipleTitle = stringResource(id = R.string.delete),
            deleteTitle = { it.videoName.orEmpty() },
            onRemove = {
                itemToDelete = it
                showDialog = true
            },
            customSingleRemoveDialog = {
                itemToDelete = it
                showDialog = true
                false
            },
            onMultipleRemove = { downloadedItems ->
                downloadedItems.forEach {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            it.assetFileStringUri?.toUri()?.let { it1 ->
                                context.contentResolver.delete(
                                    it1,
                                    "${MediaStore.Video.Media._ID} = ?",
                                    arrayOf(it.videoId.toString())
                                )
                            }
                        } else {
                            File(it.path!!).delete()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Something went wrong with ${it.videoName}", Toast.LENGTH_SHORT).show()
                    }
                }
                downloadedItems.clear()
            },
            itemUi = { item ->
                ImageFlushListItem(
                    leadingContent = {
                        Box {
                            /*convert millis to appropriate time*/
                            val runTimeString = remember {
                                val duration = item.videoDuration
                                if (duration > TimeUnit.HOURS.toMillis(1)) {
                                    String.format(
                                        "%02d:%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(duration),
                                        TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(
                                            TimeUnit.MILLISECONDS.toHours(duration)
                                        ),
                                        TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                                            TimeUnit.MILLISECONDS.toMinutes(duration)
                                        )
                                    )
                                } else {
                                    String.format(
                                        "%02d:%02d",
                                        TimeUnit.MILLISECONDS.toMinutes(duration),
                                        TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                                            TimeUnit.MILLISECONDS.toMinutes(duration)
                                        )
                                    )
                                }
                            }

                            CoilGradientImage(
                                model = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item.assetFileStringUri.orEmpty())
                                        .lifecycle(LocalLifecycleOwner.current)
                                        .crossfade(true)
                                        .size(ComposableUtils.IMAGE_HEIGHT_PX, ComposableUtils.IMAGE_WIDTH_PX)
                                        .videoFramePercent(.1)
                                        .build(),
                                    imageLoader = coilImageLoader
                                ),
                                contentScale = ContentScale.Crop,
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
                        if (context.getSharedPreferences("videos", Context.MODE_PRIVATE).contains(item.path))
                            Text(context.getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(item.path, 0).stringForTime())
                    },
                    headlineContent = { Text(item.videoName.orEmpty()) },
                    supportingContent = { Text(item.path.orEmpty()) }
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
                        .haze(hazeState)
                ) {
                    items(
                        items = itemList,
                        key = { it.videoId }
                    ) {
                        VideoContentView(
                            item = it,
                            imageLoader = coilImageLoader,
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
                    text = stringResource(id = R.string.get_started),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = stringResource(id = R.string.download_a_video),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                val navController = LocalNavController.current

                Button(
                    onClick = { navController.popBackStack(Screen.RecentScreen, false) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 4.dp)
                ) { Text(text = stringResource(id = R.string.go_download)) }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@Composable
private fun VideoContentView(
    item: VideoContent,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    SlideToDeleteDialog(showDialog = showDialog, onDialogDismiss = { showDialog = it }, video = item)

    val navController = LocalNavController.current
    val context = LocalContext.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                if (MainActivity.cast.isCastActive()) {
                    MainActivity.cast.loadMedia(
                        File(item.path!!),
                        context.getSharedPreferences("videos", Context.MODE_PRIVATE)?.getLong(item.assetFileStringUri, 0) ?: 0L,
                        null, null
                    )
                } else {
                    context.navigateToVideoPlayer(
                        navController,
                        item.assetFileStringUri,
                        item.videoName,
                        true,
                        ""
                    )
                }
            } else if (it == SwipeToDismissBoxValue.EndToStart) {
                showDialog = true
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
                    if (MainActivity.cast.isCastActive()) {
                        MainActivity.cast.loadMedia(
                            File(item.path!!),
                            context
                                .getSharedPreferences("videos", Context.MODE_PRIVATE)
                                .getLong(item.assetFileStringUri, 0),
                            null, null
                        )
                    } else {
                        context.navigateToVideoPlayer(
                            navController,
                            item.assetFileStringUri,
                            item.videoName,
                            true,
                            ""
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                ImageFlushListItem(
                    leadingContent = {
                        Box {
                            /*convert millis to appropriate time*/
                            val runTimeString = remember {
                                val duration = item.videoDuration
                                if (duration > TimeUnit.HOURS.toMillis(1)) {
                                    String.format(
                                        "%02d:%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(duration),
                                        TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
                                        TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                                            TimeUnit.MILLISECONDS.toMinutes(
                                                duration
                                            )
                                        )
                                    )
                                } else {
                                    String.format(
                                        "%02d:%02d",
                                        TimeUnit.MILLISECONDS.toMinutes(duration),
                                        TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                                            TimeUnit.MILLISECONDS.toMinutes(
                                                duration
                                            )
                                        )
                                    )
                                }
                            }

                            CoilGradientImage(
                                model = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item.assetFileStringUri.orEmpty())
                                        .lifecycle(LocalLifecycleOwner.current)
                                        .crossfade(true)
                                        .size(ComposableUtils.IMAGE_HEIGHT_PX, ComposableUtils.IMAGE_WIDTH_PX)
                                        .videoFramePercent(.1)
                                        .build(),
                                    imageLoader = imageLoader
                                ),
                                contentScale = ContentScale.Crop,
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
                        val shared = remember { context.getSharedPreferences("videos", Context.MODE_PRIVATE) }
                        if (shared.contains(item.assetFileStringUri))
                            Text(shared.getLong(item.assetFileStringUri, 0).stringForTime())
                    },
                    headlineContent = {
                        Text(item.videoName.orEmpty())
                    },
                    supportingContent = {
                        Text(item.path.orEmpty())
                    },
                    trailingContent = {
                        var showDropDown by remember { mutableStateOf(false) }

                        val dropDownDismiss = { showDropDown = false }

                        androidx.compose.material3.DropdownMenu(
                            expanded = showDropDown,
                            onDismissRequest = dropDownDismiss
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    showDialog = true
                                },
                                text = { Text(stringResource(R.string.remove)) }
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