package com.programmersbox.kmpuiviews.presentation.settings.discover

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupScope
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.koogintegration.KoogScreen

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
actual fun CategoryGroupScope.discoverPaths(navActions: NavigationActions) {
    segmentedListItem(
        content = { Text("Koog") },
        leadingContent = { Icon(Icons.Default.AutoAwesome, null) },
        onClick = { navActions.navigate(KoogScreen) }
    )
}