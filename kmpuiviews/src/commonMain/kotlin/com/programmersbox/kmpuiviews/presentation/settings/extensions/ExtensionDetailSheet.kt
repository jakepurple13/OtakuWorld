package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import com.programmersbox.kmpuiviews.utils.LocalNavActions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RemoteItemSheet(
    viewModel: ExtensionListViewModel,
    itemKey: String?,
    onDismiss: () -> Unit,
) {
    val item = itemKey?.let { viewModel.item(it) } ?: return
    val navActions = LocalNavActions.current
    val remote = item.remote
    var confirmUninstall by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RemoteIcon(remote.name, remote.iconUrl, 56.dp)
                Column(Modifier.weight(1f)) {
                    Text(remote.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.catalog, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (remote.nsfw) Pill("18+", MaterialTheme.colorScheme.errorContainer)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val version = when (val s = item.state) {
                    is RemoteItemState.UpdateAvailable -> "${s.installedVersion} → ${remote.version}"
                    else -> remote.version
                }
                KeyValue("Version", version)
                KeyValue("Package", remote.packageName, mono = true)
                if (remote.lang.isNotBlank()) KeyValue("Language", displayLang(remote.lang))
            }

            if (remote.sources.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Sources in this extension",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        remote.sources.map { it.name }.distinct().forEach { Pill(it) }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (val s = item.state) {
                    RemoteItemState.NotInstalled -> Button(onClick = { viewModel.install(remote) }) {
                        Icon(Icons.Default.Download, null)
                        Text("Install", modifier = Modifier.padding(start = 6.dp))
                    }

                    is RemoteItemState.UpdateAvailable -> Button(onClick = { viewModel.install(remote) }) {
                        Text("Update to ${remote.version}")
                    }

                    is RemoteItemState.Installing -> {
                        InstallProgress(s.status, Modifier.width(200.dp))
                        if (s.status is DownloadAndInstallStatus.Error) {
                            Button(onClick = { viewModel.install(remote) }) { Text("Try again") }
                        }
                    }

                    is RemoteItemState.Installed -> {
                        TextButton(
                            onClick = { confirmUninstall = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Uninstall") }
                        val api = s.installed.apiService
                        if (api is KmpConfigurableApiService) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    navActions.navigate(Screen.SourcePreferences(api.serviceName))
                                }
                            ) { Text("Settings") }
                        }
                        if (confirmUninstall) {
                            ConfirmUninstallDialog(
                                remote.name,
                                onConfirm = { viewModel.uninstall(s.installed) },
                                onDismiss = { confirmUninstall = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyValue(key: String, value: String, mono: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
