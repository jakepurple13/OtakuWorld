package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.updateCheck
import com.programmersbox.kmpuiviews.utils.AppConfig
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.custom_lists_title
import otakuworld.kmpuiviews.generated.resources.global_search
import otakuworld.kmpuiviews.generated.resources.settings
import otakuworld.kmpuiviews.generated.resources.tabs_back
import otakuworld.kmpuiviews.generated.resources.tabs_downloads
import otakuworld.kmpuiviews.generated.resources.tabs_forward

private enum class HistoryAnchor { Back, Forward }

/** "App / Title / Chapter": the app name, then each page of the tab's back stack. */
fun tabPath(appName: String, backStack: List<NavKey>, keys: TabKeys): String =
    (listOf(appName) + backStack.map { keys.labelOf(it).text }).joinToString(" / ")

@Composable
fun TabToolbar(
    tab: TabState,
    controller: TabsController,
    keys: TabKeys = koinInject(),
    appConfig: AppConfig = koinInject(),
) {
    // Through the tab's own navigation, so the interceptor opens each page's tab once and then switches to it.
    val nav = tab.nav

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 4.dp),
            ) {
                // One menu at a time, under whichever button opened it; it shows the whole history either way
                var menuFrom by remember(tab.id) { mutableStateOf<HistoryAnchor?>(null) }
                val drag = remember(tab.id) { HistoryDrag() }
                val closeMenu = {
                    menuFrom = null
                    drag.pointer = null
                    drag.rowBounds.clear()
                }
                val jump = { index: Int ->
                    controller.jumpTo(tab.id, index)
                    closeMenu()
                }

                HistoryAnchor.entries.forEach { anchor ->
                    Box {
                        HistoryButton(
                            icon = if (anchor == HistoryAnchor.Back) Icons.AutoMirrored.Default.ArrowBack else Icons.AutoMirrored.Default.ArrowForward,
                            description = stringResource(if (anchor == HistoryAnchor.Back) Res.string.tabs_back else Res.string.tabs_forward),
                            enabled = if (anchor == HistoryAnchor.Back) tab.nav.history.canGoBack else tab.nav.history.canGoForward,
                            onClick = { if (anchor == HistoryAnchor.Back) controller.back(tab.id) else controller.forward(tab.id) },
                            onOpenMenu = { menuFrom = anchor },
                            onHoldMove = { drag.pointer = it },
                            onHoldRelease = {
                                drag.pointer = it
                                val hot = drag.hotIndex
                                drag.pointer = null
                                // Let go on another page jumps there; anywhere else leaves the menu open
                                if (hot != null && hot != tab.nav.history.index) jump(hot)
                            },
                            modifier = Modifier.padding(horizontal = 2.dp),
                        )
                        TabHistoryMenu(
                            tab = tab,
                            expanded = menuFrom == anchor,
                            onDismiss = closeMenu,
                            onJump = jump,
                            hotIndex = drag.hotIndex,
                            keys = keys,
                            reducedMotion = controller.motion.reduced,
                            onRowBounds = { index, bounds ->
                                if (bounds == null) drag.rowBounds.remove(index) else drag.rowBounds[index] = bounds
                            },
                        )
                    }
                }

                Text(
                    text = tabPath(appConfig.appName, tab.nav.backStack, keys),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )

                IconButton(onClick = { nav.globalSearch(null) }) {
                    Icon(Icons.Default.Search, stringResource(Res.string.global_search))
                }

                IconButton(onClick = { nav.customList() }) {
                    Icon(Icons.AutoMirrored.Default.List, stringResource(Res.string.custom_lists_title))
                }

                controller.downloadsKey?.let { key ->
                    IconButton(onClick = { nav.navigate(key) }) {
                        Icon(Icons.Default.Download, stringResource(Res.string.tabs_downloads))
                    }
                }

                // Not nav.settings(): that switches top-level destinations and skips the interceptor.
                IconButton(onClick = { nav.navigate(Screen.Settings) }) {
                    BadgedBox(badge = { if (updateCheck()) Badge { Text("") } }) {
                        Icon(Icons.Default.Settings, stringResource(Res.string.settings))
                    }
                }
            }
            HorizontalDivider()
        }
    }
}
