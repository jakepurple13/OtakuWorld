package com.programmersbox.kmpuiviews.tabs

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.tabs_close_tab
import otakuworld.kmpuiviews.generated.resources.tabs_home
import kotlin.math.roundToInt

private val TabShape = RoundedCornerShape(8.dp)

/** How opaque a sleeping tab draws in the strip. */
private const val SleepingTabAlpha = 0.6f

/** The plain icon shown when covers are off: a book for titles and chapters, a play icon for episodes. */
fun plainTabIcon(key: TabKey?, videoChapters: Boolean): ImageVector? = when (key) {
    is TabKey.Title -> Icons.AutoMirrored.Filled.MenuBook
    is TabKey.Chapter -> if (videoChapters) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook
    else -> null
}

/** How tab icons draw: covers, or plain icons when "Show covers on tabs" is off. */
internal class TabIconStyle(val showCovers: Boolean, val videoChapters: Boolean, val keys: TabKeys)

@Composable
internal fun rememberTabIconStyle(keys: TabKeys): TabIconStyle {
    // Plain collectAsState: the title bar has no lifecycle owner of its own.
    val showCovers by remember { tabShowCoversPref.asFlow() }.collectAsState(true)
    val koin = getKoin()
    // Only AnimeWorld registers it, and its chapters are episodes.
    val videoChapters = remember(koin) { koin.getOrNull<MiniPlayerSupported>()?.supported == true }
    return remember(showCovers, videoChapters, keys) { TabIconStyle(showCovers, videoChapters, keys) }
}

/** A tab's look: icon and label. The strips' chips and the layout-switch ghosts both draw it. */
@Composable
internal fun TabFace(
    tab: TabState,
    label: TabLabel,
    selected: Boolean,
    side: Boolean,
    style: TabIconStyle,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Surface(
        shape = TabShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (side) 10.dp else 6.dp),
            modifier = Modifier.padding(start = 10.dp, end = if (tab.pinned) 10.dp else 2.dp),
        ) {
            Box(iconModifier) { TabIcon(tab, label, style) }
            Text(
                text = if (tab.pinned) stringResource(Res.string.tabs_home) else label.text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (side) Modifier.weight(1f) else Modifier.widthIn(max = 160.dp),
            )
            if (tab.playing.value) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Playing",
                    modifier = Modifier.size(14.dp),
                )
            }
            trailing()
        }
    }
}

/**
 * One tab in a strip: a chip in the top strip, a full-width row in the side column ([side]). Click activates,
 * middle-click closes. [modifier] goes first, so a lazy item's `animateItem()` applies to the whole tab.
 */
