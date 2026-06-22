package com.programmersbox.supabaseintegration.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface SyncEngine {
    suspend fun pushLocalChanges()
    suspend fun pullRemoteChanges(since: Long)
    suspend fun fullSync()
    /**
     * Opens a Supabase Realtime channel that watches all 8 sync tables for the current user.
     * Calls [onEvent] (coalesced) whenever any row changes. Caller controls lifetime via the
     * returned [Job] — cancelling the job tears down the channel.
     */
    fun subscribeRealtime(scope: CoroutineScope, onEvent: suspend () -> Unit): Job
}
