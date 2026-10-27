package com.programmersbox.anime.shared.videoexport

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.anime.shared.downloads.formatBytes
import com.programmersbox.anime.shared.videos.toDurationText
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JFileChooser

/** Export screen for the downloaded video at [path]. */
@Serializable
data class VideoExportRoute(val path: String) : NavKey

private enum class SubtitleChoice { None, Tracks, BurnIn }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoExportScreen(route: VideoExportRoute) {
    val manager = koinInject<VideoExportManager>()
    val settings = koinInject<AnimeDesktopSettings>()
    val scope = rememberCoroutineScope()
    val source = remember(route.path) { File(route.path) }

    // Bumped after the user picks an ffmpeg so it is looked up again.
    var ffmpegLookup by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var ffmpeg by remember { mutableStateOf<File?>(null) }
    var info by remember { mutableStateOf<MediaInfo?>(null) }
    var canBurn by remember { mutableStateOf(false) }
    val subtitles = remember(source) { manager.subtitlesFor(source) }

    LaunchedEffect(source, ffmpegLookup) {
        loading = true
        ffmpeg = manager.ffmpeg()
        ffmpeg?.let {
            info = manager.probe(it, source)
            canBurn = manager.canBurnSubtitles(it)
        }
        loading = false
    }

    val export = manager.exports.collectAsState().value[source.absolutePath]

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton() },
                title = { Text("Export video") },
            )
        },
        modifier = Modifier.padding(bottom = LocalNavHostPadding.current.calculateBottomPadding())
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 900.dp)
        ) {
            SourceCard(source, info, subtitles.size)
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                ffmpeg == null -> FfmpegMissing(
                    onChoose = {
                        chooseFile()?.let {
                            scope.launch {
                                settings.ffmpegPath.set(it.absolutePath)
                                ffmpegLookup++
                            }
                        }
                    }
                )

                export == null -> ExportForm(
                    source = source,
                    subtitles = subtitles,
                    canBurn = canBurn,
                    manager = manager,
                    onExport = { options, output -> manager.start(ffmpeg!!, source, options, output, info?.durationMs) },
                )

                else -> ExportProgress(
                    export = export,
                    onCancel = { manager.cancel(source.absolutePath) },
                    onClear = { manager.clear(source.absolutePath) },
                )
            }
        }
    }
}

