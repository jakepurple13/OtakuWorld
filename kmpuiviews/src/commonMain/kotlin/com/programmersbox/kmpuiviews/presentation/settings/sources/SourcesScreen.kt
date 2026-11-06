package com.programmersbox.kmpuiviews.presentation.settings.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.updatesources.updateSourcesCheckedCount
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.utils.showSourceChooser
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalCurrentSource
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.currentSource
import otakuworld.kmpuiviews.generated.resources.us_summary
import otakuworld.kmpuiviews.generated.resources.us_title
import otakuworld.kmpuiviews.generated.resources.view_extensions
import otakuworld.kmpuiviews.generated.resources.view_source_in_browser

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourcesScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
    itemDao: ItemDao = koinInject(),
) {
    val navActions = LocalNavActions.current
    val uriHandler = LocalUriHandler.current
    val source by LocalCurrentSource.current.asFlow().collectAsStateWithLifecycle(null)
    var showSourceChooser by showSourceChooser()
    val updateSourcesCount by remember(itemDao) { itemDao.updateSourcesCheckedCount() }.collectAsStateWithLifecycle(null)

    SettingsScaffold(
        title = "Sources & Extensions",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text(stringResource(Res.string.currentSource, source?.displayName.orEmpty())) },
                leadingContent = { Icon(Icons.Default.Source, null) },
                onClick = { showSourceChooser = true },
            )
            segmentedListItem(
                content = { Text("Source Order") },
                leadingContent = { Icon(Icons.Default.Reorder, null) },
                onClick = navActions::order,
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.view_extensions)) },
                leadingContent = { Icon(Icons.Default.Extension, null) },
                onClick = navActions::extensionList,
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.us_title)) },
                supportingContent = {
                    updateSourcesCount?.let { (checked, total) ->
                        Text(stringResource(Res.string.us_summary, checked, total))
                    }
                },
                leadingContent = { Icon(Icons.Default.FilterList, null) },
                onClick = { navActions.navigate(Screen.UpdateSourcesSettings) },
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.view_source_in_browser)) },
                leadingContent = { Icon(Icons.Default.OpenInBrowser, null) },
                onClick = { source?.baseUrl?.let { uriHandler.openUri(it) } },
            )
            apply(composeSettingsDsl.sourcesSettings)
        }
    }
}
