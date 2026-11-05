package com.programmersbox.anime.shared.subtitlegen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.videos.SectionHeader
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import javax.swing.JFileChooser

/** "X MB" below 1 GB, otherwise "X.X GB" (decimal units, matching the model size labels). */
internal fun formatDownloadedBytes(bytes: Long): String {
    val mb = bytes / 1_000_000.0
    return if (mb < 1000) "${mb.toLong()} MB" else "%.1f GB".format(java.util.Locale.US, mb / 1000)
}

@Composable
internal actual fun GeneratedSubtitlesSettings() {
    val store = koinInject<WhisperModelStore>()
    val settings = koinInject<AnimeDesktopSettings>()
    val scope = rememberCoroutineScope()
    val rows by store.rows.collectAsState()
    val downloaded by store.downloadedBytes.collectAsState()
    var addError by remember { mutableStateOf<String?>(null) }

    val installer = koinInject<WhisperCliInstaller>()
    val install by installer.state.collectAsState()
    val isMac = remember { System.getProperty("os.name").startsWith("Mac") }
    val configuredPath by settings.whisperPath.asFlow().collectAsState(initial = "")
    var found by remember { mutableStateOf<File?>(null) }
    var lookedUp by remember { mutableStateOf(false) }
    LaunchedEffect(configuredPath, install is CliInstall.Ready) {
        found = resolveWhisperCli(settings, installer)
        lookedUp = true
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SectionHeader("Generated subtitles")
    Text(
        "The speech model turns an episode's audio into subtitles on this computer. " +
            "Bigger models are more accurate but slower and take more disk space.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    // A Column, not a lazy list: custom rows animate with AnimatedVisibility keyed by path. Removed custom
    // rows are retained until their exit animation finishes.
    val initialPaths = remember { rows.mapNotNull { it.path }.toSet() }
    var retained by remember { mutableStateOf(rows.filter { it.path != null }) }
    LaunchedEffect(rows) {
        val current = rows.filter { it.path != null }
        val kept = retained.map { old -> current.firstOrNull { it.path == old.path } ?: old }
        retained = kept + current.filter { c -> kept.none { it.path == c.path } }
    }
    val currentPaths = rows.mapNotNull { it.path }.toSet()
    rows.filter { it.path == null }.forEach { row ->
        key(row.choice) {
            ModelListItem(
                row = row,
                onSelect = { store.select(row.choice) },
                onDownload = { (row.choice as? WhisperModelChoice.BuiltIn)?.let { store.download(it.model) } },
                onDelete = { (row.choice as? WhisperModelChoice.BuiltIn)?.let { store.delete(it.model) } },
                onRemove = {},
            )
        }
    }
    retained.forEach { row ->
        val path = row.path ?: return@forEach
        key(path) {
            val state = remember { MutableTransitionState(path in initialPaths) }
            state.targetState = path in currentPaths
            if (state.isIdle && !state.currentState && path !in currentPaths) {
                LaunchedEffect(Unit) { retained = retained.filter { it.path != path } }
            }
            AnimatedVisibility(
                visibleState = state,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ModelListItem(
                    row = row,
                    onSelect = { store.select(row.choice) },
                    onDownload = {},
                    onDelete = {},
                    onRemove = { store.removeCustom(path) },
                )
            }
        }
    }

    ListItem(
        headlineContent = { Text("Add model file…") },
        supportingContent = { Text("A whisper.cpp model (.bin), such as a fine-tuned or quantized one") },
        leadingContent = { Icon(Icons.Default.Add, null) },
        modifier = Modifier.clickable {
            addError = null
            // Same as VideoExportScreen: the chooser runs on the Compose/Swing event thread.
            chooseFile("Choose a whisper.cpp model")?.let { file ->
                scope.launch { addError = (store.addCustom(file) as? AddModelResult.Rejected)?.message }
            }
        },
    )
    AnimatedVisibility(visible = addError != null, enter = expandVertically(), exit = shrinkVertically()) {
        // Keep the last message while the row collapses.
        var last by remember { mutableStateOf("") }
        addError?.let { last = it }
        Text(
            last,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    ListItem(
        headlineContent = { Text("whisper-cli location") },
        supportingContent = {
            val file = found
            val badConfigured = configuredPath.isNotBlank() && !File(configuredPath).canExecute()
            // Windows with a prebuilt whisper.cpp for this CPU: AnimeWorld downloads it instead of the user.
            val downloadable = install !is CliInstall.NotNeeded
            val failed = install as? CliInstall.Failed
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when {
                        !lookedUp -> ""
                        badConfigured ->
                            configuredPath + "\n" + "Chosen file can't be run. Choose another, or clear it to search automatically."
                        file == null && failed != null -> failed.message
                        file == null && downloadable ->
                            "Not installed yet. AnimeWorld downloads whisper.cpp (${installer.asset?.sizeLabel}) the first time you generate."
                        file == null && isMac -> "Not found. Install it with brew install whisper-cpp, or choose the file."
                        file == null -> "Not found. Install whisper.cpp or build it from source, then choose whisper-cli."
                        configuredPath.isNotBlank() && file.path == configuredPath -> file.path
                        file == (install as? CliInstall.Ready)?.file -> file.path + " · downloaded by AnimeWorld"
                        else -> file.path + " · found automatically"
                    },
                    fontFamily = if (file != null) FontFamily.Monospace else null,
                    color = if (lookedUp && (badConfigured || (file == null && (!downloadable || failed != null)))) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color.Unspecified
                    },
                )
                AnimatedVisibility(
                    visible = file == null && install is CliInstall.Installing,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    // Keep the last progress visible while the bar collapses.
                    var last by remember { mutableStateOf(0f) }
                    (install as? CliInstall.Installing)?.let { last = it.fraction }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LinearProgressIndicator(progress = { last }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "Downloading whisper.cpp · ${(last * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (configuredPath.isNotBlank()) {
                    TextButton(onClick = { scope.launch { settings.whisperPath.set("") } }) { Text("Use automatic") }
                }
                if (found == null && (install is CliInstall.Missing || install is CliInstall.Failed)) {
                    OutlinedButton(onClick = installer::install) { Text("Download now") }
                }
                OutlinedButton(onClick = {
                    chooseFile("Choose whisper-cli")?.let { file ->
                        scope.launch { settings.whisperPath.set(file.absolutePath) }
                    }
                }) { Text("Choose…") }
            }
        },
    )

    Text(
        "Downloaded models use ${formatDownloadedBytes(downloaded)}. " +
            "Models not downloaded yet download the first time you generate. " +
            "Your own files stay where they are and aren't counted.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ModelListItem(
    row: ModelRow,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onRemove: () -> Unit,
) {
    val builtIn = (row.choice as? WhisperModelChoice.BuiltIn)?.model
    ListItem(
        leadingContent = { RadioButton(selected = row.selected, onClick = null) },
        headlineContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(row.name)
                if (builtIn?.recommended == true) Badge("Recommended")
                if (builtIn?.sameLanguageOnly == true) Badge("Same-language only")
                if (builtIn == null) Badge("Your file")
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (builtIn != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(row.sizeLabel)
                        Meter("Speed", builtIn.speed)
                        Meter("Accuracy", builtIn.accuracy)
                    }
                    if (builtIn.sameLanguageOnly) Text("Fast and accurate, but translates poorly into English.")
                } else {
                    Text(row.path.orEmpty(), fontFamily = FontFamily.Monospace)
                    Text("${row.sizeLabel} · Speed and accuracy depend on the model")
                }
                val download = row.download
                AnimatedVisibility(
                    visible = download is ModelDownload.Downloading,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    // Keep the last progress visible while the bar collapses.
                    var last by remember { mutableStateOf(ModelDownload.Downloading(0f, 0L)) }
                    (download as? ModelDownload.Downloading)?.let { last = it }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LinearProgressIndicator(progress = { last.fraction }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "${last.bytes / 1_000_000} MB of ${row.sizeLabel} · ${(last.fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                (download as? ModelDownload.Failed)?.let {
                    Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        trailingContent = {
            if (builtIn == null) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove ${row.name} from the list")
                }
            } else {
                AnimatedContent(row.download, contentKey = { it::class }) { download ->
                    when (download) {
                        is ModelDownload.Missing, is ModelDownload.Failed ->
                            OutlinedButton(onClick = onDownload) { Text("Download") }

                        is ModelDownload.Downloading -> IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel download")
                        }
                        is ModelDownload.Ready -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = "Downloaded", tint = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete ${row.name} model")
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier.selectable(selected = row.selected, role = Role.RadioButton, onClick = onSelect),
    )
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun Meter(label: String, value: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) { i ->
                Box(
                    Modifier
                        .width(8.dp)
                        .height(6.dp)
                        .background(
                            if (i < value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(2.dp),
                        )
                )
            }
        }
    }
}

private fun chooseFile(title: String): File? {
    val chooser = JFileChooser().apply { dialogTitle = title }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}
