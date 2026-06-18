package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.sync.SyncState
import com.programmersbox.supabaseintegration.ui.viewmodel.SyncViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SyncStatusScreen(viewModel: SyncViewModel = koinViewModel()) {
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    val idleColor = MaterialTheme.colorScheme.onSurface
    val syncingColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val offlineColor = MaterialTheme.colorScheme.tertiary

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sync Status", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        val statusLabel: String
        val statusColor: Color
        when (val state = syncState) {
            is SyncState.Idle -> {
                statusLabel = "Idle"
                statusColor = idleColor
            }
            is SyncState.Syncing -> {
                statusLabel = "Syncing…"
                statusColor = syncingColor
            }
            is SyncState.Error -> {
                statusLabel = "Error: ${state.message}"
                statusColor = errorColor
            }
            is SyncState.Offline -> {
                statusLabel = "Offline — polling when connection restores"
                statusColor = offlineColor
            }
            else -> {
                statusLabel = ""
                statusColor = idleColor
            }
        }

        Text(statusLabel, color = statusColor, style = MaterialTheme.typography.bodyLarge)
        if (syncState is SyncState.Syncing) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::triggerSync,
            enabled = syncState is SyncState.Idle || syncState is SyncState.Error,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sync Now") }
    }
}
