package com.programmersbox.supabaseintegration.sync

import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SyncManager(
    private val syncEngine: SyncEngine,
    private val authManager: AuthManager,
    private val connectivityMonitor: ConnectivityMonitor,
    private val config: SyncConfig = SyncConfig(),
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var realtimeJob: Job? = null
    private var pollingJob: Job? = null

    fun start() {
        scope.launch {
            combine(authManager.authState, connectivityMonitor.isOnline) { auth, online -> auth to online }
                .collect { (auth, online) ->
                    when {
                        auth is AuthState.Authenticated && online -> {
                            stopPolling()
                            startInitialSync()
                        }
                        auth is AuthState.Authenticated && !online -> {
                            stopRealtime()
                            startPolling()
                            _syncState.value = SyncState.Offline
                        }
                        else -> {
                            stopRealtime()
                            stopPolling()
                            _syncState.value = SyncState.Idle
                        }
                    }
                }
        }
    }

    private fun startInitialSync() {
        realtimeJob?.cancel()
        realtimeJob = scope.launch {
            try {
                withRetry(config) {
                    _syncState.value = SyncState.Syncing()
                    syncEngine.fullSync()
                    _syncState.value = SyncState.Idle
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            }
        }
    }

    private fun stopRealtime() { realtimeJob?.cancel() }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                delay(config.pollIntervalMs)
                if (connectivityMonitor.isOnline.value) {
                    withRetry(config) {
                        _syncState.value = SyncState.Syncing()
                        syncEngine.fullSync()
                        _syncState.value = SyncState.Idle
                    }
                }
            }
        }
    }

    private fun stopPolling() { pollingJob?.cancel() }

    suspend fun triggerSync() {
        withRetry(config) {
            _syncState.value = SyncState.Syncing()
            syncEngine.fullSync()
        }
        _syncState.value = if (connectivityMonitor.isOnline.value) SyncState.Idle else SyncState.Offline
    }

    fun stop() { scope.cancel() }
}

private suspend fun withRetry(config: SyncConfig, block: suspend () -> Unit) {
    var attempt = 1
    var backoff = config.initialBackoffMs
    while (attempt <= config.maxRetries) {
        runCatching { block() }
            .onSuccess { return }
            .onFailure { e ->
                attempt++
                if (attempt > config.maxRetries) throw e
                delay(backoff)
                backoff = minOf(backoff * 2, config.maxBackoffMs)
            }
    }
}
