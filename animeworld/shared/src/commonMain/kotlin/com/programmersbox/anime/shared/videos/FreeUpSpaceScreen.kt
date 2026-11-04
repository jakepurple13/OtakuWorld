package com.programmersbox.anime.shared.videos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.programmersbox.anime.shared.downloads.formatBytes
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data object FreeUpSpaceRoute : NavKey

/** Watched this far through counts as "watched to the end". */
private const val WATCHED_FRACTION = 0.9f

/** Wide windows put the meter and Delete button in a pinned side pane. */
private val TwoPaneMinWidth = 840.dp

/** Stagger between rows when a whole group is selected at once. */
private const val GROUP_SELECT_STAGGER_MS = 25L

internal enum class CleanupGroup { Watched, Other, Rest }

internal fun SharedVideoContent.cleanupGroup(): CleanupGroup = when {
    !isDownload -> CleanupGroup.Other
    duration > 0 && lastPlayedPositionMs >= duration * WATCHED_FRACTION -> CleanupGroup.Watched
    else -> CleanupGroup.Rest
}

private fun CleanupGroup.title(): String = when (this) {
    CleanupGroup.Watched -> "Watched to the end"
    CleanupGroup.Other -> "Other videos on this $libraryDeviceName"
    CleanupGroup.Rest -> "Everything else"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeUpSpaceScreen() {
    val source = koinInject<VideoLibrarySource>()
    val items by remember(source) { source.observeVideos() }.collectAsStateWithLifecycle(emptyList())
    val deleteVideos = rememberVideoDeleter()
    val scope = rememberCoroutineScope()

    var selectedPaths by remember { mutableStateOf(emptySet<String>()) }
    // Drop selections whose videos disappeared (deleted elsewhere, folder switched off).
    val selected = remember(items, selectedPaths) { items.filter { it.path in selectedPaths } }
    var showConfirm by remember { mutableStateOf(false) }

    fun toggle(item: SharedVideoContent) {
        selectedPaths = if (item.path in selectedPaths) selectedPaths - item.path else selectedPaths + item.path
    }

    fun toggleGroup(group: List<SharedVideoContent>) {
        val all = group.all { it.path in selectedPaths }
        scope.launch {
            group.forEach {
                selectedPaths = if (all) selectedPaths - it.path else selectedPaths + it.path
                delay(GROUP_SELECT_STAGGER_MS)
            }
        }
    }

    if (showConfirm) {
        DeleteConfirmDialog(
            selected = selected,
            onDismiss = { showConfirm = false },
            onConfirm = {
                showConfirm = false
                deleteVideos(selected)
                selectedPaths = emptySet()
            }
        )
    }

    // Full-screen: the app's nav bar would cover the Delete bar.
    HideNavBarWhileOnScreen()

    BoxWithConstraints {
        val twoPane = maxWidth >= TwoPaneMinWidth
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { BackButton() },
                    title = { Text("Free up space") },
                )
            },
            bottomBar = {
                if (!twoPane) {
                    Surface(tonalElevation = 3.dp) {
                        CleanupBar(
                            selected = selected,
                            onDelete = { showConfirm = true },
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        ) { padding ->
            if (twoPane) {
                Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .width(300.dp)
                            .padding(16.dp)
                    ) {
                        StorageMeter(items = items, selected = selected)
                        CleanupBar(selected = selected, onDelete = { showConfirm = true }, stacked = true)
                    }
                    VerticalDivider()
                    CleanupList(
                        items = items,
                        selectedPaths = selectedPaths,
                        showMeter = false,
                        groupByFolder = true,
                        onToggle = ::toggle,
                        onToggleGroup = ::toggleGroup,
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                CleanupList(
                    items = items,
                    selectedPaths = selectedPaths,
                    showMeter = true,
                    groupByFolder = false,
                    onToggle = ::toggle,
                    onToggleGroup = ::toggleGroup,
                    contentPadding = padding,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CleanupList(
    items: List<SharedVideoContent>,
    selectedPaths: Set<String>,
    showMeter: Boolean,
    groupByFolder: Boolean,
    onToggle: (SharedVideoContent) -> Unit,
    onToggleGroup: (List<SharedVideoContent>) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val groups = remember(items) {
        CleanupGroup.entries.associateWith { group ->
            items.filter { it.cleanupGroup() == group }.sortedByDescending { it.sizeBytes }
        }
    }
    LazyColumn(contentPadding = contentPadding, modifier = modifier) {
        if (showMeter) {
            item(key = "meter") {
                StorageMeter(
                    items = items,
                    selected = items.filter { it.path in selectedPaths },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (items.isEmpty()) {
            item(key = "empty") { CleanupEmptyState(Modifier.animateItem()) }
        }

        groups.forEach { (group, videos) ->
            if (videos.isEmpty()) return@forEach
            item(key = "group_${group.name}") {
                val allSelected = videos.all { it.path in selectedPaths }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                ) {
                    SectionHeader(
                        "${group.title()} · ${formatBytes(videos.sumOf { it.sizeBytes })}",
                        Modifier.weight(1f)
                    )
                    TextButton(onClick = { onToggleGroup(videos) }) {
                        Text(if (allSelected) "Clear" else "Select all")
                    }
                }
            }
            val folders = if (groupByFolder && group == CleanupGroup.Other) videos.groupBy { it.folder } else mapOf(null to videos)
            folders.forEach { (folder, folderVideos) ->
                if (folder != null) {
                    item(key = "folder_${group.name}_$folder") {
                        Text(
                            folder,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .animateItem()
                                .padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
                        )
                    }
                }
                items(folderVideos, key = { "row_${it.path}" }) { item ->
                    CleanupRow(
                        item = item,
                        selected = item.path in selectedPaths,
                        onToggle = { onToggle(item) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanupRow(
    item: SharedVideoContent,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "cleanupRowBackground"
    )
    VideoRowContent(
        item = item,
        selected = selected,
        leading = { Checkbox(checked = selected, onCheckedChange = { onToggle() }) },
        modifier = modifier
            .background(background)
            .clickable(onClick = onToggle)
    )
}

/** Share of downloads vs other videos, with the selected share in red so you see what Delete frees. */
@Composable
private fun StorageMeter(
    items: List<SharedVideoContent>,
    selected: List<SharedVideoContent>,
    modifier: Modifier = Modifier,
) {
    val total = items.sumOf { it.sizeBytes }.coerceAtLeast(1L).toFloat()
    val selectedPaths = selected.map { it.path }.toSet()
    val downloads = items.filter { it.isDownload && it.path !in selectedPaths }.sumOf { it.sizeBytes } / total
    val others = items.filter { !it.isDownload && it.path !in selectedPaths }.sumOf { it.sizeBytes } / total
    val chosen = selected.sumOf { it.sizeBytes } / total

    val spec = tween<Float>(500)
    val downloadsWeight by animateFloatAsState(downloads, spec, label = "downloadsWeight")
    val othersWeight by animateFloatAsState(others, spec, label = "othersWeight")
    val selectedWeight by animateFloatAsState(chosen, spec, label = "selectedWeight")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(formatBytes(items.sumOf { it.sizeBytes }), style = MaterialTheme.typography.headlineSmall)
            Text(
                "of videos on this $libraryDeviceName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            MeterSegment(downloadsWeight, MaterialTheme.colorScheme.primary)
            MeterSegment(othersWeight, MaterialTheme.colorScheme.outline)
            MeterSegment(selectedWeight, MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MeterLegend("Downloads", MaterialTheme.colorScheme.primary)
            MeterLegend("Other videos", MaterialTheme.colorScheme.outline)
            MeterLegend("Selected", MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.MeterSegment(weight: Float, color: Color) {
    // Row weights must be positive, so empty segments are skipped.
    if (weight > 0.001f) {
        Box(
            Modifier
                .weight(weight)
                .fillMaxHeight()
                .background(color)
        )
    }
}

@Composable
private fun MeterLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Selected count and size with the Delete button; [stacked] puts the button under the text (side pane). */
@Composable
private fun CleanupBar(
    selected: List<SharedVideoContent>,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    val summary: @Composable () -> Unit = {
        Column {
            Row {
                AnimatedContent(
                    targetState = selected.size,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                        } else {
                            slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                        }
                    },
                    label = "selectedCount"
                ) { Text("$it", style = MaterialTheme.typography.titleMedium) }
                Text(" selected", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                if (selected.isEmpty()) "Nothing selected" else "Frees ${formatBytes(selected.sumOf { it.sizeBytes })}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    val button: @Composable (Modifier) -> Unit = { buttonModifier ->
        Button(
            onClick = onDelete,
            enabled = selected.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = buttonModifier
        ) {
            Icon(Icons.Default.Delete, null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Text("Delete", modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
        }
    }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
            summary()
            button(Modifier.fillMaxWidth())
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) { summary() }
            button(Modifier)
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    selected: List<SharedVideoContent>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val (downloads, others) = selected.partition { it.isDownload }
    val plural = if (selected.size == 1) "" else "s"
    val text = if (otherVideosGoToTrash) {
        listOfNotNull(
            downloads.takeIf { it.isNotEmpty() }?.let {
                "${it.size} download${if (it.size == 1) " is" else "s are"} deleted (${formatBytes(it.sumOf { v -> v.sizeBytes })}), with subtitles."
            },
            others.takeIf { it.isNotEmpty() }?.let {
                "${it.size} other file${if (it.size == 1) " moves" else "s move"} to the Trash (${formatBytes(it.sumOf { v -> v.sizeBytes })})."
            },
        ).joinToString("\n")
    } else {
        listOfNotNull(
            "Frees ${formatBytes(selected.sumOf { it.sizeBytes })}. Subtitle files for downloaded episodes are deleted too.",
            otherVideosDeleteNote.takeIf { others.isNotEmpty() },
        ).joinToString(" ")
    }
    val confirmLabel = when {
        !otherVideosGoToTrash || others.isEmpty() -> "Delete"
        downloads.isEmpty() -> "Move to Trash"
        else -> "Delete and trash"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${selected.size} video$plural?") },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CleanupEmptyState(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp)
    ) {
        Icon(
            Icons.Default.CleaningServices,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp)
        )
        Text("No videos on this $libraryDeviceName", style = MaterialTheme.typography.titleMedium)
        Text(
            "Downloads you save will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
