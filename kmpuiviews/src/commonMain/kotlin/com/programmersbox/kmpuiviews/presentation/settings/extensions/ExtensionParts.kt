package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.programmersbox.kmpuiviews.IconLoader
import com.programmersbox.kmpuiviews.SourceIcon
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus

/** Letter avatar under the remote icon, so a missing/failed icon still reads. */
@Composable
internal fun RemoteIcon(name: String, iconUrl: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Text(
            name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        if (iconUrl.isNotBlank()) {
            AsyncImage(model = iconUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

internal fun statusText(status: DownloadAndInstallStatus): String = when (status) {
    is DownloadAndInstallStatus.Downloading -> "Downloading ${(status.progress * 100).toInt()}%"
    DownloadAndInstallStatus.Downloaded -> "Downloaded"
    DownloadAndInstallStatus.Installing -> "Installing"
    DownloadAndInstallStatus.PendingUserAction -> "Waiting for you"
    DownloadAndInstallStatus.PermissionRequired -> "Permission needed"
    DownloadAndInstallStatus.Installed -> "Installed"
    DownloadAndInstallStatus.Cancelled -> "Cancelled"
    is DownloadAndInstallStatus.Error -> "Failed: ${status.message}"
}

/** Determinate only while downloading; other in-flight states show indeterminate. */
@Composable
internal fun InstallProgress(status: DownloadAndInstallStatus, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            statusText(status),
            style = MaterialTheme.typography.labelSmall,
            color = if (status is DownloadAndInstallStatus.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        when (status) {
            is DownloadAndInstallStatus.Downloading -> LinearProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth())
            is DownloadAndInstallStatus.Error -> Unit
            else -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun SummaryRow(
    summary: InstalledSummary,
    filter: InstalledFilter,
    onFilter: (InstalledFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        SummaryTile(
            count = summary.installed,
            label = "installed",
            container = MaterialTheme.colorScheme.surfaceContainer,
            content = MaterialTheme.colorScheme.onSurface,
            selected = false,
            onClick = if (filter != InstalledFilter.None) ({ onFilter(InstalledFilter.None) }) else null,
            modifier = Modifier.weight(1f)
        )
        SummaryTile(
            count = summary.updates,
            label = "updates",
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            selected = filter == InstalledFilter.Updates,
            onClick = if (summary.updates > 0) ({ onFilter(InstalledFilter.Updates) }) else null,
            modifier = Modifier.weight(1f)
        )
        SummaryTile(
            count = summary.untrusted,
            label = "untrusted",
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            selected = filter == InstalledFilter.Untrusted,
            onClick = if (summary.untrusted > 0) ({ onFilter(InstalledFilter.Untrusted) }) else null,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Count tiles with nothing to show render neutral and are not clickable. */
@Composable
private fun SummaryTile(
    count: Int,
    label: String,
    container: Color,
    content: Color,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val active = count > 0
    val bg = if (active) container else MaterialTheme.colorScheme.surfaceContainer
    val fg = if (active) content else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        colors = CardDefaults.cardColors(
            containerColor = bg,
            contentColor = fg,
            disabledContainerColor = bg,
            disabledContentColor = fg,
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun RemoteItem.subtitle(): String = when (val s = state) {
    is RemoteItemState.UpdateAvailable -> "Update"
    is RemoteItemState.Installing -> (s.status as? DownloadAndInstallStatus.Downloading)
        ?.let { "${(it.progress * 100).toInt()}%" } ?: statusText(s.status)

    else -> remote.lang.takeIf { it.isNotBlank() }?.let(::displayLang) ?: "v${remote.version}"
}

@Composable
internal fun ExtensionTile(
    item: RemoteItem,
    showCatalog: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Box {
            RemoteIcon(item.remote.name, item.remote.iconUrl, 56.dp)
            if (item.state is RemoteItemState.Installed) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Installed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                )
            }
        }
        Text(item.remote.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (showCatalog) item.catalog else item.subtitle(),
            style = MaterialTheme.typography.labelSmall,
            color = if (!showCatalog && item.state is RemoteItemState.UpdateAvailable) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Non-lazy 3-column rows, for grids nested inside a LazyColumn item. */
@Composable
internal fun TileRows(
    items: List<RemoteItem>,
    showCatalog: Boolean,
    onClick: (RemoteItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    ExtensionTile(item, showCatalog, onClick = { onClick(item) }, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun InstalledRow(
    item: InstalledItem,
    isCurrent: Boolean,
    iconLoader: IconLoader,
    onClick: () -> Unit,
    onTrust: () -> Unit,
    onUpdate: () -> Unit,
    onSettings: (() -> Unit)?,
    onSetCurrent: () -> Unit,
    onUninstall: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var confirmUninstall by remember { mutableStateOf(false) }
    if (confirmUninstall) {
        ConfirmUninstallDialog(item.info.name, onConfirm = onUninstall, onDismiss = { confirmUninstall = false })
    }
    val untrusted = item.untrusted
    val update = item.update
    val installing = item.installing
    val trustAction = untrusted?.canTrust == true
    val updateAction = !trustAction && update != null
    val settingsAction = !trustAction && !updateAction && onSettings != null
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.info.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (isCurrent) Pill("Current", MaterialTheme.colorScheme.primaryContainer)
            }
        },
        supportingContent = {
            when {
                untrusted != null -> Text(untrusted.reason, color = MaterialTheme.colorScheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis)
                installing != null -> InstallProgress(installing, Modifier.width(160.dp))
                update != null -> Text("${item.version} → ${update.version}")
                else -> Text("v${item.version}")
            }
        },
        leadingContent = { SourceIcon(iconLoader, item.info) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    trustAction -> FilledTonalButton(
                        onClick = onTrust,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    ) { Text("Trust") }

                    updateAction && installing == null && update != null -> FilledTonalButton(onClick = onUpdate) {
                        Icon(Icons.Default.Update, null, modifier = Modifier.size(18.dp))
                        Text(update.version, modifier = Modifier.padding(start = 4.dp))
                    }

                    settingsAction && onSettings != null -> IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Set as current source") },
                            enabled = untrusted == null && !isCurrent,
                            onClick = {
                                showMenu = false
                                onSetCurrent()
                            }
                        )
                        if (onSettings != null && !settingsAction) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showMenu = false
                                    onSettings()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Uninstall", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                confirmUninstall = true
                            }
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
internal fun ConfirmUninstallDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uninstall $name?") },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Uninstall") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun Pill(text: String, color: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    Surface(color = color, shape = RoundedCornerShape(6.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
