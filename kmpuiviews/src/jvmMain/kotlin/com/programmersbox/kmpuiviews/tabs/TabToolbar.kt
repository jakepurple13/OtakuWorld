package com.programmersbox.kmpuiviews.tabs

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

/** "App / Title / Chapter": the app name, then each page of the tab's back stack. */
fun tabPath(appName: String, backStack: List<NavKey>, keys: TabKeys): String =
    (listOf(appName) + backStack.map { keys.labelOf(it).text }).joinToString(" / ")

@Composable
fun TabToolbar(
    tab: TabState,
    controller: TabsController,
    keys: TabKeys = koinInject(),
    appConfig: AppConfig = koinInject(),
    downloads: DownloadsDestination = koinInject(),
) {
    val open = { key: NavKey -> controller.open(key, TabOpenMode.Foreground, tab.id) }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 4.dp),
            ) {
                IconButton(
                    onClick = { controller.back(tab.id) },
                    enabled = tab.nav.backStack.size > 1,
                ) { Icon(Icons.AutoMirrored.Default.ArrowBack, stringResource(Res.string.tabs_back)) }

                IconButton(
                    onClick = { controller.forward(tab.id) },
                    enabled = tab.forward.isNotEmpty(),
                ) { Icon(Icons.AutoMirrored.Default.ArrowForward, stringResource(Res.string.tabs_forward)) }

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

                IconButton(onClick = { open(Screen.GlobalSearchScreen()) }) {
                    Icon(Icons.Default.Search, stringResource(Res.string.global_search))
                }

                IconButton(onClick = { open(Screen.CustomListScreen) }) {
                    Icon(Icons.AutoMirrored.Default.List, stringResource(Res.string.custom_lists_title))
                }

                downloads.key?.let { key ->
                    IconButton(onClick = { open(key) }) {
                        Icon(Icons.Default.Download, stringResource(Res.string.tabs_downloads))
                    }
                }

                IconButton(onClick = { open(Screen.Settings) }) {
                    BadgedBox(badge = { if (updateCheck()) Badge { Text("") } }) {
                        Icon(Icons.Default.Settings, stringResource(Res.string.settings))
                    }
                }
            }
            HorizontalDivider()
        }
    }
}
