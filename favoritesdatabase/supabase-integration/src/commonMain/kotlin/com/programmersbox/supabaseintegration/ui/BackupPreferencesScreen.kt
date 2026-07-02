package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupPreferenceItem
import com.programmersbox.supabaseintegration.ui.viewmodel.BackupPreferencesViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupPreferencesScreen(
    viewModel: BackupPreferencesViewModel = koinViewModel(),
) {
    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Backup Preferences") },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                uiState.items,
                key = BackupPreferenceItem::tableName
            ) { item ->
                ListItem(
                    content = { Text(item.displayName) },
                    checked = item.enabled,
                    onCheckedChange = { checked ->
                        viewModel.setBackupEnabled(item.tableName, checked)
                    },
                    trailingContent = {
                        Checkbox(
                            checked = item.enabled,
                            enabled = uiState.isLoggedIn,
                            onCheckedChange = null
                        )
                    },
                    enabled = uiState.isLoggedIn
                )
            }
        }
    }
}
