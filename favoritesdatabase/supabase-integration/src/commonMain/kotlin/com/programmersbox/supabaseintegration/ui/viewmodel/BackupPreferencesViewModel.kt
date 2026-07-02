package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.syncprocessor.SyncProcessor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BackupPreferenceItem(
    val tableName: String,
    val displayName: String,
    val enabled: Boolean,
)

data class BackupPreferencesUiState(
    val items: List<BackupPreferenceItem> = emptyList(),
    val isLoggedIn: Boolean = false,
)

class BackupPreferencesViewModel(
    private val backupPreferenceRepository: BackupPreferenceRepository,
    syncProcessors: List<SyncProcessor<*, *>>,
    authManager: AuthManager,
) : ViewModel() {

    val uiState: StateFlow<BackupPreferencesUiState> = combine(
        backupPreferenceRepository.observeAllPreferences(),
        authManager.authState,
    ) { preferences, authState ->
        BackupPreferencesUiState(
            items = syncProcessors.map { processor ->
                BackupPreferenceItem(
                    tableName = processor.tableName,
                    displayName = processor.displayName,
                    enabled = preferences[processor.tableName] ?: true,
                )
            },
            isLoggedIn = authState is AuthState.Authenticated,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BackupPreferencesUiState(),
    )

    fun setBackupEnabled(tableName: String, enabled: Boolean) {
        viewModelScope.launch {
            backupPreferenceRepository.setBackupEnabled(tableName, enabled)
        }
    }
}
