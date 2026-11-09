package com.programmersbox.anime.shared.videoplayer

import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.WindowPlacement
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.subtitles.SubtitleQuickSheet
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRepository
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsScreen
import com.programmersbox.anime.shared.subtitles.SubtitleTrackItem
import com.programmersbox.datastore.asState
import com.programmersbox.kmpmodels.KmpSubtitle
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.koin.compose.koinInject
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import kotlin.math.min
import kotlin.math.roundToInt

private const val CONTROLS_HIDE_DELAY_MS = 3_000L
private const val CURSOR_HIDE_DELAY_MS = 1_200L
private const val SEEK_SHORT_MS = 10_000L
private const val SEEK_LONG_MS = 30_000L
private const val SKIP_MS = 90_000L
private const val VOLUME_STEP = 5
private const val RATE_STEP = 0.25f
private const val MOVE_SLOP_PX = 4f
private const val FOCUS_ATTEMPTS = 10

/** A blank cursor, so the pointer hides with the bars like in other video players. */
private val HiddenCursor by lazy {
    PointerIcon(
        Toolkit.getDefaultToolkit().createCustomCursor(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), Point(0, 0), "hidden")
    )
}

/**
 * Full-window video under the user's [PlayerStyle] bars. A click shows or hides the bars and moving
 * the mouse shows them (they auto-hide after 3 s while playing), a double-click toggles fullscreen,
 * and the keyboard map is
 * listed in [ShortcutSheet]. [externalSubtitles] are fetched with [referer]; embedded subtitle
 * tracks come from [player].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DesktopPlayerScreen(
    player: DesktopPlayer,
    title: String,
    modifier: Modifier = Modifier,
    externalSubtitles: List<KmpSubtitle> = emptyList(),
    referer: String? = null,
    streamHeaders: Map<String, String> = emptyMap(),
) {
    HideNavBarWhileOnScreen()
    val state by player.state.collectAsState()
    state.error?.let { return PlayerErrorPanel(it, modifier) }

    val storageHolder = koinInject<StorageHolder>()
    // The poster covers the player until playback first advances; later rebuffers show only the ring.
    var seenPlayback by remember(player) { mutableStateOf(false) }
    if (state.positionMs > 0) seenPlayback = true

    val subtitleSettings = koinInject<SubtitleSettingsRepository>()
    val subtitleStyle by subtitleSettings.style.collectAsState(initial = null)
    val cueLoader = remember(streamHeaders) { SubtitleCueLoader(OkHttpClient(), streamHeaders) }
    val subtitles = rememberDesktopSubtitles(player, externalSubtitles, referer, subtitleStyle, cueLoader)
    var subtitleSheetOpen by remember { mutableStateOf(false) }
    var subtitleSettingsOpen by remember { mutableStateOf(false) }
    var shortcutsOpen by remember { mutableStateOf(false) }
    // Last track shown, so C can turn captions back on to it.
    var lastTrack by remember { mutableStateOf(subtitles.selected) }
    LaunchedEffect(subtitles.selected) { subtitles.selected?.let { lastTrack = it } }
    val scope = rememberCoroutineScope()

    val style by videoPlayerStyle.asState()
    val blurOn by videoPlayerBlur.asState()
    val blur = rememberPlayerBlur(blurOn)
    val feedback = remember { PlayerFeedback() }
    val fullscreen = rememberFullscreen()

    val barInteraction = remember { MutableInteractionSource() }
    val barHovered by barInteraction.collectIsHoveredAsState()
    var seeking by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    val overlayOpen = subtitleSheetOpen || subtitleSettingsOpen || shortcutsOpen
    val barsShown = controlsVisible || overlayOpen
    // Bumped by anything that should restart the hide timer.
    var activity by remember { mutableIntStateOf(0) }
    // Pausing brings the bars up; they only auto-hide while playing.
    LaunchedEffect(state.playing) { if (!state.playing) controlsVisible = true }
    LaunchedEffect(barsShown, state.playing, barHovered, seeking, overlayOpen, activity) {
        if (barsShown && state.playing && !barHovered && !seeking && !overlayOpen) {
            delay(CONTROLS_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }
    fun showBars() {
        controlsVisible = true
        activity++
    }

    // The tap handler outlives recompositions, so it reads the current visibility through this.
    val currentBarsShown by rememberUpdatedState(barsShown)

    var pointerMoves by remember { mutableIntStateOf(0) }
    var lastPointer by remember { mutableStateOf<Offset?>(null) }
    var cursorHidden by remember { mutableStateOf(false) }
    LaunchedEffect(pointerMoves, barsShown) {
        cursorHidden = false
        if (!barsShown) {
            delay(CURSOR_HIDE_DELAY_MS)
            cursorHidden = true
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // During the navigation transition the focus target may not be attached yet, so retry for a few frames.
        repeat(FOCUS_ATTEMPTS) {
            withFrameNanos {}
            if (focusRequester.requestFocus()) return@LaunchedEffect
        }
    }

    fun playPause() {
        feedback.flash(pausing = state.playing)
        player.playPause()
    }

    fun seek(deltaMs: Long, label: String = "${kotlin.math.abs(deltaMs) / 1000} s") {
        player.seekBy(deltaMs)
        feedback.seek(forward = deltaMs > 0, label = label)
    }

    fun changeVolume(delta: Int) {
        val volume = (state.volume + delta).coerceIn(0, 100)
        player.setVolume(volume)
        if (state.muted) player.toggleMute()
        feedback.hud(if (volume == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, "$volume%", volume / 100f)
    }

    fun toggleMute() {
        val muting = !state.muted
        player.toggleMute()
        feedback.hud(
            if (muting) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            if (muting) "Muted" else "${state.volume}%",
            if (muting) 0f else state.volume / 100f,
        )
    }

    fun changeRate(rate: Float) {
        val clamped = rate.coerceIn(MIN_RATE, MAX_RATE)
        player.setRate(clamped)
        feedback.hud(Icons.Default.Speed, formatSpeed(clamped), clamped / MAX_RATE)
    }

    fun toggleCaptions() {
        val on = subtitles.selected == null
        subtitles.select(if (on) lastTrack ?: subtitles.tracks.firstOrNull() else null)
        feedback.hud(
            if (on) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionOff,
            if (on && subtitles.tracks.isEmpty()) "No subtitles" else if (on) "Captions on" else "Captions off",
        )
    }

    /** Handles a key press; false lets it through (e.g. to a focused text field). */
    fun onKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        // The subtitle settings dialog and sheet get their own keys; Esc closes the sheet.
        if (subtitleSettingsOpen) return false
        if (subtitleSheetOpen) {
            if (event.key != Key.Escape) return false
            subtitleSheetOpen = false
            return true
        }
        val question = event.utf16CodePoint == '?'.code || (event.key == Key.Slash && event.isShiftPressed)
        if (shortcutsOpen && !question && event.key != Key.Escape) return true
        when {
            question -> shortcutsOpen = !shortcutsOpen
            event.key == Key.Escape -> when {
                shortcutsOpen -> shortcutsOpen = false
                fullscreen.isOn -> fullscreen.toggle()
                else -> return false
            }

            event.key == Key.Spacebar || event.key == Key.K -> playPause().also { showBars() }
            event.key == Key.DirectionLeft -> seek(-SEEK_SHORT_MS).also { showBars() }
            event.key == Key.DirectionRight -> seek(SEEK_SHORT_MS).also { showBars() }
            event.key == Key.J -> seek(-SEEK_LONG_MS).also { showBars() }
            event.key == Key.L -> seek(SEEK_LONG_MS).also { showBars() }
            event.key == Key.S -> seek(SKIP_MS, "1:30").also { showBars() }
            event.key == Key.DirectionUp -> changeVolume(VOLUME_STEP)
            event.key == Key.DirectionDown -> changeVolume(-VOLUME_STEP)
            event.key == Key.M -> toggleMute()
            event.key == Key.F -> fullscreen.toggle()
            event.key == Key.C && event.isShiftPressed -> subtitleSheetOpen = true
            event.key == Key.C -> toggleCaptions()
            event.key == Key.LeftBracket -> changeRate(state.rate - RATE_STEP)
            event.key == Key.RightBracket -> changeRate(state.rate + RATE_STEP)
            event.key == Key.H -> if (barsShown) controlsVisible = false else showBars()
            digitOf(event.key) != null -> {
                if (state.durationMs > 0) player.seekTo(state.durationMs * digitOf(event.key)!! / 10)
                showBars()
            }

            else -> return false
        }
        return true
    }

    val cueLift by animateDpAsState(
        when {
            !barsShown -> 0.dp
            style == PlayerStyle.Floating -> 140.dp
            else -> 112.dp
        },
        label = "cueLift",
    )

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerHoverIcon(if (cursorHidden) HiddenCursor else PointerIcon.Default)
            .onPointerEvent(PointerEventType.Move) { event ->
                // Compose also sends a Move at the same spot when layout changes (e.g. the bars hiding);
                // only a real move should bring the bars back.
                val position = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                val last = lastPointer
                lastPointer = position
                if (last != null && (position - last).getDistance() < MOVE_SLOP_PX) return@onPointerEvent
                pointerMoves++
                showBars()
            }
            // Moves are measured from where the pointer entered or last pressed, too.
            .onPointerEvent(PointerEventType.Enter) { event -> event.changes.firstOrNull()?.let { lastPointer = it.position } }
            // Any click puts the keyboard back on the player, e.g. after a bar button took focus.
            .onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) { event ->
                event.changes.firstOrNull()?.let { lastPointer = it.position }
                focusRequester.requestFocus()
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val dy = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                if (dy != 0f) changeVolume(if (dy < 0) VOLUME_STEP else -VOLUME_STEP)
            }
            .onPreviewKeyEvent(::onKey)
            .focusRequester(focusRequester)
            .focusable()
    ) {
        VideoFrame(
            player.frame.collectAsState(),
            Modifier
                .fillMaxSize()
                .playerBlurSource(blur, style)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { if (currentBarsShown) controlsVisible = false else showBars() },
                        onDoubleTap = { fullscreen.toggle() },
                    )
                }
        )
        subtitleStyle?.let {
            val lift = cueLift
            SubtitleOverlay(
                subtitles.cues,
                state.positionMs,
                it,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = -lift.toPx() }
            )
        }

        PlayerBufferingOverlay(
            visible = state.buffering || !seenPlayback,
            percent = bufferPercent(state.bufferedMs, state.positionMs),
            posterUrl = storageHolder.posterUrl,
            showPoster = !seenPlayback,
        )

        PlayerFeedbackOverlay(feedback)

        PlayerChrome(
            style = style,
            visible = barsShown,
            state = PlayerChromeState(
                title = title,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                bufferedMs = state.bufferedMs,
                isPlaying = state.playing,
                speed = state.rate,
            ),
            actions = PlayerChromeActions(
                onPlayPause = ::playPause,
                onRewind = { seek(-SEEK_SHORT_MS) },
                onFastForward = { seek(SEEK_SHORT_MS) },
                onSkip = { seek(SKIP_MS, "1:30") },
                onSeek = player::seekTo,
                onSpeed = { changeRate(if (state.rate >= MAX_RATE) 0.5f else state.rate + RATE_STEP) },
                onSeekingChange = { seeking = it },
            ),
            blur = blur,
            barInteraction = barInteraction,
            topActions = {
                IconButton(onClick = { subtitleSheetOpen = true }) {
                    Icon(if (subtitles.selected != null) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionOff, "Subtitles")
                }
                IconButton(onClick = { shortcutsOpen = true }) { Icon(Icons.Default.Keyboard, "Keyboard shortcuts") }
            },
            bottomActions = {
                VolumeControl(
                    volume = state.volume,
                    muted = state.muted,
                    onToggleMute = ::toggleMute,
                    onVolumeChange = player::setVolume,
                )
                if (fullscreen.available) {
                    IconButton(onClick = fullscreen::toggle) {
                        Icon(
                            if (fullscreen.isOn) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            if (fullscreen.isOn) "Exit fullscreen" else "Fullscreen",
                        )
                    }
                }
            },
        )

        val subStyle = subtitleStyle
        if (subStyle != null) {
            AnimatedVisibility(
                subtitleSheetOpen,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { it }
            ) {
                SubtitleQuickSheet(
                    tracks = subtitles.tracks.map { SubtitleTrackItem(it.label, it == subtitles.selected) },
                    onSelectTrack = { index -> subtitles.select(index?.let(subtitles.tracks::get)) },
                    style = subStyle,
                    onStyleChange = { transform -> scope.launch { subtitleSettings.update(transform) } },
                    onOpenSettings = {
                        subtitleSheetOpen = false
                        subtitleSettingsOpen = true
                    },
                    onDismiss = { subtitleSheetOpen = false },
                )
            }
        }

        ShortcutSheet(visible = shortcutsOpen, onDismiss = { shortcutsOpen = false })

        // Shown over the player instead of navigating: leaving this entry would close the player and lose the position.
        if (subtitleSettingsOpen) {
            DisposableEffect(player) {
                val resume = player.pauseForOverlay()
                onDispose { resume() }
            }
            Dialog(
                onDismissRequest = { subtitleSettingsOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.9f),
                ) {
                    SubtitleSettingsScreen(onBack = { subtitleSettingsOpen = false })
                }
            }
        }
    }
}

private fun digitOf(key: Key): Int? = when (key) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}

@Composable
private fun RowScope.VolumeControl(volume: Int, muted: Boolean, onToggleMute: () -> Unit, onVolumeChange: (Int) -> Unit) {
    IconButton(onClick = onToggleMute) {
        Icon(
            if (muted || volume == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (muted) "Unmute" else "Mute",
        )
    }
    Slider(
        value = if (muted) 0f else volume.toFloat(),
        onValueChange = { onVolumeChange(it.roundToInt()) },
        valueRange = 0f..100f,
        modifier = Modifier.width(100.dp),
    )
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