@Composable
private fun SourceCard(source: File, info: MediaInfo?, subtitleCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(source.parentFile?.name.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(source.nameWithoutExtension, style = MaterialTheme.typography.titleMedium)
            val details = listOfNotNull(
                info?.container,
                info?.video,
                info?.audio,
                info?.durationMs?.toDurationText(),
                formatBytes(source.length()),
                if (subtitleCount > 0) "$subtitleCount subtitles" else null,
            )
            Text(
                details.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportForm(
    source: File,
    subtitles: List<ExportSubtitle>,
    canBurn: Boolean,
    manager: VideoExportManager,
    onExport: (ExportOptions, File) -> Unit,
) {
    var format by remember { mutableStateOf(ExportFormat.Mp4) }
    var subtitleChoice by remember { mutableStateOf(if (subtitles.isEmpty()) SubtitleChoice.None else SubtitleChoice.Tracks) }
    var burnIndex by remember { mutableIntStateOf(0) }
    var folder by remember { mutableStateOf(source.absoluteFile.parentFile) }
    val output = remember(format, folder) { manager.outputFor(source, format, folder) }
    val mode = when (subtitleChoice) {
        SubtitleChoice.None -> SubtitleMode.None
        SubtitleChoice.Tracks -> SubtitleMode.AsTracks
        SubtitleChoice.BurnIn -> SubtitleMode.BurnIn(burnIndex)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        OptionGroup("Format", Modifier.weight(1f)) {
            Option(
                selected = format == ExportFormat.Mp4,
                title = "MP4",
                description = "Plays almost everywhere: phones, browsers, Chromecast, TVs.",
                onClick = { format = ExportFormat.Mp4 }
            )
            Option(
                selected = format == ExportFormat.Mkv,
                title = "MKV",
                description = "Keeps subtitle tracks as they are. VLC, mpv, Plex, Jellyfin.",
                onClick = { format = ExportFormat.Mkv }
            )
        }
        OptionGroup("Subtitles", Modifier.weight(1f)) {
            Option(
                selected = subtitleChoice == SubtitleChoice.None,
                title = "None",
                description = "Video and audio only.",
                onClick = { subtitleChoice = SubtitleChoice.None }
            )
            Option(
                selected = subtitleChoice == SubtitleChoice.Tracks,
                title = "Include as tracks",
                description = when {
                    subtitles.isEmpty() -> "This episode has no subtitles."
                    format == ExportFormat.Mp4 -> "All ${subtitles.size} tracks, switchable in the player. Styling is dropped."
                    else -> "All ${subtitles.size} tracks, switchable in the player. No re-encode."
                },
                enabled = subtitles.isNotEmpty(),
                onClick = { subtitleChoice = SubtitleChoice.Tracks }
            )
            Option(
                selected = subtitleChoice == SubtitleChoice.BurnIn,
                title = "Burn in",
                description = when {
                    subtitles.isEmpty() -> "This episode has no subtitles."
                    !canBurn -> "Your ffmpeg was built without libass, so it can't draw subtitles. Install a full build (macOS: brew install ffmpeg-full)."
                    else -> "Drawn into the picture, so any player or cast device shows it. Re-encodes video."
                },
                enabled = subtitles.isNotEmpty() && canBurn,
                onClick = { subtitleChoice = SubtitleChoice.BurnIn }
            )
            if (subtitleChoice == SubtitleChoice.BurnIn) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = subtitles.getOrNull(burnIndex)?.label.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subtitle to burn in") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        subtitles.forEachIndexed { i, sub ->
                            DropdownMenuItem(
                                text = { Text(sub.label) },
                                onClick = {
                                    burnIndex = i
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    OptionGroup("Save to") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                output.absolutePath,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = { chooseFolder(folder)?.let { folder = it } }) { Text("Change…") }
        }
    }

    Text(
        if (mode is SubtitleMode.BurnIn) {
            "Re-encodes video: expect several minutes per episode. Quality is set to visually lossless (CRF 20)."
        } else {
            "Fast: streams are copied, not re-encoded. Takes seconds and keeps full quality."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
        val navController = LocalNavActions.current
        TextButton(onClick = { navController.popBackStack() }) { Text("Cancel") }
        Button(onClick = { onExport(ExportOptions(format, mode), output) }) { Text("Export") }
    }
}

@Composable
private fun ExportProgress(export: VideoExport, onCancel: () -> Unit, onClear: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(export.output.name, style = MaterialTheme.typography.titleMedium)
            when (val state = export.state) {
                is ExportState.Running -> {
                    val fraction = state.fraction
                    if (fraction == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Stat("Progress", fraction?.let { "${(it * 100).toInt()}%" } ?: "…")
                        Stat(
                            "Position",
                            state.positionMs.toDurationText() + (state.durationMs?.let { " / ${it.toDurationText()}" } ?: "")
                        )
                        Stat("Speed", state.speed?.let { "%.1f×".format(it) } ?: "…")
                        val left = state.durationMs?.let { d -> state.speed?.takeIf { it > 0 }?.let { ((d - state.positionMs) / it).toLong() } }
                        Stat("Time left", left?.coerceAtLeast(0)?.toDurationText() ?: "…")
                        Stat("Size so far", formatBytes(state.bytes))
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onCancel) { Text("Cancel export") }
                    }
                }

                is ExportState.Done -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Exported · ${formatBytes(state.bytes)} · ${state.elapsedMs.toDurationText()}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                        val navController = LocalNavActions.current
                        TextButton(onClick = onClear) { Text("Export again") }
                        OutlinedButton(onClick = { showInFolder(state.output) }) { Text("Show in folder") }
                        Button(
                            onClick = {
                                navController.navigate(
                                    VideoScreen(
                                        showPath = state.output.absolutePath,
                                        showName = state.output.nameWithoutExtension,
                                        downloadOrStream = true,
                                        referer = ""
                                    )
                                )
                            }
                        ) { Text("Play") }
                    }
                }

                is ExportState.Failed -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Text("Export failed", color = MaterialTheme.colorScheme.error)
                    }
                    Text(state.message, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(state.log), null) }
                        ) { Text("Copy log") }
                        Button(onClick = onClear) { Text("Try again") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FfmpegMissing(onChoose: () -> Unit) {
    OutlinedCard(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ffmpeg wasn't found", style = MaterialTheme.typography.titleMedium)
            Text("Install it (macOS: brew install ffmpeg) and come back, or choose the ffmpeg program yourself.")
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onChoose) { Text("Choose ffmpeg…") }
            }
        }
    }
}

@Composable
private fun OptionGroup(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    OutlinedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun Option(selected: Boolean, title: String, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.padding(12.dp))
        Column(Modifier.padding(top = 8.dp)) {
            val alpha = if (enabled) 1f else 0.5f
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun chooseFolder(current: File): File? {
    val chooser = JFileChooser(current).apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

private fun chooseFile(): File? {
    val chooser = JFileChooser().apply { dialogTitle = "Choose ffmpeg" }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

private fun showInFolder(file: File) {
    if (!Desktop.isDesktopSupported()) return
    val desktop = Desktop.getDesktop()
    if (desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) desktop.browseFileDirectory(file)
    else file.parentFile?.let(desktop::open)
}
