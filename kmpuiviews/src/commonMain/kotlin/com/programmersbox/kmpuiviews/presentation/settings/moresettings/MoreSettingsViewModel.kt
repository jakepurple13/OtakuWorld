package com.programmersbox.kmpuiviews.presentation.settings.moresettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import io.github.vinceglb.filekit.PlatformFile

class MoreSettingsViewModel(
    private val backgroundWorkHandler: BackgroundWorkHandler,
) : ViewModel() {

    var importExportListStatus: ImportExportListStatus by mutableStateOf(ImportExportListStatus.Idle)

    fun exportFullBackup(document: PlatformFile) {
        backgroundWorkHandler.startBackup(document)
    }

    fun importFullBackup(document: PlatformFile) {
        backgroundWorkHandler.startRestore(document)
    }
}

sealed class ImportExportListStatus {
    data object Idle : ImportExportListStatus()
    data object Loading : ImportExportListStatus()
    class Error(val throwable: Throwable) : ImportExportListStatus()
    data object Success : ImportExportListStatus()
}
