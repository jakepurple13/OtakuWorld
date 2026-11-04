package com.programmersbox.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.videos.VideoLibrarySource
import com.programmersbox.anime.shared.videos.libraryFolderLabel
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

/** Wide windows put the switches on the left and the folder list on the right. */
private val TwoPaneMinWidth = 840.dp

/** Settings › Library folders: which folders "Other videos on this computer" is built from. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryFoldersScreen() {
    val settings = koinInject<AnimeDesktopSettings>()
    val source = koinInject<VideoLibrarySource>()
    val scope = rememberCoroutineScope()

    val include by settings.includeOtherFolders.asFlow().collectAsStateWithLifecycle(false)
    val extras by settings.extraLibraryFolders.asFlow().collectAsStateWithLifecycle(emptySet())
    val disabled by settings.disabledLibraryFolders.asFlow().collectAsStateWithLifecycle(emptySet())
    val watch by settings.watchLibraryFolders.asFlow().collectAsStateWithLifecycle(true)
    val scanning by source.scanProgress.collectAsStateWithLifecycle()
    val videos by remember(source) { source.observeVideos() }.collectAsStateWithLifecycle(emptyList())

    val folders = remember(extras) {
        (AnimeDesktopSettings.DEFAULT_LIBRARY_FOLDERS.filter { File(it).isDirectory } + extras).distinct()
    }
    val counts = remember(videos) { videos.groupingBy { it.folder }.eachCount() }

    val picker = rememberDirectoryPickerLauncher { picked ->
        val path = picked?.absolutePath() ?: return@rememberDirectoryPickerLauncher
        scope.launch {
            settings.extraLibraryFolders.set(extras + path)
            settings.disabledLibraryFolders.set(disabled - path)
        }
    }

    val listColors = ListItemDefaults.colors(containerColor = Color.Transparent)

    val includeRow: @Composable () -> Unit = {
        ListItem(
            headlineContent = { Text("Include videos from other folders") },
            supportingContent = {
                Text("Show video files from these folders in Downloads. AnimeWorld only reads them; nothing is uploaded.")
            },
            trailingContent = {
                Switch(
                    checked = include,
                    onCheckedChange = { scope.launch { settings.includeOtherFolders.set(it) } }
                )
            },
            colors = listColors,
        )
    }

    val watchRow: @Composable () -> Unit = {
        ListItem(
            headlineContent = { Text("Watch for new files") },
            supportingContent = { Text("New videos in these folders show up without a rescan.") },
            trailingContent = {
                Switch(
                    checked = watch,
                    onCheckedChange = { scope.launch { settings.watchLibraryFolders.set(it) } }
                )
            },
            colors = listColors,
        )
    }

    val actions: @Composable () -> Unit = {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedButton(onClick = { picker.launch() }) {
                Icon(Icons.Default.Add, null)
                Text("Add folder…", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = { source.rescan() }) {
                Icon(Icons.Default.Refresh, null)
                Text("Rescan now", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }

    val folderRow: @Composable (String, Modifier) -> Unit = { path, modifier ->
        val enabled = path !in disabled
        val label = libraryFolderLabel(path)
        ListItem(
            leadingContent = {
                Checkbox(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        scope.launch {
                            settings.disabledLibraryFolders.set(if (checked) disabled - path else disabled + path)
                        }
                    }
                )
            },
            headlineContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(
                        targetState = scanning != null,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "folderCount"
                    ) { isScanning ->
                        when {
                            !enabled -> Text("Off", style = MaterialTheme.typography.bodySmall)
                            isScanning -> CircularWavyProgressIndicator(modifier = Modifier.size(18.dp))
                            else -> {
                                val count = counts[label] ?: 0
                                Text("$count video${if (count == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (path in extras) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    settings.extraLibraryFolders.set(extras - path)
                                    settings.disabledLibraryFolders.set(disabled - path)
                                }
                            }
                        ) { Icon(Icons.Default.Close, contentDescription = "Remove folder") }
                    } else {
                        // Keeps counts lined up with rows that have a remove button.
                        Box(Modifier.size(48.dp))
                    }
                }
            },
            colors = listColors,
            modifier = modifier
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library folders") },
                navigationIcon = { BackButton() },
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (maxWidth >= TwoPaneMinWidth) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                    ) {
                        includeRow()
                        AnimatedVisibility(
                            visible = include,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column {
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                watchRow()
                                actions()
                            }
                        }
                    }
                    VerticalDivider()
                    AnimatedContent(
                        targetState = include,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "folderPane",
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                    ) { showFolders ->
                        if (showFolders) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(folders, key = { it }) { path -> folderRow(path, Modifier.animateItem()) }
                            }
                        } else {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                Text(
                                    "Turn on \"Include videos from other folders\" to choose folders.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item(key = "include") {
                        includeRow()
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                    item(key = "folders") {
                        AnimatedVisibility(
                            visible = include,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                folders.forEach { path -> folderRow(path, Modifier) }
                                actions()
                                watchRow()
                            }
                        }
                    }
                }
            }
        }
    }
}
