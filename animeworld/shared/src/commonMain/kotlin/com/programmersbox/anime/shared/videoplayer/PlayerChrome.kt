package com.programmersbox.anime.shared.videoplayer

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.BlurKind
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.blurkind.BlurKindState
import com.programmersbox.kmpuiviews.presentation.components.blurkind.rememberBlurKindState
import com.programmersbox.kmpuiviews.presentation.components.blurkind.setBlurKind
import com.programmersbox.kmpuiviews.presentation.components.blurkind.setBlurKindSource
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt

/** What the bars show. */
@Immutable
data class PlayerChromeState(
    val title: String,
    val subtitle: String? = null,
    val positionMs: Long,
    val durationMs: Long,
    val bufferedMs: Long = 0,
    val isPlaying: Boolean,
    val speed: Float = 1f,
)

/** What the bars' controls do. [onSpeed] null hides the speed chip. */
@Stable
class PlayerChromeActions(
    val onPlayPause: () -> Unit,
    val onRewind: () -> Unit,
    val onFastForward: () -> Unit,
    val onSkip: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onSpeed: (() -> Unit)? = null,
    val onSeekingChange: (Boolean) -> Unit = {},
)

/**
 * Blur for the bars. [kind] is null when the player blur setting is off; [edgeHaze] backs
 * [PlayerStyle.Theater]'s progressive edges, which always use Haze whatever the blur kind.
 */
@Stable
class PlayerBlur(val kind: BlurKindState?, val edgeHaze: HazeState) {
    val enabled: Boolean get() = kind?.showBlur == true
}

@Composable
fun rememberPlayerBlur(enabled: Boolean): PlayerBlur {
    val kind = if (enabled) rememberBlurKindState() else null
    val edgeHaze = rememberHazeState()
    return remember(kind, edgeHaze) { PlayerBlur(kind, edgeHaze) }
}

/** Goes on the video so the bars of [style] can blur it. */
fun Modifier.playerBlurSource(blur: PlayerBlur, style: PlayerStyle): Modifier {
    val kind = blur.kind
    return when {
        kind == null || !kind.showBlur -> this
        style == PlayerStyle.Theater -> hazeSource(blur.edgeHaze)
        else -> setBlurKindSource(kind)
    }
}

/**
 * The player's top and bottom bars in the user's [style]. Sits over the video; [visible] animates
 * them in and out with the style's own motion.
 *
 * [topActions] and [bottomActions] hold the platform's extra controls (cast and battery on
 * Android; volume, fullscreen and shortcuts on desktop). [barInteraction] reports hover over the
 * bars so callers can keep them up while the pointer is on them.
 */
@Composable
fun PlayerChrome(
    style: PlayerStyle,
    visible: Boolean,
    state: PlayerChromeState,
    actions: PlayerChromeActions,
    blur: PlayerBlur,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = { BackButton() },
    topActions: @Composable RowScope.() -> Unit = {},
    bottomActions: @Composable RowScope.() -> Unit = {},
    barInteraction: MutableInteractionSource? = null,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(modifier.fillMaxSize()) {
            val bars = BarParts(state, actions, blur, navigationIcon, topActions, bottomActions, barInteraction)
            when (style) {
                PlayerStyle.Classic -> ClassicChrome(visible, bars)
                PlayerStyle.Floating -> FloatingChrome(visible, bars)
                PlayerStyle.Theater -> TheaterChrome(visible, bars)
            }
        }
    }
}

private class BarParts(
    val state: PlayerChromeState,
    val actions: PlayerChromeActions,
    val blur: PlayerBlur,
    val navigationIcon: @Composable () -> Unit,
    val topActions: @Composable RowScope.() -> Unit,
    val bottomActions: @Composable RowScope.() -> Unit,
    val interaction: MutableInteractionSource?,
)

