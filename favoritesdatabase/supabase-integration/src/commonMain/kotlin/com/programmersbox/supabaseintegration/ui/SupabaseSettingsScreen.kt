package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation3.runtime.NavKey

private const val SEGMENT_COUNT = 4

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SupabaseSettingsScreen(
    onNavigate: (NavKey) -> Unit,
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val segmentedColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Supabase Settings") },
                navigationIcon = { BackButton() },
                subtitle = { Text("Settings for the Supabase integration") },
                scrollBehavior = topAppBarScrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(0, SEGMENT_COUNT),
                onClick = { onNavigate(SupabaseConfigRoute) },
                content = { Text("Supabase Config") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
                colors = segmentedColors
            )

            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(1, SEGMENT_COUNT),
                onClick = { onNavigate(AuthRoute) },
                content = { Text("Supabase Auth") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
                colors = segmentedColors
            )

            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(2, SEGMENT_COUNT),
                onClick = { onNavigate(SyncStatusRoute) },
                content = { Text("Supabase Sync Status") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
                colors = segmentedColors
            )

            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(3, SEGMENT_COUNT),
                onClick = { onNavigate(BackupPreferencesRoute) },
                content = { Text("Supabase Backup Choices") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
                colors = segmentedColors
            )

            /*SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(3, 4),
                onClick = { onNavigate(BackupRestoreRoute) },
                content = { Text("Supabase Backup/Restore") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
            )*/
        }
    }
}