package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.LocalNavController
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.global_search

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SourceNotInstalledModal(
    showItem: String?,
    onShowItemDismiss: (String?) -> Unit,
    source: String?,
    additionOptions: @Composable ColumnScope.() -> Unit = {},
) {
    val navController = LocalNavController.current

    if (showItem != null) {
        BackHandler { onShowItemDismiss(null) }

        ModalBottomSheet(
            onDismissRequest = { onShowItemDismiss(null) },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Text(
                source.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            CenterAlignedTopAppBar(title = { Text(showItem) })
            ListItem(
                headlineContent = { Text(stringResource(Res.string.global_search)) },
                leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.clickable {
                    onShowItemDismiss(null)
                    navController.navigate(Screen.GlobalSearchScreen(showItem))
                }
            )
            additionOptions()
        }
    }
}