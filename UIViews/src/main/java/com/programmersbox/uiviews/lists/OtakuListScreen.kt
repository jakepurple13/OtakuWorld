package com.programmersbox.uiviews.lists

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalCustomListDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.components.ListBottomScreen
import com.programmersbox.uiviews.utils.components.ListBottomSheetItemModel
import com.programmersbox.uiviews.utils.getSystemDateTimeFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtakuListScreen(
    listDao: ListDao = LocalCustomListDao.current,
    vm: OtakuListViewModel = viewModel { OtakuListViewModel(listDao) }
) {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val dao = LocalCustomListDao.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { document -> document?.let { Screen.ImportListScreen.navigate(navController, it) } }

    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.create_new_list)) },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.list_name)) },
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
                ) { Text(stringResource(id = R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(stringResource(id = R.string.cancel)) } }
        )
    }

    OtakuScaffold(
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(stringResource(R.string.custom_lists_title)) },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(
                        onClick = { pickDocumentLauncher.launch(arrayOf("application/json")) }
                    ) { Icon(Icons.Default.FileDownload, null) }

                    IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null) }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(vm.customLists) {
                ElevatedCard(
                    onClick = { Screen.CustomListItemScreen.navigate(navController, it.item.uuid) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    val time = remember { context.getSystemDateTimeFormat().format(it.item.time) }
                    ListItem(
                        overlineContent = { Text(stringResource(id = R.string.custom_list_updated_at, time)) },
                        trailingContent = { Text("(${it.list.size})") },
                        headlineContent = { Text(it.item.name) },
                        supportingContent = {
                            Column {
                                it.list.take(3).forEach { info ->
                                    Text(info.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    )
                }
                Divider(Modifier.padding(top = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListChoiceScreen(
    onClick: (CustomList) -> Unit
) {
    val dao = LocalCustomListDao.current
    val scope = rememberCoroutineScope()
    val list by dao.getAllLists().collectAsState(initial = emptyList())
    ListBottomScreen(
        title = stringResource(R.string.choose_list_title),
        list = list,
        onClick = onClick,
        lazyListContent = {
            item {
                var showAdd by remember { mutableStateOf(false) }
                ElevatedCard(
                    onClick = { showAdd = !showAdd }
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.create_new_list_option), style = MaterialTheme.typography.titleLarge) },
                        trailingContent = { Icon(Icons.Default.Add, null) }
                    )
                }
                if (showAdd) {
                    var name by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showAdd = false },
                        title = { Text(stringResource(R.string.create_new_list)) },
                        text = {
                            TextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(stringResource(id = R.string.list_name)) },
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
                            ) { Text(stringResource(id = R.string.confirm)) }
                        },
                        dismissButton = { TextButton(onClick = { showAdd = false }) { Text(stringResource(id = R.string.cancel)) } }
                    )
                }
            }
        },
        itemContent = {
            ListBottomSheetItemModel(
                primaryText = it.item.name,
                trailingText = "(${it.list.size})"
            )
        }
    )
}
