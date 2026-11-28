package com.programmersbox.kmpuiviews.tabs

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
                HistoryNavButtons(
                    tab = tab,
                    keys = keys,
                    reducedMotion = controller.motion.reduced,
                    onBack = { controller.back(tab.id) },
                    onForward = { controller.forward(tab.id) },
                    onJump = { controller.jumpTo(tab.id, it) },
                )

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
