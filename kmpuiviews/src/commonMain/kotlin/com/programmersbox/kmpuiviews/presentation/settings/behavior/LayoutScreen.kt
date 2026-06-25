package com.programmersbox.kmpuiviews.presentation.settings.behavior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.general.DetailPaneSettings
import com.programmersbox.kmpuiviews.presentation.settings.general.GridTypeSettings
import com.programmersbox.kmpuiviews.presentation.settings.general.NavigationBarSettings
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LayoutScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val handling: NewSettingsHandling = koinInject()

    SettingsScaffold(
        title = "Layout",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            item(false) { GridTypeSettings(handling = handling) }
            item(false) { DetailPaneSettings(handling = handling) }
        }

        CategoryGroupListItem {
            item { NavigationBarSettings(handling = handling) }
        }

        composeSettingsDsl.layoutSettings()
    }
}
