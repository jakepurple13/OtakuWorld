package com.programmersbox.supabaseintegration.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface SyncEngine {
    suspend fun pushLocalChanges()
    /** Pull remote changes. [tables] restricts which tables are fetched; null = all tables. */
    suspend fun pullRemoteChanges(since: Long, tables: Set<String>? = null)
    suspend fun fullSync()
    /**
     * Opens a Supabase Realtime channel that watches all 8 sync tables for the current user.
     * Calls [onEvent] with the set of table names that changed (coalesced per batch).
     * Caller controls lifetime via the returned [Job] — cancelling the job tears down the channel.
     */
    fun subscribeRealtime(scope: CoroutineScope, onEvent: suspend (Set<String>) -> Unit): Job
}
