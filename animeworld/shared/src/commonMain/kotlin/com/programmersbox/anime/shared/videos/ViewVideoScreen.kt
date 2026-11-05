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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.saveable.rememberSaveable
import com.programmersbox.anime.shared.downloads.DownloadQueueSource
import kotlinx.serialization.Serializable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.graphicsLayer
import org.koin.compose.koinInject
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.programmersbox.anime.shared.downloads.splitEpisodeTitle
import com.programmersbox.anime.shared.subtitlegen.GenerateSubtitlesDialog
import com.programmersbox.anime.shared.subtitlegen.GenerationStatus
import com.programmersbox.anime.shared.subtitlegen.SubtitleGenerator
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRoute
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

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

/** Where the row menu's "Export…" goes for the video at [path]; null hides it (only desktop has ffmpeg export). */
internal expect fun videoExportRoute(path: String): NavKey?

/** The local subtitle generator; null hides "Generate subtitles…" (only desktop runs whisper.cpp). */
@Composable
internal expect fun rememberSubtitleGenerator(): SubtitleGenerator?

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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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

    val navActions = LocalNavActions.current

    val items by remember(videoLibrarySource) { videoLibrarySource.observeVideos() }.collectAsStateWithLifecycle(emptyList())
    val queue by remember(downloadQueueSource) { downloadQueueSource.observe() }.collectAsStateWithLifecycle(emptyList())

    // null until the header is tapped: folder-grouped other videos (desktop) start open, the phone's start closed.
    var otherExpandedChoice by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var collapsedFolders by remember { mutableStateOf(emptySet<String>()) }
    // null = all; "" = AnimeWorld downloads only; otherwise a library folder label.
    var folderFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val generator = rememberSubtitleGenerator()
    val generations by remember(generator) { generator?.statuses ?: flowOf(emptyMap()) }
        .collectAsStateWithLifecycle(emptyMap())
    var generateFor by remember { mutableStateOf<SharedVideoContent?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val currentItems by rememberUpdatedState(items)
    LaunchedEffect(generator) {
        generator?.finished?.collect { path ->
            val item = currentItems.firstOrNull { it.path == path } ?: return@collect
            // Launched so a second finish queues its snackbar instead of waiting to be collected.
            launch {
                val title = splitEpisodeTitle(item.videoName.substringBeforeLast('.'))
                val result = snackbarHostState.showSnackbar(
                    "Subtitles ready for ${title.episode ?: title.show}",
                    actionLabel = "Play"
                )
                if (result == SnackbarResult.ActionPerformed) navActions.navigate(item.videoScreen())
            }
        }
    }

    if (generator != null) {
        generateFor?.let { item ->
            val model by generator.model.collectAsStateWithLifecycle()
            val initial = remember(item) { generator.lastRequest.value }
            GenerateSubtitlesDialog(
                title = remember(item) { splitEpisodeTitle(item.videoName.substringBeforeLast('.')) },
                initial = initial,
                model = model,
                onOpenSettings = {
                    generateFor = null
                    navActions.navigate(SubtitleSettingsRoute)
                },
                onGenerate = { request, rememberChoices ->
                    generator.enqueue(item.path, request, rememberChoices)
                    generateFor = null
                },
                onDismiss = { generateFor = null }
            )
        }
    }

    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier.padding(bottom = LocalNavHostPadding.current.calculateBottomPadding())
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { BackButton() },
                    title = { Text("Downloads") },
                    actions = {
                        castButton()
                        libraryFoldersRoute()?.let { route ->
                            IconButton(onClick = { navActions.navigate(route) }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Library folders")
                            }
                        }
                        IconButton(onClick = { navActions.navigate(FreeUpSpaceRoute) }) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "Free up space")
                        }
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
        ) { p ->
            if (items.isEmpty() && queue.isEmpty()) {
                EmptyState(p)
            } else {
                val (allDownloaded, allOther) = remember(items) { items.partition { it.isDownload } }
                val folders = remember(allOther) { allOther.mapNotNull { it.folder }.distinct() }
                val filter = folderFilter?.takeIf { it == "" || it in folders }
                val downloaded = if (filter == null || filter == "") allDownloaded else emptyList()
                val other = when (filter) {
                    null -> allOther
                    "" -> emptyList()
                    else -> allOther.filter { it.folder == filter }
                }
                val otherExpanded = otherExpandedChoice ?: folders.isNotEmpty()
                LazyColumn(
                    contentPadding = p,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState)
                ) {
                    item(key = "thumbnail_notice") { ThumbnailProgressNotice(Modifier.animateItem()) }

                    if (folders.isNotEmpty()) {
                        item(key = "folder_chips") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                (listOf(null, "") + folders).forEach { option ->
                                    FilterChip(
                                        selected = filter == option,
                                        onClick = { folderFilter = option },
                                        label = { Text(option?.ifEmpty { "Downloaded" } ?: "All") },
                                        leadingIcon = if (filter == option) {
                                            { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (queue.isNotEmpty() && filter == null) {
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

                    if (filter == null || filter == "") {
                        item(key = "header_downloaded") {
                            SectionHeader("Downloaded · ${downloaded.size}", Modifier.animateItem())
                        }
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
                            generation = generations[it.path],
                            generator = generator,
                            onGenerateSubtitles = { generateFor = it },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "other_prompt") { OtherVideosPrompt(Modifier.animateItem()) }

                    if (other.isNotEmpty()) {
                        item(key = "header_other") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clickable { otherExpandedChoice = !otherExpanded }
                                    .padding(end = 16.dp)
                            ) {
                                SectionHeader(
                                    "Other videos on this $libraryDeviceName · ${other.size}",
                                    Modifier.weight(1f)
                                )
                                libraryFoldersRoute()?.let { route ->
                                    TextButton(onClick = { navActions.navigate(route) }) { Text("Manage folders") }
                                }
                                Icon(
                                    if (otherExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (otherExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (otherExpanded) {
                            other.groupBy { it.folder }.forEach { (folder, folderVideos) ->
                                val collapsed = folder != null && folder in collapsedFolders
                                if (folder != null) {
                                    item(key = "folder_$folder") {
                                        FolderHeader(
                                            label = "$folder · ${folderVideos.size}",
                                            collapsed = collapsed,
                                            onClick = {
                                                collapsedFolders = if (collapsed) collapsedFolders - folder else collapsedFolders + folder
                                            },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                                if (!collapsed) {
                                    items(
                                        items = folderVideos,
                                        key = { it.path }
                                    ) {
                                        VideoContentView(
                                            item = it,
                                            isCastActive = isCastActive,
                                            onCastLoad = onCastLoad,
                                            deleteDialog = deleteDialog,
                                            generation = generations[it.path],
                                            generator = generator,
                                            onGenerateSubtitles = { generateFor = it },
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
    }
}

@Composable
private fun FolderHeader(
    label: String,
    collapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (collapsed) -90f else 0f, label = "folderChevron")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    ) {
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = if (collapsed) "Expand" else "Collapse",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = rotation }
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

        OtherVideosPrompt(Modifier.padding(top = 120.dp))
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
    generation: GenerationStatus?,
    generator: SubtitleGenerator?,
    onGenerateSubtitles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deleteVideos = rememberVideoDeleter()

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        deleteDialog(item) { confirmed ->
            showDeleteDialog = false
            if (confirmed) deleteVideos(listOf(item))
        }
    }

    val navController = LocalNavActions.current

    // Swipe left to delete only; playing is a tap.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) showDeleteDialog = true
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                    else -> Color.Transparent
                }, label = ""
            )
            val scale by animateFloatAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f, label = "")

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
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
                        navController.navigate(item.videoScreen())
                    }
                },
                onRemove = { showDeleteDialog = true },
                onExport = videoExportRoute(item.path)?.let { route -> { navController.navigate(route) } },
                onGenerateSubtitles = if (generator != null) onGenerateSubtitles else null,
                generation = generation,
                onCancelGeneration = { generator?.cancel(item.path) },
                onRetryGeneration = {
                    (generation as? GenerationStatus.Failed)?.let { generator?.enqueue(item.path, it.request, false) }
                },
                onDismissGeneration = { generator?.dismiss(item.path) },
            )
        },
        modifier = modifier
    )
}

/** The player route for a library video; the row tap and the "Subtitles ready" snackbar both use it. */
private fun SharedVideoContent.videoScreen() = VideoScreen(
    showPath = path,
    showName = videoName,
    downloadOrStream = true,
    referer = ""
)
