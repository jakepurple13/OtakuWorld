package com.programmersbox.supabaseintegration.sync

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val entity: String? = null) : SyncState()
    data class Error(val message: String) : SyncState()
    object Offline : SyncState()
}
