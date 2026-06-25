package com.programmersbox.kmpuiviews.presentation.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.LocalNavActions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiagnosticsScreen() {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Diagnostics",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Background Worker Info") },
                leadingContent = { Icon(Icons.Default.Engineering, null) },
                onClick = navActions::workerInfo,
            )
            segmentedListItem(
                content = { Text("Exceptions") },
                leadingContent = { Icon(Icons.Default.Error, null) },
                onClick = { navActions.navigate(Screen.ExceptionScreen) },
            )
        }
    }
}
