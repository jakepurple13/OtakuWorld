package com.programmersbox.koogintegration.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ModelTraining
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
import com.programmersbox.koogintegration.Koog
import com.programmersbox.koogintegration.KoogModels
import com.programmersbox.koogintegration.KoogSettings
import com.programmersbox.sharedcomponents.components.GenericBackButton

private const val SEGMENT_COUNT = 3

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KoogScreen(
    onNavigate: (NavKey) -> Unit,
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val segmentedColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Koog Settings") },
                navigationIcon = { GenericBackButton() },
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
                onClick = { onNavigate(Koog) },
                content = { Text("Koog") },
                leadingContent = { Icon(Icons.Default.AutoAwesome, null) },
                colors = segmentedColors
            )

            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(1, SEGMENT_COUNT),
                onClick = { onNavigate(KoogSettings) },
                content = { Text("Koog Config") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
                colors = segmentedColors
            )

            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(2, SEGMENT_COUNT),
                onClick = { onNavigate(KoogModels) },
                content = { Text("Koog Models") },
                leadingContent = { Icon(Icons.Default.ModelTraining, null) },
                colors = segmentedColors
            )
        }
    }
}