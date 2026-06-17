package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.supabaseintegration.sync.SyncManager
import com.programmersbox.supabaseintegration.sync.SyncState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SyncViewModel(private val syncManager: SyncManager) : ViewModel() {
    val syncState: StateFlow<SyncState> = syncManager.syncState
    fun triggerSync() { viewModelScope.launch { syncManager.triggerSync() } }
    override fun onCleared() { syncManager.stop() }
}
