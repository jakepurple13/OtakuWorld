package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.tabs_close_tab
import otakuworld.kmpuiviews.generated.resources.tabs_home
import otakuworld.kmpuiviews.generated.resources.tabs_new_tab

private val TabShape = RoundedCornerShape(8.dp)

/** The window's tabs, for the title bar. Click activates, middle-click closes, "+" opens a new tab. */
@Composable
fun TabStrip(
    window: BrowserWindowState,
    controller: TabsController,
    modifier: Modifier = Modifier,
    keys: TabKeys = koinInject(),
) {
    val motion = controller.motion
    // Tabs already open when the strip appears are not "new": only later ones animate in.
    remember(window.id) { window.tabs.forEach { motion.markSeen(it.id) } }
    val closeSpec = tween<IntOffset>(motion.ms(TabMotion.Close), easing = motion.easing(TabMotion.Close))
    val closeFade = tween<Float>(motion.ms(TabMotion.Close), easing = motion.easing(TabMotion.Close))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            items(window.tabs, key = { it.id.v }) { tab ->
                TabChip(
                    tab = tab,
                    label = keys.labelOf(tab.current),
                    selected = tab.id == window.activeId.value,
                    motion = motion,
                    onActivate = { controller.activate(tab.id) },
                    onClose = { controller.close(tab.id) },
                    modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = closeSpec, fadeOutSpec = closeFade),
                )
            }
        }

        // Placeholder new-tab page until the real one exists.
        IconButton(
            onClick = { controller.open(Screen.RecentScreen, TabOpenMode.Foreground, window.activeId.value) },
        ) { Icon(Icons.Default.Add, stringResource(Res.string.tabs_new_tab)) }
    }
}

@Composable
private fun TabChip(
    tab: TabState,
    label: TabLabel,
    selected: Boolean,
    motion: TabMotionState,
    onActivate: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A new tab's slot opens (its width, or just a fade when motion is reduced).
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
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = TabShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .height(30.dp)
            .clip(TabShape)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val w = if (motion.reduced) placeable.width else (placeable.width * open.value).roundToInt()
                layout(w, placeable.height) { placeable.placeRelative(0, 0) }
            }
            .graphicsLayer { alpha = if (motion.reduced) open.value else 1f }
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
            .pointerInput(tab.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        when {
                            // Middle-click closes on press, like a browser.
                            event.type == PointerEventType.Press && event.buttons.isTertiaryPressed -> {
                                change.consume()
                                if (!tab.pinned) onClose()
                            }

                            event.type == PointerEventType.Press && event.buttons.isPrimaryPressed -> {
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
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 10.dp, end = if (tab.pinned) 10.dp else 2.dp),
        ) {
            // The icon waits for the cover that is flying into it.
            Box(
                Modifier
                    .onGloballyPositioned { motion.slotBounds[tab.id] = it.boundsInWindow() }
                    .graphicsLayer { alpha = if (tab.id in motion.flying) 0f else 1f }
            ) { TabIcon(tab, label) }
            DisposableEffect(tab.id) { onDispose { motion.slotBounds.remove(tab.id) } }
            Text(
                text = if (tab.pinned) stringResource(Res.string.tabs_home) else label.text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
            if (!tab.pinned) {
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, stringResource(Res.string.tabs_close_tab), Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun TabIcon(tab: TabState, label: TabLabel) {
    val iconModifier = Modifier.size(16.dp)
    when {
        tab.pinned -> Icon(Icons.Default.Home, null, iconModifier)
        label.imageUrl != null -> KamelImage(
            resource = { asyncPainterResource(label.imageUrl) },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onFailure = { label.icon?.let { Icon(it, null, iconModifier) } },
            modifier = iconModifier.clip(RoundedCornerShape(3.dp)),
        )

        label.icon != null -> Icon(label.icon, null, iconModifier)
    }
}
