package com.programmersbox.kmpuiviews.tabs

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.programmersbox.kmpuiviews.presentation.components.rememberShownAtLeast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import otakuworld.kmpuiviews.generated.resources.tabs_downloads

/**
 * "App / … / Search / Title / Chapter": the app name, then the last [maxPages] pages of the tab's back stack, with "…"
 * after the app name when earlier pages were left out.
 */
fun tabPath(appName: String, backStack: List<NavKey>, keys: TabKeys, maxPages: Int = 3): String {
    val shown = backStack.takeLast(maxPages).map { keys.labelOf(it).text }
    val trimmed = if (backStack.size > maxPages) listOf("…") else emptyList()
    return (listOf(appName) + trimmed + shown).joinToString(" / ")
}

@Composable
fun TabToolbar(
    tab: TabState,
    controller: TabsController,
    keys: TabKeys = koinInject(),
    appConfig: AppConfig = koinInject(),
) {
    // Through the tab's own navigation, so each page joins this tab's history (an armed click still opens a new tab).
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
                HistoryNavButtons(
                    tab = tab,
                    keys = keys,
                    reducedMotion = controller.motion.reduced,
                    onBack = { controller.back(tab.id) },
                    onForward = { controller.forward(tab.id) },
                    onJump = { controller.jumpTo(tab.id, it) },
                )

                // The address bar: the tab's path, with Refresh at its end.
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .padding(horizontal = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tabPath(appConfig.appName, tab.nav.backStack, keys),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp, end = 4.dp),
                        )
                        RefreshButton(
                            registry = tab.refresh,
                            reduceMotion = controller.motion.reduced,
                            onRefresh = { controller.refresh(tab.id) },
                            onStop = { controller.stopRefresh(tab.id) },
                            modifier = Modifier.padding(end = 1.dp),
                        )
                    }
                }

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

                IconButton(onClick = { nav.navigate(Screen.Settings) }) {
                    BadgedBox(badge = { if (updateCheck()) Badge { Text("") } }) {
                        Icon(Icons.Default.Settings, stringResource(Res.string.settings))
                    }
                }
            }
            // The page's progress grows over the divider while it reloads, in a fixed slot so nothing shifts. It stays
            // at least 600 ms so a fast reload still animates in and out.
            Box(Modifier.fillMaxWidth().height(2.dp), contentAlignment = Alignment.Center) {
                HorizontalDivider()
                ToolbarProgress(
                    visible = rememberShownAtLeast(tab.refresh.refreshing, minMillis = 600),
                    reduced = controller.motion.reduced,
                )
            }
        }
    }
}

@Composable
private fun ToolbarProgress(visible: Boolean, reduced: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = if (reduced) fadeIn() else fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = if (reduced) fadeOut() else fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = Modifier.fillMaxSize(),
    ) {
        LinearProgressIndicator(Modifier.fillMaxSize())
    }
}
