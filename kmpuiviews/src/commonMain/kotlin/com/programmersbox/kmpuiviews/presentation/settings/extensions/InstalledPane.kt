package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpuiviews.IconLoader
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.LocalCurrentSource
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun InstalledPane(
    viewModel: ExtensionListViewModel,
    modifier: Modifier = Modifier,
) {
    val iconLoader = koinInject<IconLoader>()
    val navActions = LocalNavActions.current
    val currentSourceRepository = LocalCurrentSource.current
    val currentSource by currentSourceRepository.asFlow().collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val filtering = viewModel.filter != InstalledFilter.None

    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "summary") {
            SummaryRow(
                summary = viewModel.summary,
                filter = viewModel.filter,
                onFilter = { viewModel.filter = viewModel.filter.toggle(it) },
            )
        }

        if (viewModel.filter == InstalledFilter.Updates && viewModel.summary.updates > 0) {
            item(key = "updateAll") {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Button(onClick = viewModel::updateAll) { Text("Update all") }
                }
            }
        }

        viewModel.visibleInstalled.forEach { (catalog, rows) ->
            item(key = "catalog:$catalog") {
                val needsAttention = rows.any { it.update != null || it.untrusted != null }
                val open = filtering || (viewModel.expanded[catalog] ?: needsAttention)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .animateItem()
                ) {
                    CatalogHeader(
                        title = catalog ?: "Other sources",
                        rows = rows,
                        open = open,
                        iconLoader = iconLoader,
                        onToggle = { if (!filtering) viewModel.expanded[catalog] = !open },
                        onUninstallAll = rows.map { it.info }
                            .takeIf { catalog != null && it.canUninstallAll() }
                            ?.let { infos -> { viewModel.uninstall(infos.first()) } },
                    )
                    if (open) {
                        rows.forEach { row ->
                            val api = row.info.apiService
                            InstalledRow(
                                item = row,
                                isCurrent = currentSource?.serviceName == api.serviceName,
                                iconLoader = iconLoader,
                                onClick = { if (row.untrusted == null) currentSourceRepository.tryEmit(api) },
                                onTrust = { viewModel.trust(row.info) },
                                onUpdate = { row.update?.let(viewModel::install) },
                                onSettings = if (api is KmpConfigurableApiService) {
                                    { navActions.navigate(Screen.SourcePreferences(api.serviceName)) }
                                } else {
                                    null
                                },
                                onSetCurrent = { scope.launch { currentSourceRepository.emit(api) } },
                                onUninstall = { viewModel.uninstall(row.info) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogHeader(
    title: String,
    rows: List<InstalledItem>,
    open: Boolean,
    iconLoader: IconLoader,
    onToggle: () -> Unit,
    onUninstallAll: (() -> Unit)?,
) {
    val updates = rows.count { it.update != null }
    val rotation by animateFloatAsState(if (open) 180f else 0f)
    var showMenu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    if (confirm && onUninstallAll != null) {
        ConfirmUninstallDialog(title, onConfirm = onUninstallAll, onDismiss = { confirm = false })
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
            rows.take(3).forEach { InstalledIcon(iconLoader, it.info, 32.dp) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row {
                Text("${rows.size} extensions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (updates > 0) {
                    Text(" · $updates updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (onUninstallAll != null) {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Uninstall all", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            confirm = true
                        }
                    )
                }
            }
        }
        Icon(
            Icons.Default.ExpandMore,
            null,
            modifier = Modifier
                .padding(end = 8.dp)
                .rotate(rotation)
        )
    }
}
