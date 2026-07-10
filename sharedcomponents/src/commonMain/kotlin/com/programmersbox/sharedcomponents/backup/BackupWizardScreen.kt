package com.programmersbox.sharedcomponents.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BackupWizardScreen(
    onDone: () -> Unit,
    viewModel: BackupWizardViewModel<PlatformFile> = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
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
                    TextButton(onClick = {
                        if (state.items.all { it.selected }) viewModel.deselectAll() else viewModel.selectAll()
                    }) { Text(if (state.items.all { it.selected }) "Deselect All" else "Select All") }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.items, key = { it.uiInfo.key }) { item ->
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
                            WizardItemRow(item = item.copy(expanded = true), onToggleSelected = {}, onToggleExpanded = {})
                        }
                    }
                    Button(
                        onClick = { saveLauncher.launch("backup", "zip") },
                        enabled = backupRestoreSupported,
                        modifier = Modifier.padding(16.dp),
                    ) { Text(if (backupRestoreSupported) "Confirm Backup" else "Not supported on this platform yet") }
                }

                BackupWizardStep.Executing -> {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Backing up… (${state.results.size}/${state.items.size} done)")
                    }
                }

                BackupWizardStep.Complete -> {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Backup complete")
                        state.results.forEach { result ->
                            Text(if (result.success) "✓ ${result.key}" else "✗ ${result.key}: ${result.error}")
                        }
                        Button(onClick = onDone, modifier = Modifier.padding(top = 16.dp)) { Text("Done") }
                    }
                }
            }
        }
    }
}
