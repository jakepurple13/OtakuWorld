package com.programmersbox.anime.shared.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.anime.shared.subtitles.ColorSwatches
import com.programmersbox.anime.shared.subtitles.EdgeSegments
import com.programmersbox.anime.shared.subtitles.StepSlider
import com.programmersbox.anime.shared.subtitles.SubtitlePreview
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRepository
import com.programmersbox.anime.shared.subtitles.SubtitleStyle
import com.programmersbox.anime.shared.subtitles.coerced
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import org.koin.compose.koinInject

/** Full-screen remote for the active cast session. Pops itself once a connected session ends. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastRemoteScreen() {
    val controller = koinInject<CastController>()
    val state by controller.state.collectAsStateWithLifecycle()
    val navActions = LocalNavActions.current
    var showStyle by remember { mutableStateOf(false) }

    // A cold start from the notification gets a grace period while the Cast SDK resumes the session;
    // after that, or once a connected session ends, the screen closes itself.
    LaunchedEffect(Unit) {
        val connections = snapshotFlow { state.connection }
        val connected = withTimeoutOrNull(CONNECT_GRACE_MS) { connections.first { it == CastConnection.Connected } }
        if (connected != null) connections.first { it != CastConnection.Connected }
        navActions.remove(CastRemoteRoute)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = navActions::popBackStack) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize")
                    }
                },
                title = {
                    Column {
                        Text(
                            "Casting to",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            state.deviceName.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    Icon(
                        Icons.Default.CastConnected,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        ) {
            NowPlayingArt(state)
            Column {
                state.showTitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    state.episodeName.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SeekBar(state.positionMs, state.durationMs, onSeek = controller::seekTo)
            TransportRow(
                playing = state.playing,
                onTogglePlay = controller::togglePlay,
                onJump = controller::seekBy,
            )
            SubtitleCard(
                tracks = state.tracks,
                activeTrackId = state.activeTrackId,
                onSelect = controller::selectTrack,
                onStyle = { showStyle = true },
                modifier = Modifier.weight(1f, fill = false),
            )
            VolumeRow(state.volume, onVolume = controller::setVolume)
            OutlinedButton(onClick = controller::stop, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Stop casting")
            }
        }
    }

    if (showStyle) CastSubtitleStyleSheet(onDismiss = { showStyle = false })
}

@Composable
internal fun CastPoster(url: String?, modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.background(fill)) {
        if (url != null) {
            ImageLoaderChoice(
                imageUrl = url,
                name = "",
                placeHolder = { remember(fill) { ColorPainter(fill) } },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun NowPlayingArt(state: CastUiState) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        CastPoster(state.posterUrl, Modifier.fillMaxSize())
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("On TV", style = MaterialTheme.typography.labelMedium)
            }
        }
        state.currentCue?.let { cue ->
            // Stands in for the caption drawn on the video, so it keeps fixed colors like SubtitlePreview.
            Text(
                cue,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    // Local value while dragging; the seek is sent once on release.
    var dragging by remember { mutableStateOf<Float?>(null) }
    // Seek target held after release until the receiver reports a nearby position, so the thumb doesn't snap back.
    var pending by remember { mutableStateOf<Long?>(null) }
    val currentPosition by rememberUpdatedState(positionMs)
    LaunchedEffect(pending) {
        val target = pending ?: return@LaunchedEffect
        withTimeoutOrNull(SEEK_SETTLE_TIMEOUT_MS) {
            snapshotFlow { currentPosition }.first { abs(it - target) <= SEEK_SETTLE_TOLERANCE_MS }
        }
        pending = null
    }
    val max = durationMs.coerceAtLeast(1).toFloat()
    val shown = (dragging ?: pending?.toFloat() ?: positionMs.toFloat()).coerceIn(0f, max)
    Column {
        Slider(
            value = shown,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let {
                    onSeek(it.toLong())
                    pending = it.toLong()
                }
                dragging = null
            },
            valueRange = 0f..max,
            enabled = durationMs > 0,
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                formatCastTime(shown.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "-" + formatCastTime((durationMs - shown.toLong()).coerceAtLeast(0)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransportRow(playing: Boolean, onTogglePlay: () -> Unit, onJump: (Long) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        JumpButton(Icons.Default.FastRewind, "15", "Back 15 seconds") { onJump(CastJumps.BACK_15) }
        JumpButton(Icons.Default.Replay10, null, "Back 10 seconds") { onJump(CastJumps.BACK_10) }
        FilledIconButton(onClick = onTogglePlay, modifier = Modifier.size(72.dp)) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                modifier = Modifier.size(36.dp),
            )
        }
        JumpButton(Icons.AutoMirrored.Filled.Forward, "85", "Skip 85 seconds") { onJump(CastJumps.OPENING) }
        JumpButton(Icons.Default.FastForward, "15", "Forward 15 seconds") { onJump(CastJumps.FORWARD_15) }
    }
}

@Composable
private fun JumpButton(icon: ImageVector, label: String?, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = description)
            if (label != null) Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SubtitleCard(
    tracks: List<CastTrack>,
    activeTrackId: Long?,
    onSelect: (Long?) -> Unit,
    onStyle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            ) {
                Icon(Icons.Default.ClosedCaption, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Subtitles", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onStyle) {
                    Icon(Icons.Default.FormatSize, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Style")
                }
            }
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .selectableGroup()
            ) {
                TrackRow("Off", hint = null, selected = activeTrackId == null) { onSelect(null) }
                tracks.forEach { track ->
                    TrackRow(track.label, track.origin, selected = track.id == activeTrackId) { onSelect(track.id) }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(label: String, hint: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Box(Modifier.size(20.dp)) {
            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        hint?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VolumeRow(volume: Float, onVolume: (Float) -> Unit) {
    // Local value while dragging so receiver echoes don't fight the thumb.
    var dragging by remember { mutableStateOf<Float?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null)
        Slider(
            value = dragging ?: volume,
            onValueChange = {
                dragging = it
                onVolume(it)
            },
            onValueChangeFinished = { dragging = null },
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastSubtitleStyleSheet(onDismiss: () -> Unit) {
    val repository = koinInject<SubtitleSettingsRepository>()
    val style by repository.style.collectAsStateWithLifecycle(SubtitleStyle())
    val scope = rememberCoroutineScope()
    // In-progress slider value, shown until the committed change comes back from the store.
    var draft by remember { mutableStateOf<SubtitleStyle?>(null) }
    LaunchedEffect(style) { draft = null }
    val shown = draft ?: style
    val onChange: ((SubtitleStyle) -> SubtitleStyle) -> Unit = { transform -> scope.launch { repository.update(transform) } }
    val onDraft: ((SubtitleStyle) -> SubtitleStyle) -> Unit = { transform -> draft = transform(draft ?: style).coerced() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Text("Subtitle style on TV", style = MaterialTheme.typography.titleLarge)
            Text(
                "Same as your Subtitle settings. Changes apply on the TV right away.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StepSlider(
                label = "Size",
                valueLabel = "${shown.sizePercent}%",
                value = shown.sizePercent,
                range = 50..200,
                step = 10,
                onDraft = { v -> onDraft { it.copy(sizePercent = v) } },
                onCommit = { v -> onChange { it.copy(sizePercent = v) } },
            )
            ColorSwatches(selected = shown.textColor, onSelect = { c -> onChange { it.copy(textColor = c) } })
            EdgeSegments(selected = shown.edge, onSelect = { e -> onChange { it.copy(edge = e) } })
            StepSlider(
                label = "Background",
                valueLabel = if (shown.backgroundPercent == 0) "Off" else "${shown.backgroundPercent}%",
                value = shown.backgroundPercent,
                range = 0..100,
                step = 5,
                onDraft = { v -> onDraft { it.copy(backgroundPercent = v) } },
                onCommit = { v -> onChange { it.copy(backgroundPercent = v) } },
            )
            SubtitlePreview(shown)
        }
    }
}

private const val CONNECT_GRACE_MS = 5_000L
private const val SEEK_SETTLE_TIMEOUT_MS = 2_000L
private const val SEEK_SETTLE_TOLERANCE_MS = 1_500L

internal fun formatCastTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
