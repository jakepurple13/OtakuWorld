package com.programmersbox.supabaseintegration.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class SyncConfig(
    val pollIntervalMs: Long = 5 * 60 * 1000L,
    val maxRetries: Int = 5,
    val initialBackoffMs: Long = 1_000L,
    val maxBackoffMs: Long = 30_000L,
)

class SyncConfigRepository(
    private val syncConfigStore: SyncConfigDataStore,
) {
    fun listenForChanges() = combine(
        syncConfigStore.pollIntervalMs,
        syncConfigStore.maxRetries,
        syncConfigStore.initialBackoffMs,
        syncConfigStore.maxBackoffMs
    ) { pollIntervalMs, maxRetries, initialBackoffMs, maxBackoffMs ->
        SyncConfig(
            pollIntervalMs = pollIntervalMs,
            maxRetries = maxRetries,
            initialBackoffMs = initialBackoffMs,
            maxBackoffMs = maxBackoffMs
        )
    }

    suspend fun updatePollIntervalMs(pollIntervalMs: Long) = syncConfigStore.setPollIntervalMs(pollIntervalMs)
    suspend fun updateMaxRetries(maxRetries: Int) = syncConfigStore.setMaxRetries(maxRetries)
    suspend fun updateInitialBackoffMs(initialBackoffMs: Long) = syncConfigStore.setInitialBackoffMs(initialBackoffMs)
    suspend fun updateMaxBackoffMs(maxBackoffMs: Long) = syncConfigStore.setMaxBackoffMs(maxBackoffMs)
}

data class SyncConfigDataStore(
    val pollIntervalMs: Flow<Long>,
    val maxRetries: Flow<Int>,
    val initialBackoffMs: Flow<Long>,
    val maxBackoffMs: Flow<Long>,
    val setPollIntervalMs: suspend (Long) -> Unit,
    val setMaxRetries: suspend (Int) -> Unit,
    val setInitialBackoffMs: suspend (Long) -> Unit,
    val setMaxBackoffMs: suspend (Long) -> Unit,
)