package com.programmersbox.sharedcomponents.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WizardItemRow(
    item: WizardItemState,
    onToggleSelected: () -> Unit,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = item.selected, onCheckedChange = { onToggleSelected() })
            item.uiInfo.icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.uiInfo.displayName, style = MaterialTheme.typography.bodyLarge)
                item.summary?.itemCount?.let {
                    Text("$it items", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onToggleExpanded) {
                Icon(if (item.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
        }
        AnimatedVisibility(visible = item.expanded) {
            Column(modifier = Modifier.padding(start = 48.dp, top = 4.dp, bottom = 4.dp)) {
                item.uiInfo.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                item.summary?.let { summary ->
                    summary.itemCount?.let { Text("Records: $it", style = MaterialTheme.typography.bodySmall) }
                    summary.sizeBytes?.let { Text("Size: $it bytes", style = MaterialTheme.typography.bodySmall) }
                    summary.details.forEach { (k, v) ->
                        Text("$k: $v", style = MaterialTheme.typography.bodySmall)
                    }
                } ?: Text("Loading…", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
