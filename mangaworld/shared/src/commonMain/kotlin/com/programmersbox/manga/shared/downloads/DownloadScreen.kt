package com.programmersbox.manga.shared.downloads

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
)
@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text("Downloaded Chapters") },
                navigationIcon = { BackButton() }
            )
        }
    ) { p1 ->
        PermissionRequester {
            DownloadViewer(viewModel, p1)
        }
    }
}

@Composable
expect fun PermissionRequester(content: @Composable () -> Unit)

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
private fun DownloadViewer(
    viewModel: DownloadViewModel,
    p1: PaddingValues,
    mangaSettingsHandling: MangaNewSettingsHandling = koinInject(),
) {
    val useNewReader by mangaSettingsHandling
        .useNewReader
        .flow
        .collectAsStateWithLifecycle(initialValue = true)

    val fileList by viewModel.fileList.collectAsStateWithLifecycle()

    val fileEntries = remember(fileList) { fileList.entries.toList() }

    val activeDownloads by viewModel
        .activeDownloads
        .collectAsStateWithLifecycle()

    val inProgressDownloads by remember {
        derivedStateOf {
            activeDownloads.filter {
                it.state is DownloadState.Queued || it.state is DownloadState.Downloading
            }
        }
    }

    if (fileList.isEmpty() && inProgressDownloads.isEmpty()) {
        EmptyState(p1)
    } else {
        LazyColumn(
            contentPadding = p1,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            if (inProgressDownloads.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Downloads",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                items(
                    items = inProgressDownloads,
                ) { download ->
                    ActiveDownloadItem(
                        download = download,
                        onCancel = { viewModel.cancelDownload(it) }
                    )
                }
            }

            if (fileList.isNotEmpty()) {
                if (inProgressDownloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "Downloaded",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                items(
                    items = fileEntries,
                    key = { it.key },
                ) { file ->
                    ChapterItem(
                        file = file,
                        useNewReader = useNewReader,
                        onDeleted = viewModel::delete
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveDownloadItem(
    download: ChapterDownloadProgress,
    onCancel: (String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            modifier = Modifier.padding(4.dp),
            headlineContent = { Text(download.chapterName) },
            supportingContent = { Text(download.mangaTitle) },
            leadingContent = {
                when (val state = download.state) {
                    is DownloadState.Downloading -> {
                        val fraction = if (state.totalImages > 0)
                            state.imagesDownloaded.toFloat() / state.totalImages
                        else 0f
                        CircularProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }

                    else -> CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (download.state is DownloadState.Downloading) {
                        val state = download.state as DownloadState.Downloading
                        if (state.totalImages > 0) {
                            Text(
                                text = "${state.imagesDownloaded}/${state.totalImages}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    IconButton(onClick = { onCancel(download.chapterUrl) }) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel download")
                    }
                }
            }
        )
    }
}

@Composable
private fun EmptyState(p1: PaddingValues) {
    val navController = LocalNavActions.current
    Box(
        modifier = Modifier
            .padding(p1)
            .fillMaxSize()
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(modifier = Modifier) {

                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "Download a Manga",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Button(
                    onClick = { navController.popBackStack(Screen.RecentScreen, false) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 4.dp)
                ) { Text(text = "Go Download!") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterItem(
    file: Map.Entry<String, Map<String, List<DownloadedChapters>>>,
    onDeleted: (DownloadedChapters) -> Unit,
    useNewReader: Boolean = true,
) {
    val chapterHolder: ChapterHolder = koinInject()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = ripple(),
                    interactionSource = null
                ) { expanded = !expanded }
        ) {
            ListItem(
                modifier = Modifier.padding(4.dp),
                headlineContent = { Text(file.value.values.firstOrNull()?.firstOrNull()?.folderName.orEmpty()) },
                supportingContent = { Text("Chapter Count ${file.value.size}") },
                trailingContent = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        null,
                        modifier = Modifier.rotate(animateFloatAsState(if (expanded) 180f else 0f, label = "").value)
                    )
                }
            )
        }

        if (expanded) {
            file.value.values.forEach { chapter ->
                val c = chapter.firstOrNull()

                var showPopup by remember { mutableStateOf(false) }

                if (showPopup) {
                    val onDismiss = { showPopup = false }

                    AlertDialog(
                        onDismissRequest = onDismiss,
                        title = { Text("Delete ${c?.chapterName.orEmpty()}") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    chapter.fastForEach { onDeleted(it) }
                                    onDismiss()
                                }
                            ) { Text("Yes") }
                        },
                        dismissButton = { TextButton(onClick = onDismiss) { Text("No") } }
                    )
                }

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            //delete
                            showPopup = true
                        }
                        false
                    }
                )

                SwipeToDismissBox(
                    modifier = Modifier.padding(start = 32.dp),
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
                        val navController = LocalNavActions.current
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = ripple(),
                                    interactionSource = null
                                ) {
                                    if (useNewReader) {
                                        chapterHolder.downloadedChapterPaths = sortedChapterPaths(file.value)
                                        ReadViewModel.navigateToMangaReader(
                                            navController,
                                            mangaTitle = c?.folderName,
                                            filePath = c?.chapterFolder,
                                            downloaded = true
                                        )
                                    } else {
                                        /*context.startActivity(
                                            Intent(context, ReadActivity::class.java).apply {
                                                putExtra("downloaded", true)
                                                putExtra("filePath", c?.chapterFolder?.let { f -> File(f) })
                                            }
                                        )*/
                                    }
                                }
                        ) {
                            ListItem(
                                modifier = Modifier.padding(4.dp),
                                headlineContent = { Text(c?.chapterName.orEmpty()) },
                                supportingContent = { Text("Chapter Count ${chapter.size}") },
                                trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                            )
                        }
                    }
                )
            }
        }
    }
}

internal fun sortedChapterPaths(chapters: Map<String, List<DownloadedChapters>>): List<String> =
    chapters.entries
        .sortedByDescending { (_, pageList) ->
            pageList.firstOrNull()?.chapterName?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        }
        .map { (chapterFolder, _) -> chapterFolder }