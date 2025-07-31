package com.programmersbox.kmpuiviews.presentation.settings.lists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.thenIf
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.LocalCustomListDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.kmpuiviews.utils.rememberBiometricPrompting
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.confirm
import otakuworld.kmpuiviews.generated.resources.create_new_list
import otakuworld.kmpuiviews.generated.resources.custom_list_updated_at
import otakuworld.kmpuiviews.generated.resources.custom_lists_title
import otakuworld.kmpuiviews.generated.resources.list_name

@Composable
fun OtakuListView(
    viewModel: OtakuListViewModel = koinViewModel(),
) {
    val navActions = LocalNavActions.current
    val biometric = rememberBiometricPrompting()
    OtakuListView(
        customLists = viewModel.customLists,
        navigateDetail = {
            if (it.item.useBiometric) {
                biometric.authenticate(
                    title = "Authentication required",
                    subtitle = "In order to view ${it.item.name}, please authenticate",
                    negativeButtonText = "Never Mind",
                    onAuthenticationSucceeded = { navActions.customList(it) },
                    onAuthenticationFailed = {}
                )
            } else {
                navActions.customList(it)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OtakuListView(
    customLists: List<CustomList>,
    customItem: CustomList? = null,
    navigateDetail: (CustomList) -> Unit,
) {
    val navController = LocalNavActions.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val dateTimeFormatter = LocalSystemDateTimeFormat.current
    val dao = LocalCustomListDao.current
    val scope = rememberCoroutineScope()

    val pickDocumentLauncher = rememberFilePickerLauncher(
        type = FileKitType.File("json")
    ) { document -> document?.let { navController.importFullList(it.toString()) } }

    var showAdd by remember { mutableStateOf(false) }

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
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.custom_lists_title)) },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(
                        onClick = { pickDocumentLauncher.launch() }
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
            items(customLists) {
                ElevatedCard(
                    onClick = { navigateDetail(it) },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 4.dp)
                        .thenIf(customItem == it) {
                            border(
                                2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CardDefaults.elevatedShape
                            )
                        }
                ) {
                    val time = remember { dateTimeFormatter.format(it.item.time.toLocalDateTime()) }
                    ListItem(
                        overlineContent = { Text(stringResource(Res.string.custom_list_updated_at, time)) },
                        trailingContent = { Text("(${it.list.size})") },
                        headlineContent = { Text(it.item.name) },
                        leadingContent = if (it.item.uuid == AppConfig.forLaterUuid) {
                            { Icon(Icons.Default.WatchLater, null) }
                        } else null,
                        supportingContent = {
                            if (!it.item.useBiometric) {
                                Column {
                                    it.list.take(3).forEach { info ->
                                        Text(info.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    )
                }
                HorizontalDivider(Modifier.padding(top = 4.dp))
            }
        }
    }
}