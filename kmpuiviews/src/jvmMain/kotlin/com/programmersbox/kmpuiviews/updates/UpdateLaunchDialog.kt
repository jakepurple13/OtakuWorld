package com.programmersbox.kmpuiviews.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import org.koin.compose.koinInject
import java.util.Locale

@Composable
fun UpdateLaunchDialog(updater: DesktopUpdater) {
    val show by updater.showLaunchDialog.collectAsState()
    val state by updater.state.collectAsState()
    if (!show) return
    val appName = koinInject<AppConfig>().appName
    val uriHandler = LocalUriHandler.current

    when (val s = state) {
        is DesktopUpdateState.Available -> AlertDialog(
            onDismissRequest = updater::dismissLaunchDialog,
            title = { Text("Update available") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$appName ${s.version} is ready to download. You’re on ${updater.currentVersion}.")
                    Text(
                        "${s.level.displayName()} · " +
                                "${updater.track.name} track",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { uriHandler.openUri(updater.releasePageUrl()) }) {
                        Text("Download from GitHub")
                    }
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = updater::skipThisVersion) { Text("Skip this version") }
                    TextButton(onClick = updater::dismissLaunchDialog) { Text("Later") }
                }
            },
            confirmButton = { TextButton(onClick = updater::download) { Text("Download") } },
        )

        is DesktopUpdateState.Downloading -> AlertDialog(
            onDismissRequest = updater::dismissLaunchDialog,
            title = { Text("Downloading ${s.version}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { if (s.total > 0) (s.percent / 100f).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("${s.bytes.toMb()} of ${s.total.toMb()} MB")
                    Text("You can keep reading. We’ll ask before restarting.")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        updater.cancelDownload()
                        updater.dismissLaunchDialog()
                    }
                ) { Text("Cancel") }
            },
            confirmButton = { TextButton(onClick = updater::dismissLaunchDialog) { Text("Hide") } },
        )

        is DesktopUpdateState.Ready -> AlertDialog(
            onDismissRequest = updater::dismissLaunchDialog,
            title = { Text("Restart to update") },
            text = {
                Text(
                    "$appName ${s.version} is downloaded and verified. " +
                            "The app will close, install the update, and reopen where you left off."
                )
            },
            dismissButton = { TextButton(onClick = updater::dismissLaunchDialog) { Text("Later") } },
            confirmButton = { TextButton(onClick = updater::restart) { Text("Restart now") } },
        )

        is DesktopUpdateState.Error -> AlertDialog(
            onDismissRequest = updater::dismissLaunchDialog,
            title = { Text("Update failed") },
            text = { Text(s.message) },
            dismissButton = {
                TextButton(onClick = { uriHandler.openUri(updater.releasePageUrl()) }) {
                    Text("Download from GitHub")
                }
            },
            confirmButton = { TextButton(onClick = updater::dismissLaunchDialog) { Text("Close") } },
        )

        else -> LaunchedEffect(Unit) { updater.dismissLaunchDialog() }
    }
}

@Composable
fun JustUpdatedSnackbar(updater: DesktopUpdater) {
    val notifier = koinInject<InAppNotifier>()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(Unit) {
        val version = updater.consumeJustUpdated() ?: return@LaunchedEffect
        notifier.success("Updated to $version", action = "What’s new") {
            uriHandler.openUri(updater.releasePageUrl())
        }
    }
}

private fun Long.toMb(): String = String.format(Locale.ROOT, "%.1f", this / 1_048_576.0)
