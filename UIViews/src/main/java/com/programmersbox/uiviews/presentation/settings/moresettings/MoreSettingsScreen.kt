package com.programmersbox.uiviews.presentation.settings.moresettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.sonner.ToastType
import com.dokar.sonner.rememberToasterState
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.CategorySetting
import com.programmersbox.kmpuiviews.presentation.components.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.moresettings.ImportExportListStatus
import com.programmersbox.kmpuiviews.presentation.settings.moresettings.MoreSettingsViewModel
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.ToasterSetup
import com.programmersbox.uiviews.utils.ToasterUtils
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSettingsScreen(
    viewModel: MoreSettingsViewModel = koinViewModel(),
) {
    val navController = LocalNavController.current
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

    val appName = stringResource(id = R.string.app_name)
    val exportLauncher = rememberFileSaverLauncher { document ->
        document?.let { viewModel.writeToFile(it) }
    }

    val importLauncher = rememberFilePickerLauncher(
        type = FileKitType.File("json")
    ) { document -> document?.let { viewModel.importFavorites(it) } }

    SettingsScaffold(stringResource(R.string.more_settings)) {
        CategorySetting(
            settingIcon = {
                Icon(Icons.Default.Star, null)
            }
        ) { Text(stringResource(R.string.viewFavoritesMenu)) }

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.export_favorites)) },
            modifier = Modifier.clickable(
                enabled = viewModel.importExportListStatus !is ImportExportListStatus.Loading,
                indication = ripple(),
                interactionSource = null
            ) { exportLauncher.launch("${appName}_favorites", "json") }
        )

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.import_favorites)) },
            modifier = Modifier.clickable(
                enabled = viewModel.importExportListStatus !is ImportExportListStatus.Loading,
                indication = ripple(),
                interactionSource = null
            ) { importLauncher.launch() }
        )

        HorizontalDivider()

        CategorySetting(
            settingIcon = {
                Icon(Icons.AutoMirrored.Filled.List, null)
            }
        ) { Text(stringResource(R.string.custom_lists_title)) }

        val exportListLauncher = rememberFileSaverLauncher { document ->
            document?.let { viewModel.writeListsToFile(it) }
        }

        val importListLauncher = rememberFilePickerLauncher(
            type = FileKitType.File("json")
        ) { document ->
            document?.let {
                navController.navigate(Screen.ImportFullListScreen(it.toString()))
            }
        }

        PreferenceSetting(
            settingTitle = { Text("Export All Lists") },
            modifier = Modifier.clickable(
                enabled = true,
                indication = ripple(),
                interactionSource = null
            ) { exportListLauncher.launch("${appName}_lists.json") }
        )

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
            modifier = Modifier.clickable(
                enabled = true,
                indication = ripple(),
                interactionSource = null
            ) { showListSelection = true }
        )

        PreferenceSetting(
            settingTitle = { Text("Import List") },
            modifier = Modifier.clickable(
                enabled = true,
                indication = ripple(),
                interactionSource = null
            ) { importListLauncher.launch() }
        )

        HorizontalDivider()
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
    val scope = rememberCoroutineScope()
    val appName = stringResource(id = R.string.app_name)

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
                InsetSmallTopAppBar(
                    title = { Text("Select Lists to Export") },
                    insetPadding = WindowInsets(0.dp)
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