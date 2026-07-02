package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupPreferenceItem
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupPreferencesViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupPreferencesScreen(viewModel: BackupPreferencesViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Backup Preferences") },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(uiState.items, key = BackupPreferenceItem::tableName) { item ->
                ListItem(
                    headlineContent = { Text(item.displayName) },
                    trailingContent = {
                        Switch(
                            checked = item.enabled,
                            enabled = uiState.isLoggedIn,
                            onCheckedChange = { checked ->
                                viewModel.setBackupEnabled(item.tableName, checked)
                            }
                        )
                    }
                )
            }
        }
    }
}
