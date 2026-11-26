package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.tabs_new_tab

/** Width of the side tab column. */
val SideTabsWidth = 200.dp

private fun TabsController.openNewTab(window: BrowserWindowState) {
    open(NewTabRoute, TabOpenMode.Foreground, window.activeId.value)
}

/** Records this strip's bounds as [window]'s, for drops. */
private fun Modifier.stripOf(window: BrowserWindowState, controller: TabsController): Modifier =
    onGloballyPositioned { controller.ui.stripBounds[window.id] = it.boundsInWindow() }

/** A strip tab that can be dragged; it dims while it is the one being dragged. */
@Composable
private fun Modifier.draggedFrom(tab: TabState, window: BrowserWindowState, controller: TabsController): Modifier =
    graphicsLayer { alpha = if (controller.ui.drag?.tab == tab.id) DraggedTabAlpha else 1f }
        .tabDragSource(tab, window, controller)

private const val DraggedTabAlpha = 0.5f

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
    val style = rememberTabIconStyle(keys)
    val closeSpec = tween<IntOffset>(motion.ms(TabMotion.Close), easing = motion.easing(TabMotion.Close))
    val closeFade = tween<Float>(motion.ms(TabMotion.Close), easing = motion.easing(TabMotion.Close))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.stripOf(window, controller)) {
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
                    side = false,
                    style = style,
                    motion = motion,
                    onActivate = { controller.activate(tab.id) },
                    onClose = { controller.close(tab.id) },
                    modifier = Modifier
                        .animateItem(fadeInSpec = null, placementSpec = closeSpec, fadeOutSpec = closeFade)
                        .draggedFrom(tab, window, controller),
                )
            }
        }

        IconButton(onClick = { controller.openNewTab(window) }) {
            Icon(Icons.Default.Add, stringResource(Res.string.tabs_new_tab))
        }
    }
}

/** The window's tabs as full-width rows, for the side column, with "+ New tab" at the bottom. */
@Composable
fun SideTabColumn(
    window: BrowserWindowState,
    controller: TabsController,
    modifier: Modifier = Modifier,
    keys: TabKeys = koinInject(),
) {
    val motion = controller.motion
    remember(window.id) { window.tabs.forEach { motion.markSeen(it.id) } }
    val style = rememberTabIconStyle(keys)
    val closeSpec = tween<IntOffset>(motion.ms(TabMotion.Close), easing = motion.easing(TabMotion.Close))
    val closeFade = tween<Float>(motion.ms(TabMotion.Close), easing = motion.easing(TabMotion.Close))
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.stripOf(window, controller)) {
        Column(Modifier.fillMaxSize().padding(6.dp)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(window.tabs, key = { it.id.v }) { tab ->
                    TabChip(
                        tab = tab,
                        label = keys.labelOf(tab.current),
                        selected = tab.id == window.activeId.value,
                        side = true,
                        style = style,
                        motion = motion,
                        onActivate = { controller.activate(tab.id) },
                        onClose = { controller.close(tab.id) },
                        modifier = Modifier
                            .animateItem(fadeInSpec = null, placementSpec = closeSpec, fadeOutSpec = closeFade)
                            .draggedFrom(tab, window, controller)
                            .fillMaxWidth(),
                    )
                }
            }

            TextButton(
                onClick = { controller.openNewTab(window) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, null)
                Text(
                    stringResource(Res.string.tabs_new_tab),
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
            }
        }
    }
}