private val BarShape = RoundedCornerShape(0.dp)
private val PillShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.ClassicChrome(visible: Boolean, bars: BarParts) {
    val fallback = MaterialTheme.colorScheme.surface.copy(alpha = .5f)
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter),
    ) {
        TopAppBar(
            title = { TitleBlock(bars.state) },
            navigationIcon = bars.navigationIcon,
            actions = bars.topActions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.bar(bars, BarShape, fallback),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .bar(bars, BarShape, fallback)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeText(bars.state.positionMs, Modifier.width(64.dp))
                SeekBar(bars, Modifier.weight(1f))
                TimeText(bars.state.durationMs, Modifier.width(64.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                RewindButton(bars)
                PlayPauseButton(bars, filled = false)
                ForwardButton(bars)
                TextButton(onClick = bars.actions.onSkip) { Text("+1:30") }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = bars.bottomActions,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.FloatingChrome(visible: Boolean, bars: BarParts) {
    val fallback = MaterialTheme.colorScheme.surface.copy(alpha = .5f)
    val pillTint = MaterialTheme.colorScheme.surface.copy(alpha = .25f)
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it / 3 } + fadeIn() + scaleIn(initialScale = .96f),
        exit = slideOutVertically { -it / 3 } + fadeOut() + scaleOut(targetScale = .96f),
        modifier = Modifier.align(Alignment.TopCenter),
    ) {
        Row(
            // SpaceBetween, not a Spacer: a weight(fill = false) child only takes its own width,
            // which would leave the actions pill short of the end.
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 12.dp)
                    .bar(bars, PillShape, fallback, pillTint)
                    .padding(start = 4.dp, end = 20.dp, top = 4.dp, bottom = 4.dp)
            ) {
                bars.navigationIcon()
                TitleBlock(bars.state, compact = true)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .bar(bars, PillShape, fallback, pillTint)
                    .padding(4.dp),
                content = bars.topActions,
            )
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it / 3 } + fadeIn() + scaleIn(initialScale = .96f),
        exit = slideOutVertically { it / 3 } + fadeOut() + scaleOut(targetScale = .96f),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .bar(bars, PillShape, fallback, pillTint)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SeekBar(bars, Modifier.fillMaxWidth())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PlayPauseButton(bars, filled = true)
                RewindButton(bars)
                ForwardButton(bars)
                Text(
                    "${formatPlayerTime(bars.state.positionMs)} / ${formatPlayerTime(bars.state.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = bars.actions.onSkip) { Text("Skip +1:30") }
                bars.actions.onSpeed?.let { onSpeed ->
                    FilledTonalButton(onClick = onSpeed) { Text(formatSpeed(bars.state.speed)) }
                }
                bars.bottomActions(this)
            }
        }
    }
}

@Composable
private fun BoxScope.TheaterChrome(visible: Boolean, bars: BarParts) {
    val edgeStyle = HazeBlurStyle.Material3()
    val scrim = MaterialTheme.colorScheme.surface.copy(alpha = if (bars.blur.enabled) .35f else .75f)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = Modifier.align(Alignment.TopCenter),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .edge(bars, fromTop = true, scrim = scrim, style = edgeStyle)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 48.dp)
        ) {
            bars.navigationIcon()
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp, top = 2.dp)
            ) {
                Text(
                    bars.state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                bars.state.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = bars.topActions)
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .edge(bars, fromTop = false, scrim = scrim, style = edgeStyle)
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 8.dp)
        ) {
            SeekBar(bars, Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayPauseButton(bars, filled = false)
                RewindButton(bars)
                ForwardButton(bars)
                Text(
                    "${formatPlayerTime(bars.state.positionMs)} / ${formatPlayerTime(bars.state.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = bars.actions.onSkip) { Text("Skip opening") }
                bars.bottomActions(this)
            }
        }
    }
}

/**
 * A bar's surface: the blur kind when blur is on, else the translucent surface Android always used.
 * [hazeTint] goes over a Haze blur, which has no tint of its own, so a floating pill's shape shows.
 */
private fun Modifier.bar(bars: BarParts, shape: RoundedCornerShape, fallback: Color, hazeTint: Color? = null): Modifier {
    val kind = bars.blur.kind
    val surface = if (kind != null && kind.showBlur) {
        if (kind.blurKind == BlurKind.Haze) {
            // Haze draws to its layer bounds, so it needs the clip; the glass kinds draw the shape themselves.
            clip(shape)
                .setBlurKind(kind)
                .then(hazeTint?.let { Modifier.background(it, shape) } ?: Modifier)
        } else {
            setBlurKind(kind, liquidGlassShape = { shape })
        }
    } else {
        background(fallback, shape)
    }
    return surface
        .then(bars.interaction?.let { Modifier.hoverable(it) } ?: Modifier)
        .blockPointerPassThrough()
}

