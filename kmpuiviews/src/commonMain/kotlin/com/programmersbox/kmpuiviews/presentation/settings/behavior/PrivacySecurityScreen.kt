package com.programmersbox.kmpuiviews.presentation.settings.behavior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.LocalNavActions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrivacySecurityScreen() {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Privacy & Security",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Biometric Lock") },
                leadingContent = { Icon(Icons.Default.Security, null) },
                onClick = navActions::security,
            )
            segmentedListItem(
                content = { Text("Incognito Sources") },
                leadingContent = { Icon(Icons.Default.VisibilityOff, null) },
                onClick = navActions::incognito,
            )
        }
    }
}
