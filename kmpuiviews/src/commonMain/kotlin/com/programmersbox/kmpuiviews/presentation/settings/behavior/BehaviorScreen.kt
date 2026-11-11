package com.programmersbox.kmpuiviews.presentation.settings.behavior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Today
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_settings_title

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BehaviorScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Behavior",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Layout") },
                leadingContent = { Icon(Icons.Default.GridView, null) },
                onClick = navActions::layout,
            )
            segmentedListItem(
                content = { Text("Content & Reading") },
                leadingContent = { Icon(Icons.Default.MenuBook, null) },
                onClick = navActions::contentReading,
            )
            segmentedListItem(
                content = { Text("Notifications") },
                leadingContent = { Icon(Icons.Default.Notifications, null) },
                onClick = navActions::notificationsSettings,
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.do_settings_title)) },
                leadingContent = { Icon(Icons.Default.Today, null) },
                onClick = { navActions.navigate(Screen.DailyOverview.Settings) },
            )
            segmentedListItem(
                content = { Text("Privacy & Security") },
                leadingContent = { Icon(Icons.Default.Security, null) },
                onClick = navActions::privacySecurity,
            )
        }

        composeSettingsDsl.behaviorSettings()
    }
}
