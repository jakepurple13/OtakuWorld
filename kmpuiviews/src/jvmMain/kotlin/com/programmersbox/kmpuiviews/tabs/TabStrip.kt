package com.programmersbox.kmpuiviews.tabs

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
                    onActivate = { controller.activate(tab.id) },
                    onClose = { controller.close(tab.id) },
                    modifier = Modifier.animateItem(),
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
    onActivate: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = TabShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .height(30.dp)
            .clip(TabShape)
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
            TabIcon(tab, label)
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
