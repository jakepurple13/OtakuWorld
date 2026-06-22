package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SupabaseSettingsScreen(
    onNavigate: (NavKey) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supabase") }
            )
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.padding(padding)
        ) {
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(0, 4),
                onClick = { onNavigate(SupabaseConfigRoute) },
                content = { Text("Supabase Config") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
            )

            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(1, 4),
                onClick = { onNavigate(AuthRoute) },
                content = { Text("Supabase Auth") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
            )

            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(2, 4),
                onClick = { onNavigate(SyncStatusRoute) },
                content = { Text("Supabase Sync Status") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
            )

            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(3, 4),
                onClick = { onNavigate(BackupRestoreRoute) },
                content = { Text("Supabase Backup/Restore") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
            )
        }
    }
}