package com.programmersbox.kmpuiviews.presentation.settings.lists

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.optionsSheet
import com.programmersbox.kmpuiviews.presentation.components.thenIf
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalCustomListDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.printLogs
import com.programmersbox.kmpuiviews.utils.rememberBiometricPrompting
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.are_you_sure_delete_list
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.confirm
import otakuworld.kmpuiviews.generated.resources.create_new_list
import otakuworld.kmpuiviews.generated.resources.custom_list_updated_at
import otakuworld.kmpuiviews.generated.resources.custom_lists_title
import otakuworld.kmpuiviews.generated.resources.delete_list_title
import otakuworld.kmpuiviews.generated.resources.list_name
import otakuworld.kmpuiviews.generated.resources.update_list_name_title

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

    val biometricPrompting = rememberBiometricPrompting()

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
                var optionsSheet by optionsSheet {
                    OutlinedCard(
                        modifier = Modifier.animateContentSize()
                    ) {

                        var currentName by remember { mutableStateOf(it.item.name) }

                        var showAdd by remember { mutableStateOf(false) }

                        if (showAdd) {
                            AlertDialog(
                                onDismissRequest = { showAdd = false },
                                title = { Text(stringResource(Res.string.update_list_name_title)) },
                                text = { Text("Are you sure you want to change the name?") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            scope.launch { it.item.copy(name = currentName).let { dao.updateFullList(it) } }
                                            showAdd = false
                                        }
                                    ) { Text(stringResource(Res.string.confirm)) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAdd = false }) { Text(stringResource(Res.string.cancel)) }
                                }
                            )
                        }

                        OutlinedCard {
                            ListItem(
                                headlineContent = {
                                    OutlinedTextField(
                                        currentName,
                                        onValueChange = { currentName = it },
                                        shape = MaterialTheme.shapes.large,
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { showAdd = true },
                                                enabled = currentName != it.item.name
                                            ) { Icon(Icons.Default.Check, null) }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                leadingContent = it
                                    .list
                                    .firstOrNull()
                                    ?.let { image ->
                                        {
                                            ImageLoaderChoice(
                                                imageUrl = image.imageUrl,
                                                name = it.item.name,
                                                placeHolder = { painterLogo() },
                                                modifier = Modifier
                                                    .size(
                                                        width = ComposableUtils.IMAGE_WIDTH,
                                                        height = ComposableUtils.IMAGE_HEIGHT
                                                    )
                                                    .clip(MaterialTheme.shapes.medium)
                                            )
                                        }
                                    },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }

                        OptionsItem(
                            "Open",
                            onClick = { navigateDetail(it) }
                        )

                        fun updateBiometric(value: Boolean) {
                            biometricPrompting.authenticate(
                                title = "Authentication required",
                                subtitle = "In order to change biometric setting for ${it.item.name}, please authenticate",
                                negativeButtonText = "Never Mind",
                                onAuthenticationSucceeded = {
                                    scope
                                        .launch { dao.updateBiometric(it.item.uuid, value) }
                                        .invokeOnCompletion { dismiss() }
                                },
                            )
                        }

                        OptionsItem(
                            "Requires Biometric",
                            onClick = { updateBiometric(!it.item.useBiometric) },
                            trailingContent = {
                                Switch(
                                    checked = it.item.useBiometric,
                                    onCheckedChange = { useBiometric -> updateBiometric(useBiometric) }
                                )
                            }
                        )

                        fun writeToFile(document: PlatformFile) {
                            runCatching {
                                scope.launch {
                                    runCatching {
                                        if (!document.exists()) document.createDirectories()
                                        listOf(it)
                                            .let { Json.encodeToString(it) }
                                            .let { document.writeString(it) }
                                    }.onFailure { it.printStackTrace() }
                                }
                            }
                                .onSuccess { printLogs { "Written!" } }
                                .onFailure { it.printStackTrace() }
                        }

                        val pickDocumentLauncher = rememberFileSaverLauncher { document -> document?.let { writeToFile(it) } }

                        OptionsItem(
                            "Export List",
                            onClick = { pickDocumentLauncher.launch(it.item.name, "json") }
                        )

                        var deleteOption by deleteOption(
                            customItem = it,
                            deleteAll = { dao.removeList(it) },
                            scope = scope
                        )

                        OptionsItem(
                            "Delete List",
                            onClick = { deleteOption = true }
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier
                        .animateItem()
                        .combinedClickable(
                            onClick = { navigateDetail(it) },
                            onLongClick = { optionsSheet = true }
                        )
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
                    //TODO: Also, add a way to allow user to choose the cover
                    // defaults to first item in the list cover
                    // but we could allow user to:
                    // choose a cover from items in list
                    // enter a url themselves
                    // choose an image from device
                    // if the choice is an image from device, exporting will be a problem.
                    // Might use the image loaders ability to show an error image
                    ListItem(
                        overlineContent = { Text(stringResource(Res.string.custom_list_updated_at, time)) },
                        trailingContent = { Text("(${it.list.size})") },
                        headlineContent = { Text(it.item.name) },
                        leadingContent = if (it.item.uuid == AppConfig.forLaterUuid) {
                            { Icon(Icons.Default.WatchLater, null) }
                        } else {
                            it
                                .list
                                .firstOrNull()
                                ?.let { image ->
                                    {
                                        ImageLoaderChoice(
                                            imageUrl = image.imageUrl,
                                            name = it.item.name,
                                            placeHolder = { painterLogo() },
                                            modifier = Modifier
                                                .size(
                                                    width = ComposableUtils.IMAGE_WIDTH,
                                                    height = ComposableUtils.IMAGE_HEIGHT
                                                )
                                                .clip(MaterialTheme.shapes.medium)
                                        )
                                    }
                                }
                        },
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

@Composable
private fun deleteOption(
    customItem: CustomList,
    deleteAll: suspend () -> Unit,
    scope: CoroutineScope,
): MutableState<Boolean> {
    val deleteList = remember { mutableStateOf(false) }

    if (deleteList.value) {
        var listName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { deleteList.value = false },
            title = { Text(stringResource(Res.string.delete_list_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.are_you_sure_delete_list))
                    Text(customItem.item.name)
                    OutlinedTextField(
                        value = listName,
                        onValueChange = { listName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { deleteAll() }
                            deleteList.value = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = listName == customItem.item.name
                ) { Text(stringResource(Res.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleteList.value = false }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    return deleteList
}