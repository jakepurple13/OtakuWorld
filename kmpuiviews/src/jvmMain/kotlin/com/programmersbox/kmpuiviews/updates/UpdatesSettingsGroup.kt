package com.programmersbox.kmpuiviews.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import org.koin.compose.koinInject
import java.util.Locale

private const val UPDATES_COUNT = 4

/** "Updates" header plus the 4 update rows. Needs [DesktopUpdater] and [MangaDesktopSettings] in Koin. */
fun LazyListScope.updatesSettingsGroup() {
    item(contentType = "updatesHeader") {
        Text(
            "Updates",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .animateItem()
                .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
        )
    }
    item(contentType = "updatesCheck") { CheckForUpdatesItem() }
    item(contentType = "updatesAuto") { CheckAutomaticallyItem() }
    item(contentType = "updatesTrack") { UpdateTrackItem() }
    item(contentType = "updatesGithub") { DownloadFromGitHubItem() }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LazyItemScope.CheckForUpdatesItem() {
    val updater = koinInject<DesktopUpdater>()
    val state by updater.state.collectAsState()

    val highlighted = state is DesktopUpdateState.Available ||
            state is DesktopUpdateState.Downloading ||
            state is DesktopUpdateState.Ready
    val container = when {
        state is DesktopUpdateState.Error -> MaterialTheme.colorScheme.errorContainer
        highlighted -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val colors = staticItemColors(
        container = container,
        content = if (state is DesktopUpdateState.Error) MaterialTheme.colorScheme.onErrorContainer
        else if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        supporting = if (state is DesktopUpdateState.Error) MaterialTheme.colorScheme.onErrorContainer
        else if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val status = when (val s = state) {
        DesktopUpdateState.Idle -> "Not checked yet"
        DesktopUpdateState.Checking -> "Checking GitHub…"
        DesktopUpdateState.UpToDate -> "Up to date · checked just now"
        is DesktopUpdateState.Available -> "${s.version} is ready to download"
        is DesktopUpdateState.Downloading ->
            "Downloading ${s.version} · ${s.bytes.toMb()} of ${s.total.toMb()} MB"

        is DesktopUpdateState.Ready -> "${s.version} downloaded · restart to finish"
        is DesktopUpdateState.Error -> s.message
        DesktopUpdateState.Unsupported -> "Updates are off when running from Gradle"
    }

    SegmentedListItem(
        onClick = {},
        enabled = false,
        content = {
            Row {
                Text(
                    when (state) {
                        is DesktopUpdateState.Available -> "Update available"
                        is DesktopUpdateState.Ready -> "Ready to install"
                        else -> "Check for updates"
                    }
                )
                (state as? DesktopUpdateState.Available)?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            it.level.name.lowercase().replaceFirstChar { c -> c.titlecase(Locale.ROOT) },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        supportingContent = {
            Column {
                Text(status)
                (state as? DesktopUpdateState.Downloading)?.let {
                    LinearProgressIndicator(
                        progress = { it.percent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                Text("Current: ${updater.currentVersion}")
            }
        },
        leadingContent = { Icon(Icons.Default.SystemUpdateAlt, null) },
        trailingContent = {
            when (state) {
                DesktopUpdateState.Idle, DesktopUpdateState.UpToDate ->
                    TextButton(onClick = updater::checkNow) { Text("Check now") }

                DesktopUpdateState.Checking -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                is DesktopUpdateState.Available -> TextButton(onClick = updater::download) { Text("Download") }
                is DesktopUpdateState.Downloading -> TextButton(onClick = updater::cancelDownload) { Text("Cancel") }
                is DesktopUpdateState.Ready -> TextButton(onClick = updater::restart) { Text("Restart") }
                is DesktopUpdateState.Error -> TextButton(onClick = updater::checkNow) { Text("Retry") }
                DesktopUpdateState.Unsupported -> Unit
            }
        },
        colors = colors,
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = UPDATES_COUNT),
        modifier = Modifier.animateItem()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LazyItemScope.CheckAutomaticallyItem() {
    val updater = koinInject<DesktopUpdater>()
    val settings = koinInject<MangaDesktopSettings>()
    val state by updater.state.collectAsState()
    var checkOnLaunch by settings.checkUpdatesOnLaunch.asState()
    val enabled = state != DesktopUpdateState.Unsupported

    SegmentedListItem(
        content = { Text("Check automatically") },
        supportingContent = { Text("Look for updates each time the app starts") },
        leadingContent = { Icon(Icons.Default.Schedule, null) },
        trailingContent = {
            Switch(checked = checkOnLaunch, onCheckedChange = null, enabled = enabled)
        },
        checked = checkOnLaunch,
        onCheckedChange = { checkOnLaunch = it },
        enabled = enabled,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shapes = ListItemDefaults.segmentedShapes(index = 1, count = UPDATES_COUNT),
        modifier = Modifier.animateItem()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LazyItemScope.UpdateTrackItem() {
    val updater = koinInject<DesktopUpdater>()
    val track = updater.track

    SegmentedListItem(
        onClick = {},
        enabled = false,
        content = { Text("Update track · ${track.name}") },
        supportingContent = {
            val gets = when (track) {
                UpdateTrack.Nightly -> "Gets nightly and stable releases"
                UpdateTrack.Stable -> "Gets stable releases only"
            }
            Text("$gets. Set by the build you installed.")
        },
        leadingContent = { Icon(Icons.Default.Update, null) },
        colors = staticItemColors(
            container = MaterialTheme.colorScheme.surfaceContainer,
            content = MaterialTheme.colorScheme.onSurface,
            supporting = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        shapes = ListItemDefaults.segmentedShapes(index = 2, count = UPDATES_COUNT),
        modifier = Modifier.animateItem()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LazyItemScope.DownloadFromGitHubItem() {
    val updater = koinInject<DesktopUpdater>()
    val uriHandler = LocalUriHandler.current

    SegmentedListItem(
        onClick = { uriHandler.openUri(updater.releasePageUrl()) },
        content = { Text("Download from GitHub") },
        supportingContent = {
            Text("Open the ${updater.track.name} release page and install it yourself")
        },
        leadingContent = { Icon(Icons.Default.OpenInBrowser, null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shapes = ListItemDefaults.segmentedShapes(index = 3, count = UPDATES_COUNT),
        modifier = Modifier.animateItem()
    )
}

private fun Long.toMb(): String = String.format(Locale.ROOT, "%.1f", this / 1_048_576.0)

/** Colors for non-interactive rows (`enabled = false`) so content keeps its normal, undimmed colors. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun staticItemColors(container: Color, content: Color, supporting: Color) = ListItemDefaults.segmentedColors(
    containerColor = container,
    contentColor = content,
    leadingContentColor = content,
    trailingContentColor = content,
    overlineContentColor = supporting,
    supportingContentColor = supporting,
    disabledContainerColor = container,
    disabledContentColor = content,
    disabledLeadingContentColor = content,
    disabledTrailingContentColor = content,
    disabledOverlineContentColor = supporting,
    disabledSupportingContentColor = supporting,
)