@Composable
internal fun TabChip(
    tab: TabState,
    label: TabLabel,
    selected: Boolean,
    side: Boolean,
    style: TabIconStyle,
    motion: TabMotionState,
    onActivate: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A new tab's slot opens (its width on top, its height on the side, or just a fade when motion is reduced).
    val open = remember { Animatable(if (motion.markSeen(tab.id)) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (open.value < 1f) open.animateTo(1f, tween(motion.ms(TabMotion.SlotOpen), easing = motion.easing(TabMotion.SlotOpen)))
    }
    val ring = remember { Animatable(0f) }
    val flashCount = motion.flashes[tab.id]
    LaunchedEffect(flashCount) {
        if (flashCount != null) {
            ring.snapTo(1f)
            ring.animateTo(0f, tween(motion.ms(TabMotion.Flash), easing = motion.easing(TabMotion.Flash)))
        }
    }
    // A sleeping tab shows dimmed.
    val sleepAlpha by animateFloatAsState(
        targetValue = if (tab.sleeping.value) SleepingTabAlpha else 1f,
        animationSpec = motion.fade(TabMotion.Switch),
    )
    val accent = MaterialTheme.colorScheme.primary
    val position = if (side) TabPosition.Side else TabPosition.Top
    TabFace(
        tab = tab,
        label = label,
        selected = selected,
        side = side,
        style = style,
        modifier = modifier
            .onGloballyPositioned { coords ->
                // Unclipped: the side column slides in under a clip, and the glide must land on the real row.
                if (motion.position == position) motion.chipBounds[tab.id] = Rect(coords.positionInWindow(), coords.size.toSize())
            }
            .height(if (side) 36.dp else 30.dp)
            .clip(TabShape)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val f = if (motion.reduced) 1f else open.value
                val w = if (side) placeable.width else (placeable.width * f).roundToInt()
                val h = if (side) (placeable.height * f).roundToInt() else placeable.height
                layout(w, h) { placeable.placeRelative(0, 0) }
            }
            .graphicsLayer { alpha = (if (motion.reduced) open.value else 1f) * motion.chipAlpha(tab.id) * sleepAlpha }
            .drawWithContent {
                drawContent()
                if (ring.value > 0f) {
                    drawRoundRect(
                        color = accent,
                        alpha = ring.value,
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(2.dp.toPx()),
                    )
                }
            }
            .tabChipPointer(tab.pinned, onActivate, onClose),
        // The icon waits for the cover that is flying into it.
        iconModifier = Modifier
            .onGloballyPositioned { if (motion.position == position) motion.slotBounds[tab.id] = it.boundsInWindow() }
            .graphicsLayer { alpha = if (tab.id in motion.flying) 0f else 1f },
        trailing = {
            if (!tab.pinned) {
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, stringResource(Res.string.tabs_close_tab), Modifier.size(14.dp))
                }
            }
        },
    )
    DisposableEffect(tab.id, side) {
        onDispose {
            // The other strip may already show this tab; leave its bounds alone.
            if (motion.position == position) {
                motion.slotBounds.remove(tab.id)
                motion.chipBounds.remove(tab.id)
            }
        }
    }
}

@Composable
private fun TabIcon(tab: TabState, label: TabLabel, style: TabIconStyle) {
    val iconModifier = Modifier.size(16.dp)
    val plain = if (style.showCovers) null else plainTabIcon(style.keys.keyOf(tab.current), style.videoChapters)
    when {
        // The page in this tab is reloading, as a browser shows it in place of the favicon.
        tab.refresh.refreshing -> Box(iconModifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
        }

        tab.pinned -> Icon(Icons.Default.Home, null, iconModifier)
        plain != null -> Icon(plain, null, iconModifier)
        style.showCovers && label.imageUrl != null -> KamelImage(
            resource = { asyncPainterResource(label.imageUrl) },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onFailure = { label.icon?.let { Icon(it, null, iconModifier) } },
            modifier = iconModifier.clip(RoundedCornerShape(3.dp)),
        )

        label.icon != null -> Icon(label.icon, null, iconModifier)
    }
}

/**
 * A tab chip's clicks: middle-click closes on press, primary click activates on release (unless the close button or a
 * tab drag consumed the release).
 *
 * The press is consumed so the title bar around the strip treats it as a click on a control. Nucleus's macOS title
 * bar starts a native window drag on the first move after an unconsumed press, which would swallow the tab drag.
 */
internal fun Modifier.tabChipPointer(pinned: Boolean, onActivate: () -> Unit, onClose: () -> Unit): Modifier =
    pointerInput(pinned) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                when {
                    // Middle-click closes on press, like a browser.
                    event.type == PointerEventType.Press && event.buttons.isTertiaryPressed -> {
                        change.consume()
                        if (!pinned) onClose()
                    }

                    event.type == PointerEventType.Press && event.buttons.isPrimaryPressed -> {
                        change.consume()
                        // Activate on release so the close button can consume its own click.
                        var up = awaitPointerEvent()
                        while (up.type != PointerEventType.Release) up = awaitPointerEvent()
                        if (up.changes.none { it.isConsumed }) {
                            onActivate()
                        }
                    }
                }
            }
        }
    }
