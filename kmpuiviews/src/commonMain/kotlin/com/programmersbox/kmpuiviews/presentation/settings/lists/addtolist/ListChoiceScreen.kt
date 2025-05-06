package com.programmersbox.kmpuiviews.presentation.settings.lists.addtolist

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.kmpuiviews.utils.LocalCustomListDao
import com.programmersbox.kmpuiviews.utils.LocalNavController
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.choose_list_title
import otakuworld.kmpuiviews.generated.resources.confirm
import otakuworld.kmpuiviews.generated.resources.create_new_list
import otakuworld.kmpuiviews.generated.resources.create_new_list_option
import otakuworld.kmpuiviews.generated.resources.list_name

@Composable
fun ListChoiceScreen(
    url: String? = null,
    navigationIcon: @Composable () -> Unit = {
        val navController = LocalNavController.current
        IconButton(
            onClick = { navController.popBackStack() }
        ) { Icon(Icons.Default.Close, null) }
    },
    onClick: (CustomList) -> Unit,
) {
    val dao = LocalCustomListDao.current
    val scope = rememberCoroutineScope()
    val list by dao
        .getAllLists()
        .collectAsStateWithLifecycle(emptyList())
    ListBottomScreen(
        title = stringResource(Res.string.choose_list_title),
        list = list,
        navigationIcon = navigationIcon,
        onClick = onClick,
        lazyListContent = {
            item {
                var showAdd by remember { mutableStateOf(false) }
                ElevatedCard(
                    onClick = { showAdd = !showAdd }
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(Res.string.create_new_list_option),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        trailingContent = { Icon(Icons.Default.Add, null) }
                    )
                }
                if (showAdd) {
                    var name by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showAdd = false },
                        title = { Text(stringResource(Res.string.create_new_list)) },
                        text = {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(stringResource(Res.string.list_name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        dao.create(name)
                                        showAdd = false
                                    }
                                },
                                enabled = name.isNotEmpty()
                            ) { Text(stringResource(Res.string.confirm)) }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showAdd = false }
                            ) { Text(stringResource(Res.string.cancel)) }
                        }
                    )
                }
            }
        },
        itemContent = {
            ListBottomSheetItemModel(
                primaryText = it.item.name,
                trailingText = "(${it.list.size})",
                icon = it.list.find { l -> l.url == url }?.let { Icons.Default.Check }
            )
        }
    )
}