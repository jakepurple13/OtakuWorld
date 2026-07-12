package com.programmersbox.kmpuiviews.presentation.settings.backuprestore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BackupWizardScreen(
    onDone: () -> Unit,
    viewModel: BackupWizardViewModel<PlatformFile> = koinViewModel(),
) {
    HideNavBarWhileOnScreen()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stepLabels = listOf("Select", "Review", "Backup", "Done")
    val currentIndex = when (state.step) {
        BackupWizardStep.SelectItems -> 0
        BackupWizardStep.Review -> 1
        BackupWizardStep.Executing -> 2
        BackupWizardStep.Complete -> 3
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            WizardStepper(steps = stepLabels, currentIndex = currentIndex, modifier = Modifier.padding(16.dp))

            when (state.step) {
                BackupWizardStep.SelectItems -> {
                    TextButton(
                        onClick = {
                            if (state.items.all { it.selected }) viewModel.deselectAll() else viewModel.selectAll()
                        }
                    ) { Text(if (state.items.all { it.selected }) "Deselect All" else "Select All") }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = state.items,
                            key = { it.uiInfo.key }
                        ) { item ->
                            WizardItemRow(
                                item = item,
                                onToggleSelected = { viewModel.toggleSelected(item.uiInfo.key) },
                                onToggleExpanded = { viewModel.toggleExpanded(item.uiInfo.key) },
                            )
                        }
                    }


                    Button(
                        onClick = viewModel::goToReview,
                        modifier = Modifier.padding(16.dp),
                    ) { Text("Next: Review") }
                }

                BackupWizardStep.Review -> {
                    val saveLauncher = rememberFileSaverLauncher(
                        dialogSettings = FileKitDialogSettings.createDefault()
                    ) { document -> document?.let { viewModel.confirm(it) } }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.items, key = { it.uiInfo.key }) { item ->
                            WizardItemRow(
                                item = item.copy(expanded = true),
                                onToggleSelected = {},
                                onToggleExpanded = {}
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Button(
                            onClick = viewModel::goToSelectItems,
                        ) { Text("Previous: Select Items") }

                        Button(
                            onClick = { saveLauncher.launch(suggestedName = "backup", defaultExtension = "zip") },
                        ) { Text("Confirm Backup") }
                    }
                }

                BackupWizardStep.Executing -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Backing up… (${state.results.size}/${state.items.size} done)")
                        state.results.forEach { result ->
                            ListItem(
                                headlineContent = { Text(if (result.success) "✓ ${result.key}" else "✗ ${result.key}: ${result.error}") },
                                supportingContent = { Text(result.timeTaken) }
                            )
                        }
                    }
                }

                BackupWizardStep.Complete -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Backup complete")
                        state.results.forEach { result ->
                            ListItem(
                                headlineContent = { Text(if (result.success) "✓ ${result.key}" else "✗ ${result.key}: ${result.error}") },
                                supportingContent = { Text(result.timeTaken) }
                            )
                        }
                        Button(onClick = onDone, modifier = Modifier.padding(top = 16.dp)) { Text("Done") }
                    }
                }
            }
        }
    }
}
