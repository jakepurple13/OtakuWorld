package com.programmersbox.supabaseintegration.sync

interface SyncEngine {
    suspend fun pushLocalChanges()
    suspend fun pullRemoteChanges(since: Long)
    suspend fun fullSync()
}
