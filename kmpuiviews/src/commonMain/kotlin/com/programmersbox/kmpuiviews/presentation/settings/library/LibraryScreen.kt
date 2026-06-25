package com.programmersbox.kmpuiviews.presentation.settings.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Library",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Favorites") },
                leadingContent = { Icon(Icons.Default.Star, null) },
                onClick = navActions::favorites,
            )
            segmentedListItem(
                content = { Text("History") },
                leadingContent = { Icon(Icons.Default.History, null) },
                supportingContent = {
                    val historyCount by LocalHistoryDao.current
                        .getAllRecentHistoryCount()
                        .collectAsStateWithLifecycle(0)
                    Text(historyCount.toString())
                },
                onClick = navActions::history,
            )
            segmentedListItem(
                content = { Text("Bookmarks") },
                leadingContent = { Icon(Icons.Default.Bookmark, null) },
                onClick = navActions::bookmarks,
            )
            segmentedListItem(
                content = { Text("Notes") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                onClick = navActions::notes,
            )
            segmentedListItem(
                content = { Text("Custom Lists") },
                leadingContent = { Icon(Icons.AutoMirrored.Default.List, null) },
                onClick = navActions::customList,
            )
            segmentedListItem(
                content = { Text("Saved Notifications") },
                leadingContent = { Icon(Icons.Default.Notifications, null) },
                onClick = navActions::notifications,
            )
            apply(composeSettingsDsl.librarySettings)
        }
    }
}
