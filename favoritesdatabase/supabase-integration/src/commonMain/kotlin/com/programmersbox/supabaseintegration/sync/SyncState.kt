package com.programmersbox.supabaseintegration.sync

sealed class SyncState {
    data object Idle : SyncState()
    data class Syncing(val entity: String? = null) : SyncState()
    data class Error(val message: String) : SyncState()
    data object Offline : SyncState()
}


sealed class SyncConnectedStatus {
    data object Realtime : SyncConnectedStatus()
    data object Polling : SyncConnectedStatus()
    data object Offline : SyncConnectedStatus()
    data object Idle : SyncConnectedStatus()
}