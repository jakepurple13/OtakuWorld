package com.programmersbox.kmpuiviews.presentation.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bento
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperScreen() {
    val navActions = LocalNavActions.current
    val appConfig: AppConfig = koinInject()

    SettingsScaffold(
        title = "Developer",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            if (appConfig.isDebug) {
                segmentedListItem(
                    content = { Text("Debug Menu") },
                    leadingContent = { Icon(Icons.Default.Android, null) },
                    onClick = navActions::debug,
                )
            }
            segmentedListItem(
                content = { Text("Pre-release Builds") },
                leadingContent = { Icon(Icons.Default.Bento, null) },
                onClick = navActions::prerelease,
            )
            segmentedListItem(
                content = { Text("Color Helper") },
                leadingContent = { Icon(Icons.Default.Colorize, null) },
                onClick = { navActions.navigate(Screen.ColorHelper) },
            )
        }
    }
}
