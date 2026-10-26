package com.programmersbox.anime.shared.videoplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import kotlin.math.min
import kotlin.math.roundToInt

private const val CONTROLS_HIDE_DELAY_MS = 3_000L
private const val KEY_SEEK_MS = 10_000L

/** Full-window video with auto-hiding controls; keyboard: Space, ←/→ (10 s), F (fullscreen), Esc. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DesktopPlayerScreen(
    player: DesktopPlayer,
    title: String,
    modifier: Modifier = Modifier,
) {
    val state by player.state.collectAsState()
    state.error?.let { return PlayerErrorPanel(it, modifier) }

    val fullscreen = rememberFullscreen()
    var activity by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(activity, state.playing) {
        controlsVisible = true
        if (state.playing) {
            delay(CONTROLS_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPointerEvent(PointerEventType.Move) { activity++ }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                activity++
                when (event.key) {
                    Key.Spacebar -> player.playPause()
                    Key.DirectionLeft -> player.seekBy(-KEY_SEEK_MS)
                    Key.DirectionRight -> player.seekBy(KEY_SEEK_MS)
                    Key.F -> fullscreen.toggle()
                    Key.Escape -> if (fullscreen.isOn) fullscreen.toggle() else return@onPreviewKeyEvent false
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        VideoFrame(player.frame.collectAsState(), Modifier.fillMaxSize())

        if (state.buffering) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }

        CompositionLocalProvider(LocalContentColor provides Color.White) {
            AnimatedVisibility(
                controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackButton()
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                }
            }

            AnimatedVisibility(
                controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                PlayerControls(
                    player = player,
                    state = state,
                    fullscreen = fullscreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Draws the latest frame letterboxed; reading the frame only in the draw phase keeps frames from recomposing. */
@Composable
private fun VideoFrame(frame: State<ImageBitmap?>, modifier: Modifier) {
    Canvas(modifier) {
        val image = frame.value ?: return@Canvas
        val scale = min(size.width / image.width, size.height / image.height)
        val width = (image.width * scale).roundToInt()
        val height = (image.height * scale).roundToInt()
        drawImage(
            image = image,
            dstOffset = IntOffset(((size.width - width) / 2).roundToInt(), ((size.height - height) / 2).roundToInt()),
            dstSize = IntSize(width, height),
            filterQuality = FilterQuality.Low,
        )
    }
}

@Composable
private fun PlayerControls(
    player: DesktopPlayer,
    state: PlayerState,
    fullscreen: FullscreenState,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(state.positionMs), textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
            SeekBar(state, onSeek = player::seekTo, modifier = Modifier.weight(1f))
            Text(formatTime(state.durationMs), textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { player.seekBy(-30_000) }) { Icon(Icons.Default.Replay30, "Back 30 seconds") }
            IconButton(onClick = player::playPause) {
                Icon(
                    if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (state.playing) "Pause" else "Play",
                )
            }
            IconButton(onClick = { player.seekBy(30_000) }) { Icon(Icons.Default.Forward30, "Forward 30 seconds") }
            TextButton(onClick = { player.seekBy(90_000) }) { Text("+1:30", color = Color.White) }

            Spacer(Modifier.weight(1f))

            VolumeControl(state.volume, onVolumeChange = player::setVolume)
            if (fullscreen.available) {
                IconButton(onClick = fullscreen::toggle) {
                    Icon(
                        if (fullscreen.isOn) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        if (fullscreen.isOn) "Exit fullscreen" else "Fullscreen",
                    )
                }
            }
        }
    }
}

/** Seek slider over a bar showing how far the media is buffered. */
@Composable
private fun SeekBar(state: PlayerState, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(0)
    Box(modifier, contentAlignment = Alignment.Center) {
        LinearProgressIndicator(
            progress = { if (duration > 0) (state.bufferedMs.toFloat() / duration).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            color = Color.White.copy(alpha = 0.45f),
            trackColor = Color.White.copy(alpha = 0.15f),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Slider(
            value = dragging ?: state.positionMs.coerceIn(0, duration).toFloat(),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            enabled = duration > 0,
            colors = SliderDefaults.colors(inactiveTrackColor = Color.Transparent),
        )
    }
}

@Composable
private fun VolumeControl(volume: Int, onVolumeChange: (Int) -> Unit) {
    Icon(
        if (volume == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
        contentDescription = "Volume",
        modifier = Modifier.size(24.dp),
    )
    Slider(
        value = volume.toFloat(),
        onValueChange = { onVolumeChange(it.roundToInt()) },
        valueRange = 0f..100f,
        modifier = Modifier.width(120.dp),
    )
}

@Composable
private fun PlayerErrorPanel(message: String, modifier: Modifier = Modifier) {
    val navigationActions = koinInject<NavigationActions>()
    Column(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Text(message, color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Button(onClick = navigationActions::popBackStack) { Text("Back") }
    }
}

/** Shown when the player can't start at all, e.g. libVLC failed to load. */
@Composable
internal fun PlayerUnavailable(message: String) = PlayerErrorPanel(message)

private class FullscreenState(private val window: ComposeWindow?) {
    private var restorePlacement = WindowPlacement.Floating

    var isOn by mutableStateOf(window?.placement == WindowPlacement.Fullscreen)
        private set

    val available get() = window != null

    fun toggle() {
        val window = window ?: return
        if (isOn) {
            window.placement = restorePlacement
        } else {
            restorePlacement = window.placement
            window.placement = WindowPlacement.Fullscreen
        }
        isOn = !isOn
    }
}

/** Fullscreen of the window hosting the player; left again when the player leaves composition. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun rememberFullscreen(): FullscreenState {
    val window = LocalAwtWindow.current as? ComposeWindow
    val state = remember(window) { FullscreenState(window) }
    DisposableEffect(state) {
        onDispose { if (state.isOn) state.toggle() }
    }
    return state
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
