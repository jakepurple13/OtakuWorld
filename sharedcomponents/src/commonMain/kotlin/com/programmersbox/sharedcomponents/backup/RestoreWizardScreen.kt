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
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RestoreWizardScreen(
    onDone: () -> Unit,
    viewModel: RestoreWizardViewModel<PlatformFile> = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val stepLabels = listOf("File", "Select", "Review", "Restore", "Done")
    val currentIndex = when (state.step) {
        RestoreWizardStep.PickFile -> 0
        RestoreWizardStep.SelectItems -> 1
        RestoreWizardStep.Review -> 2
        RestoreWizardStep.Executing -> 3
        RestoreWizardStep.Complete -> 4
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            WizardStepper(steps = stepLabels, currentIndex = currentIndex, modifier = Modifier.padding(16.dp))

            when (state.step) {
                RestoreWizardStep.PickFile -> {
                    val pickLauncher = rememberFilePickerLauncher(type = FileKitType.File("zip")) { file ->
                        file?.let { viewModel.pickFile(it) }
                    }
                    Button(onClick = { pickLauncher.launch() }, modifier = Modifier.padding(16.dp)) {
                        Text("Choose Backup File")
                    }
                }

                RestoreWizardStep.SelectItems -> {
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

                    Button(onClick = viewModel::goToReview, modifier = Modifier.padding(16.dp)) { Text("Next: Review") }
                }

                RestoreWizardStep.Review -> {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.items, key = { it.uiInfo.key }) { item ->
                            WizardItemRow(item = item.copy(expanded = true), onToggleSelected = {}, onToggleExpanded = {})
                        }
                    }
                    Button(
                        onClick = viewModel::confirm,
                        enabled = backupRestoreSupported,
                        modifier = Modifier.padding(16.dp),
                    ) { Text(if (backupRestoreSupported) "Confirm Restore" else "Not supported on this platform yet") }
                }

                RestoreWizardStep.Executing -> {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Restoring… (${state.results.size}/${state.items.size} done)")
                    }
                }

                RestoreWizardStep.Complete -> {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("Restore complete")
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
