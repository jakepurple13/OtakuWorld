package com.programmersbox.supabaseintegration.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpmodels.ManagedTable
import com.programmersbox.kmpmodels.SupportedTableAction
import com.programmersbox.supabaseintegration.ui.viewmodel.ManagedTableSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoutBottomSheet(
    manageDatabasesEnabled: Boolean,
    tableSelections: List<ManagedTableSelection>,
    onManageDatabasesEnabledChange: (Boolean) -> Unit,
    onTableActionChange: (ManagedTable, SupportedTableAction) -> Unit,
    onContinueToLogout: () -> Unit,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = manageDatabasesEnabled,
                    onCheckedChange = onManageDatabasesEnabledChange,
                )
                Text("Manage local databases")
            }

            AnimatedVisibility(visible = manageDatabasesEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                    tableSelections.forEach { selection ->
                        ManagedTableRow(
                            selection = selection,
                            onActionChange = { action -> onTableActionChange(selection.table, action) },
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = onContinueToLogout, modifier = Modifier.weight(1f)) {
                    Text("Continue to Logout")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagedTableRow(
    selection: ManagedTableSelection,
    onActionChange: (SupportedTableAction) -> Unit,
) {
    Column {
        Text(selection.table.displayName, style = MaterialTheme.typography.bodyLarge)
        val options = selection.table.supportedActions
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            options.forEachIndexed { index, action ->
                val isDestructive = action.isDestructive
                SegmentedButton(
                    selected = selection.selectedAction == action,
                    onClick = { onActionChange(action) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = if (isDestructive) {
                        SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.errorContainer,
                            activeContentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    } else {
                        SegmentedButtonDefaults.colors()
                    },
                ) {
                    Text(actionLabel(action))
                }
            }
        }
    }
}

private fun actionLabel(action: SupportedTableAction): String = when (action) {
    SupportedTableAction.NONE -> "None"
    SupportedTableAction.CLEAR_ALL -> "Clear All"
    SupportedTableAction.PURGE_DELETED -> "Purge Deleted"
    SupportedTableAction.RESTORE_DELETED -> "Restore Deleted"
}
