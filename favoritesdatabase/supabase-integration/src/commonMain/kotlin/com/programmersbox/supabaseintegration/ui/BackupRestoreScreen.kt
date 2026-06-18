package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.backup.BackupEntry
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupRestoreViewModel
import kotlinx.datetime.Instant
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel = koinViewModel(),
    getLocalDbPath: () -> String,
) {
    val backups by viewModel.backups.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var confirmRestore by remember { mutableStateOf<BackupEntry?>(null) }

    LaunchedEffect(Unit) { viewModel.loadBackups() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Backup & Restore", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.uploadBackup(getLocalDbPath()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Back Up Now")
        }
        status?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        Text("Available Backups", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(backups) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Instant.fromEpochMilliseconds(entry.createdAt).toString(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        OutlinedButton(onClick = { confirmRestore = entry }) { Text("Restore") }
                    }
                }
            }
        }
    }

    confirmRestore?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("Restore Backup?") },
            text = { Text("This will replace your local data with \"${entry.name}\". The app must be restarted after restore.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.downloadBackup(entry, getLocalDbPath())
                    confirmRestore = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = null }) { Text("Cancel") }
            }
        )
    }
}
