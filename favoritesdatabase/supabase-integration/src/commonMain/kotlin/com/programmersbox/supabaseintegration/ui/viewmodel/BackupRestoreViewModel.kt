package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.supabaseintegration.backup.BackupEntry
import com.programmersbox.supabaseintegration.backup.BackupManager
import com.programmersbox.supabaseintegration.backup.RestoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BackupRestoreViewModel(
    private val backupManager: BackupManager,
    private val restoreManager: RestoreManager,
) : ViewModel() {
    private val _backups = MutableStateFlow<List<BackupEntry>>(emptyList())
    val backups: StateFlow<List<BackupEntry>> = _backups
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status
    val uploadProgress = backupManager.getUploadProgress()
    val downloadProgress = restoreManager.getDownloadProgress()

    fun loadBackups() {
        viewModelScope.launch {
            runCatching { _backups.value = restoreManager.listBackups() }
                .onFailure { _status.value = "Failed to load backups: ${it.message}" }
        }
    }

    fun uploadBackup(filePath: String) {
        viewModelScope.launch {
            backupManager.uploadBackup(filePath)
                .onSuccess { _status.value = "Backup uploaded: $it" }
                .onFailure { _status.value = "Upload failed: ${it.message}" }
            loadBackups()
        }
    }

    fun downloadBackup(entry: BackupEntry, localPath: String) {
        viewModelScope.launch {
            restoreManager.downloadBackup(entry, localPath)
                .onSuccess { _status.value = "Downloaded to: $it" }
                .onFailure { _status.value = "Download failed: ${it.message}" }
        }
    }
}
