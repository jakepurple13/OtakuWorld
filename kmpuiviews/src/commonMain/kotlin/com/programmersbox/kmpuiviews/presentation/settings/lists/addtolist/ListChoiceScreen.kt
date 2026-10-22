package com.programmersbox.kmpuiviews.presentation.settings.lists.addtolist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.add_to_lists_button
import otakuworld.kmpuiviews.generated.resources.add_to_lists_title
import otakuworld.kmpuiviews.generated.resources.added_to_lists
import otakuworld.kmpuiviews.generated.resources.added_to_n_lists
import otakuworld.kmpuiviews.generated.resources.already_in_lists
import otakuworld.kmpuiviews.generated.resources.already_in_this_list
import otakuworld.kmpuiviews.generated.resources.create
import otakuworld.kmpuiviews.generated.resources.current_list
import otakuworld.kmpuiviews.generated.resources.list_name
import otakuworld.kmpuiviews.generated.resources.lists_selected
import otakuworld.kmpuiviews.generated.resources.new_list
import otakuworld.kmpuiviews.generated.resources.pick_lists_hint
import otakuworld.kmpuiviews.generated.resources.some_already_here
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Multi-select sheet content for adding [urls] to one or more lists.
 *
 * Lists that already hold every url, and the [currentListUuid] list, are shown disabled.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalUuidApi::class)
@Composable
fun ListChoiceScreen(
    urls: Set<String>,
    subtitle: String,
    currentListUuid: String? = null,
    navigationIcon: @Composable () -> Unit = {
        val navController = LocalNavActions.current
        IconButton(
            onClick = { navController.popBackStack() }
        ) { Icon(Icons.Default.Close, null) }
    },
    onConfirm: (List<CustomList>) -> Unit,
) {
    val listRepository = koinInject<ListRepository>()
    val scope = rememberCoroutineScope()
    val lists by listRepository
        .getAllLists()
        .collectAsStateWithLifecycle(emptyList())
    val picked: SnapshotStateSet<String> = remember { SnapshotStateSet() }

    Column(modifier = Modifier.navigationBarsPadding()) {
        TopAppBar(
            windowInsets = WindowInsets.statusBars,
            title = {
                Column {
                    Text(stringResource(Res.string.add_to_lists_title))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = navigationIcon,
        )
        HorizontalDivider()

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            item {
                var showAdd by remember { mutableStateOf(false) }
                if (showAdd) {
                    var name by remember { mutableStateOf("") }
                    val focusRequester = remember { FocusRequester() }
                    val create = {
                        scope.launch {
                            val uuid = Uuid.random().toString()
                            listRepository.createList(CustomListItem(uuid = uuid, name = name.trim()))
                            picked.add(uuid)
                            showAdd = false
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(Res.string.list_name)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                        )
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        Button(
                            onClick = { create() },
                            enabled = name.isNotBlank()
                        ) { Text(stringResource(Res.string.create)) }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showAdd = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Text(stringResource(Res.string.new_list))
                    }
                }
            }

            items(lists, key = { it.item.uuid }) { list ->
                val alreadyHere = urls.count { url -> list.list.any { it.url == url } }
                val isCurrent = list.item.uuid == currentListUuid
                val full = alreadyHere == urls.size
                val enabled = !isCurrent && !full
                val checked = full || list.item.uuid in picked
                ListItem(
                    checked = checked,
                    onCheckedChange = { if (it) picked.add(list.item.uuid) else picked.remove(list.item.uuid) },
                    enabled = enabled,
                    leadingContent = { Checkbox(checked = checked && !isCurrent, onCheckedChange = null, enabled = enabled) },
                    supportingContent = when {
                        isCurrent -> ({ Text(stringResource(Res.string.current_list)) })
                        full -> ({ Text(stringResource(Res.string.already_in_this_list)) })
                        alreadyHere > 0 -> ({ Text(stringResource(Res.string.some_already_here, alreadyHere, urls.size)) })
                        else -> null
                    },
                    trailingContent = { Text("${list.list.size}") },
                    content = { Text(list.item.name) },
                )
            }
        }

        HorizontalDivider()
        val pickedLists = lists.filter { it.item.uuid in picked }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                if (pickedLists.isEmpty()) {
                    stringResource(Res.string.pick_lists_hint)
                } else {
                    pluralStringResource(Res.plurals.lists_selected, pickedLists.size, pickedLists.size)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onConfirm(pickedLists) },
                enabled = pickedLists.isNotEmpty()
            ) {
                Text(
                    if (pickedLists.isEmpty()) {
                        stringResource(Res.string.add_to_lists_title)
                    } else {
                        pluralStringResource(Res.plurals.add_to_lists_button, pickedLists.size, pickedLists.size)
                    }
                )
            }
        }
    }
}

/** Snackbar text for the lists something was newly added to. */
suspend fun addedToListsMessage(added: List<CustomList>): String = when {
    added.isEmpty() -> getString(Res.string.already_in_lists)
    added.size <= 2 -> getString(Res.string.added_to_lists, added.joinToString { it.item.name })
    else -> getPluralString(Res.plurals.added_to_n_lists, added.size, added.size)
}