/** Theater's edges: a progressive Haze blur when blur is on, over a scrim that keeps text readable. */
private fun Modifier.edge(bars: BarParts, fromTop: Boolean, scrim: Color, style: HazeBlurStyle): Modifier {
    val blurred = if (bars.blur.enabled) {
        hazeBlur(
            input = HazeInput.Sources(bars.blur.edgeHaze),
            style = style.then {
                progressive(
                    HazeProgressive.verticalGradient(
                        startIntensity = if (fromTop) 1f else 0f,
                        endIntensity = if (fromTop) 0f else 1f,
                    )
                )
            },
            performanceMode = HazePerformanceMode.Performance,
        )
    } else {
        this
    }
    val colors = if (fromTop) listOf(scrim, Color.Transparent) else listOf(Color.Transparent, scrim)
    return blurred
        .background(Brush.verticalGradient(colors))
        .then(bars.interaction?.let { Modifier.hoverable(it) } ?: Modifier)
        .blockPointerPassThrough()
}

/** Like Surface: taps on a bar's background don't fall through to the video's tap handling. */
private fun Modifier.blockPointerPassThrough() = pointerInput(Unit) {}

@Composable
private fun TitleBlock(state: PlayerChromeState, compact: Boolean = false) {
    Column {
        Text(
            state.title,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        state.subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PlayPauseButton(bars: BarParts, filled: Boolean) {
    val icon = if (bars.state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
    val label = if (bars.state.isPlaying) "Pause" else "Play"
    if (filled) {
        FilledIconButton(onClick = bars.actions.onPlayPause) { Icon(icon, label) }
    } else {
        IconButton(onClick = bars.actions.onPlayPause) { Icon(icon, label) }
    }
}

@Composable
private fun RewindButton(bars: BarParts) {
    IconButton(onClick = bars.actions.onRewind) { Icon(Icons.Default.FastRewind, "Rewind") }
}

@Composable
private fun ForwardButton(bars: BarParts) {
    IconButton(onClick = bars.actions.onFastForward) { Icon(Icons.Default.FastForward, "Fast forward") }
}

@Composable
private fun TimeText(ms: Long, modifier: Modifier = Modifier) {
    Text(
        formatPlayerTime(ms),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        modifier = modifier,
    )
}

/** Inset of the slider's track from its bounds, so the buffer bar and hover time line up with it. */
private val TrackInset = 10.dp

/** Seek slider over a bar showing how far the media is buffered, with the time under the pointer. */
@Composable
private fun SeekBar(bars: BarParts, modifier: Modifier = Modifier) {
    val state = bars.state
    var dragging by remember { mutableStateOf<Float?>(null) }
    var pointerX by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(0)
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    pointerX = when (event.type) {
                        PointerEventType.Exit, PointerEventType.Release -> null
                        else -> event.changes.firstOrNull()?.position?.x
                    }
                }
            }
        },
    ) {
        LinearProgressIndicator(
            progress = { if (duration > 0) (state.bufferedMs.toFloat() / duration).coerceIn(0f, 1f) else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TrackInset),
            color = LocalContentColor.current.copy(alpha = 0.45f),
            trackColor = LocalContentColor.current.copy(alpha = 0.15f),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Slider(
            value = dragging ?: state.positionMs.coerceIn(0, duration).toFloat(),
            onValueChange = {
                if (dragging == null) bars.actions.onSeekingChange(true)
                dragging = it
            },
            onValueChangeFinished = {
                dragging?.let { bars.actions.onSeek(it.toLong()) }
                dragging = null
                bars.actions.onSeekingChange(false)
            },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            enabled = duration > 0,
            colors = SliderDefaults.colors(inactiveTrackColor = Color.Transparent),
        )

        val x = pointerX
        if (x != null && duration > 0) {
            val insetPx = TrackInset.value * androidx.compose.ui.platform.LocalDensity.current.density
            val widthPx = constraints.maxWidth.toFloat()
            val fraction = ((x - insetPx) / (widthPx - 2 * insetPx)).coerceIn(0f, 1f)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset((insetPx + fraction * (widthPx - 2 * insetPx)).roundToInt() - 24.dp.roundToPx(), -28.dp.roundToPx()) }
            ) {
                Text(
                    formatPlayerTime((fraction * duration).toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(48.dp)
                        .padding(vertical = 2.dp),
                )
            }
        }
    }
}

fun formatPlayerTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    fun Long.two() = toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.two()}:${seconds.two()}" else "$minutes:${seconds.two()}"
}

fun formatSpeed(speed: Float): String {
    val rounded = (speed * 100).roundToInt() / 100f
    return (if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()) + "×"
}
