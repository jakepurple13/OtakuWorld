package com.programmersbox.kmpuiviews.presentation.settings.moresettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.sharedtools.BackupProcessor
import io.github.vinceglb.filekit.PlatformFile

class MoreSettingsViewModel(
    private val backgroundWorkHandler: BackgroundWorkHandler,
    private val backupProcessors: List<BackupProcessor>,
) : ViewModel() {

    var importExportListStatus: ImportExportListStatus by mutableStateOf(ImportExportListStatus.Idle)

    fun exportFullBackup(document: PlatformFile) {
        backgroundWorkHandler.startBackup(document, backupProcessors.map { it.fileName }.toSet())
    }

    fun importFullBackup(document: PlatformFile) {
        backgroundWorkHandler.startRestore(document, backupProcessors.map { it.fileName }.toSet())
    }
}

sealed class ImportExportListStatus {
    data object Idle : ImportExportListStatus()
    data object Loading : ImportExportListStatus()
    class Error(val throwable: Throwable) : ImportExportListStatus()
    data object Success : ImportExportListStatus()
}
