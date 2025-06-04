package com.programmersbox.kmpuiviews.presentation.settings.moresettings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.sonner.ToastType
import com.dokar.sonner.rememberToasterState
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.categorySetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.ToasterSetup
import com.programmersbox.kmpuiviews.utils.ToasterUtils
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.custom_lists_title
import otakuworld.kmpuiviews.generated.resources.export_favorites
import otakuworld.kmpuiviews.generated.resources.import_favorites
import otakuworld.kmpuiviews.generated.resources.more_settings
import otakuworld.kmpuiviews.generated.resources.viewFavoritesMenu
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSettingsScreen(
    viewModel: MoreSettingsViewModel = koinViewModel(),
) {
    val navController = LocalNavActions.current
    val toaster = rememberToasterState(
        onToastDismissed = { viewModel.importExportListStatus = ImportExportListStatus.Idle }
    )

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.importExportListStatus }
            .onEach {
                toaster.dismiss(ToasterUtils.LOADING_TOAST_ID)
                when (it) {
                    is ImportExportListStatus.Error -> toaster.show(
                        "Error: ${it.throwable.message}",
                        type = ToastType.Error,
                    )

                    ImportExportListStatus.Success -> toaster.show(
                        "Completed!",
                        type = ToastType.Success,
                    )

                    ImportExportListStatus.Loading -> toaster.show(
                        "Working...",
                        id = ToasterUtils.LOADING_TOAST_ID,
                        icon = ToasterUtils.LOADING_TOAST_ID,
                        duration = Duration.INFINITE,
                    )

                    else -> {}
                }
            }
            .launchIn(this)
    }

    val appConfig = koinInject<AppConfig>()
    val appName = appConfig.appName
    val exportLauncher = rememberFileSaverLauncher { document ->
        document?.let { viewModel.writeToFile(it) }
    }

    val importLauncher = rememberFilePickerLauncher(
        type = FileKitType.File("json")
    ) { document -> document?.let { viewModel.importFavorites(it) } }

    SettingsScaffold(
        stringResource(Res.string.more_settings),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CategoryGroup {
            categorySetting(
                settingIcon = {
                    Icon(Icons.Default.Star, null)
                }
            ) { Text(stringResource(Res.string.viewFavoritesMenu)) }

            item {
                PreferenceSetting(
                    settingTitle = { Text(stringResource(Res.string.export_favorites)) },
                    settingIcon = { Icon(Icons.Default.Publish, null) },
                    modifier = Modifier.clickable(
                        enabled = viewModel.importExportListStatus !is ImportExportListStatus.Loading,
                        indication = ripple(),
                        interactionSource = null
                    ) { exportLauncher.launch("${appName}_favorites", "json") }
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text(stringResource(Res.string.import_favorites)) },
                    settingIcon = { Icon(Icons.Default.Download, null) },
                    modifier = Modifier.clickable(
                        enabled = viewModel.importExportListStatus !is ImportExportListStatus.Loading,
                        indication = ripple(),
                        interactionSource = null
                    ) { importLauncher.launch() }
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Sync Cloud To Local Favorites") },
                    settingIcon = { Icon(Icons.Default.Sync, null) },
                    summaryValue = {
                        Column {
                            Text("Syncs favorites from the cloud to the local database")
                            Crossfade(
                                viewModel.cloudToLocalSync,
                                label = "Cloud Local Sync"
                            ) { target ->
                                when (target) {
                                    is CloudLocalSync.Error -> Text(target.throwable.message ?: "Unknown Error")
                                    CloudLocalSync.Idle -> {}
                                    CloudLocalSync.Loading -> LinearProgressIndicator()
                                    is CloudLocalSync.Success -> Text("Added ${target.size} favorites")
                                }
                            }
                        }
                    },
                    modifier = Modifier.clickable(
                        enabled = true,
                        indication = ripple(),
                        interactionSource = null,
                        onClick = viewModel::pullCloudToLocal
                    )
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth(.75f)
                        .padding(vertical = 4.dp)
                        .align(Alignment.CenterHorizontally)
                )

                PreferenceSetting(
                    settingTitle = { Text("Sync Local To Cloud Favorites") },
                    settingIcon = { Icon(Icons.Default.CloudSync, null) },
                    summaryValue = {
                        Column {
                            Text("Syncs favorites from the local database to the cloud")
                            Crossfade(
                                viewModel.localToCloudSync,
                                label = "Cloud Local Sync"
                            ) { target ->
                                when (target) {
                                    is CloudLocalSync.Error -> Text(target.throwable.message ?: "Unknown Error")
                                    CloudLocalSync.Idle -> {}
                                    CloudLocalSync.Loading -> LinearProgressIndicator()
                                    is CloudLocalSync.Success -> Text("Added ${target.size} favorites")
                                }
                            }
                        }
                    },
                    modifier = Modifier.clickable(
                        enabled = true,
                        indication = ripple(),
                        interactionSource = null,
                        onClick = viewModel::pullLocalToCloud
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        val exportListLauncher = rememberFileSaverLauncher { document ->
            document?.let { viewModel.writeListsToFile(it) }
        }

        val importListLauncher = rememberFilePickerLauncher(
            type = FileKitType.File("json")
        ) { document ->
            document?.let {
                navController.importFullList(it.toString())
            }
        }

        CategoryGroup {
            categorySetting(
                settingIcon = {
                    Icon(Icons.AutoMirrored.Filled.List, null)
                }
            ) { Text(stringResource(Res.string.custom_lists_title)) }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Export All Lists") },
                    settingIcon = { Icon(Icons.Default.Publish, null) },
                    modifier = Modifier.clickable(
                        enabled = true,
                        indication = ripple(),
                        interactionSource = null
                    ) { exportListLauncher.launch("${appName}_lists", "json") }
                )
            }

            item {
                var showListSelection by remember { mutableStateOf(false) }

                if (showListSelection) {
                    ExportListSelection(
                        onExport = { uri, list ->
                            viewModel.writeListsToFile(uri, list)
                        },
                        onDismiss = { showListSelection = false },
                        list = viewModel
                            .lists
                            .collectAsStateWithLifecycle(emptyList())
                            .value
                    )
                }

                PreferenceSetting(
                    settingTitle = { Text("Export Lists") },
                    settingIcon = { Icon(Icons.Default.Publish, null) },
                    modifier = Modifier.clickable(
                        enabled = true,
                        indication = ripple(),
                        interactionSource = null
                    ) { showListSelection = true }
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Import List") },
                    settingIcon = { Icon(Icons.Default.Download, null) },
                    modifier = Modifier.clickable(
                        enabled = true,
                        indication = ripple(),
                        interactionSource = null
                    ) { importListLauncher.launch() }
                )
            }
        }
    }

    //TODO: Remove toaster and switch back to snackbar
    ToasterSetup(toaster = toaster)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportListSelection(
    onExport: (PlatformFile, List<CustomList>) -> Unit,
    list: List<CustomList>,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(true),
) {
    val appConfig = koinInject<AppConfig>()
    val scope = rememberCoroutineScope()
    val appName = appConfig.appName

    val selection = remember(list) {
        mutableStateMapOf(
            *list
                .map { it to true }
                .toTypedArray()
        )
    }

    val exportListSelectionLauncher = rememberFileSaverLauncher { document ->
        document?.let {
            onExport(it, selection.filterValues { b -> b }.keys.toList())
            scope.launch { sheetState.hide() }
                .invokeOnCompletion { onDismiss() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Select Lists to Export") },
                    windowInsets = WindowInsets(0.dp)
                )
            },
            bottomBar = {
                BottomAppBar {
                    Button(
                        onClick = { exportListSelectionLauncher.launch("${appName}_lists", "json") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Export") }
                }
            },
        ) { padding ->
            LazyColumn(
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                selection.forEach {
                    item {
                        OutlinedCard(
                            onClick = { selection[it.key] = !it.value }
                        ) {
                            ListItem(
                                headlineContent = { Text(it.key.item.name) },
                                trailingContent = { Text(it.key.list.size.toString()) },
                                overlineContent = { Text(it.key.item.uuid.toString()) },
                                leadingContent = {
                                    Checkbox(
                                        checked = it.value,
                                        onCheckedChange = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}