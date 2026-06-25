package com.programmersbox.kmpuiviews.presentation.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Appearance",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Theme") },
                leadingContent = { Icon(Icons.Default.Palette, null) },
                onClick = { navActions.navigate(Screen.ThemeSettings) },
            )
            segmentedListItem(
                content = { Text("Colors") },
                leadingContent = { Icon(Icons.Default.ColorLens, null) },
                onClick = { navActions.navigate(Screen.Settings.Colors) },
            )
            segmentedListItem(
                content = { Text("Blur Effects") },
                leadingContent = { Icon(Icons.Default.BlurOn, null) },
                onClick = { navActions.navigate(Screen.Settings.Blur) },
            )
        }

        composeSettingsDsl.appearanceSettings()
    }
}
