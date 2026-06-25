package com.programmersbox.kmpuiviews.presentation.settings.behavior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.general.HistorySettings
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContentReadingScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current
    val dataStoreHandling: DataStoreHandling = koinInject()

    SettingsScaffold(
        title = "Content & Reading",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Details") },
                leadingContent = { Icon(Icons.Default.Animation, null) },
                onClick = { navActions.navigate(Screen.DetailsSettings) },
            )
            segmentedListItem(
                content = { Text("Player") },
                leadingContent = { Icon(Icons.Default.PlayCircleOutline, null) },
                onClick = navActions::otherSettings,
            )
        }

        CategoryGroupListItem {
            item(false) { HistorySettings(dataStoreHandling = dataStoreHandling) }
        }

        composeSettingsDsl.generalSettings()
        composeSettingsDsl.contentReadingSettings()
    }
}
