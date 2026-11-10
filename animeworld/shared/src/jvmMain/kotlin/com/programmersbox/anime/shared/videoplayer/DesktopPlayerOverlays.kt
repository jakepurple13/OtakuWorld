package com.programmersbox.anime.shared.videoplayer

import com.programmersbox.kmpuiviews.presentation.shortcuts.ShortcutEntry
import com.programmersbox.kmpuiviews.presentation.shortcuts.ShortcutSection
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** One-shot feedback for keyboard and mouse actions; each pulse gets a new id so repeats replay. */
@Stable
internal class PlayerFeedback {
    var seek by mutableStateOf<SeekPulse?>(null)
        private set
    var flash by mutableStateOf<FlashPulse?>(null)
        private set
    var hud by mutableStateOf<HudPulse?>(null)
        private set
    private var next = 0

    fun seek(forward: Boolean, label: String) {
        seek = SeekPulse(++next, forward, label)
    }

    /** [pausing] shows the pause icon, else play, like Android's quick-seek play/pause. */
    fun flash(pausing: Boolean) {
        flash = FlashPulse(++next, pausing)
    }

    fun hud(icon: ImageVector, text: String, fraction: Float? = null) {
        hud = HudPulse(++next, icon, text, fraction)
    }
}

internal data class SeekPulse(val id: Int, val forward: Boolean, val label: String)
internal data class FlashPulse(val id: Int, val pausing: Boolean)
internal data class HudPulse(val id: Int, val icon: ImageVector, val text: String, val fraction: Float?)

/** Seek ripples on the left/right third, the centre play/pause flash, and the volume/speed pill. */
@Composable
internal fun BoxScope.PlayerFeedbackOverlay(feedback: PlayerFeedback) {
    feedback.seek?.let { SeekRipple(it) }
    feedback.flash?.let { CenterFlash(it, Modifier.align(Alignment.Center)) }
    PlayerHud(feedback.hud, Modifier.align(Alignment.TopCenter).padding(top = 96.dp))
}

@Composable
private fun BoxScope.SeekRipple(pulse: SeekPulse) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(pulse.id) {
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(100))
        delay(350)
        alpha.animateTo(0f, tween(200))
    }
    val shape = if (pulse.forward) {
        RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
    } else {
        RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .align(if (pulse.forward) Alignment.CenterEnd else Alignment.CenterStart)
            .fillMaxHeight()
            .fillMaxWidth(0.32f)
            .alpha(alpha.value)
            .background(Color.White.copy(alpha = 0.12f), shape)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (pulse.forward) Icons.Default.FastForward else Icons.Default.FastRewind, null, tint = Color.White)
            Text(pulse.label, color = Color.White, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun CenterFlash(pulse: FlashPulse, modifier: Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pulse.id) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(500))
    }
    val p = progress.value
    // 0..0.25 grows in, then fades out while it keeps growing.
    val alpha = if (p < .25f) p / .25f else 1f - (p - .25f) / .75f
    val scale = if (p < .25f) .7f + .3f * (p / .25f) else 1f + .25f * ((p - .25f) / .75f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(72.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
    ) {
        Icon(
            if (pulse.pausing) Icons.Default.Pause else Icons.Default.PlayArrow,
            null,
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun PlayerHud(pulse: HudPulse?, modifier: Modifier) {
    var shown by remember { mutableStateOf<HudPulse?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(pulse?.id) {
        if (pulse == null) return@LaunchedEffect
        shown = pulse
        visible = true
        delay(900)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier,
    ) {
        val hud = shown ?: return@AnimatedVisibility
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(hud.icon, null, tint = MaterialTheme.colorScheme.primary)
                hud.fraction?.let {
                    LinearProgressIndicator(progress = { it }, modifier = Modifier.width(110.dp), drawStopIndicator = {})
                }
                Text(hud.text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

internal val PlayerShortcuts = listOf(
    ShortcutEntry(listOf("Space", "K"), "Play / pause"),
    ShortcutEntry(listOf("←", "→"), "Back / forward 10 s"),
    ShortcutEntry(listOf("J", "L"), "Back / forward 30 s"),
    ShortcutEntry(listOf("S"), "Skip +1:30"),
    ShortcutEntry(listOf("↑", "↓", "wheel"), "Volume"),
    ShortcutEntry(listOf("M"), "Mute"),
    ShortcutEntry(listOf("F", "double-click"), "Fullscreen"),
    ShortcutEntry(listOf("C"), "Captions on / off"),
    ShortcutEntry(listOf("Shift", "C"), "Subtitle sheet", separator = " + "),
    ShortcutEntry(listOf("[", "]"), "Speed"),
    ShortcutEntry(listOf("0–9"), "Jump to 0–90%"),
    ShortcutEntry(listOf("H", "click"), "Show / hide bars"),
    ShortcutEntry(listOf("?"), "This sheet", listLabel = "Player shortcuts sheet"),
    ShortcutEntry(listOf("Esc"), "Close / exit fullscreen"),
)

fun playerShortcutSection() = ShortcutSection("Video player", PlayerShortcuts)

/** The `?` sheet listing every shortcut; clicking outside the card closes it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShortcutSheet(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(0.9f)
                    .animateEnterExit(enter = scaleIn(initialScale = .95f), exit = scaleOut(targetScale = .95f))
                    .clickable(interactionSource = null, indication = null) {}
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Keyboard shortcuts", style = MaterialTheme.typography.titleLarge)
                    FlowRow(
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PlayerShortcuts.forEach { entry ->
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    entry.keysText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(120.dp)
                                )
                                Text(entry.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Text("Press ? or Esc to close", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
