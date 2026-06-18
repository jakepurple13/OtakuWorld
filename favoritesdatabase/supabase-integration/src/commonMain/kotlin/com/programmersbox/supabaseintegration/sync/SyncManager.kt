package com.programmersbox.supabaseintegration.sync

import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SyncManager(
    private val syncEngine: SyncEngine,
    private val authManager: AuthManager,
    private val connectivityMonitor: ConnectivityMonitor,
    configFlow: Flow<SyncConfig> = flowOf(SyncConfig()),
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val config = configFlow.stateIn(scope, SharingStarted.Eagerly, SyncConfig())
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var realtimeJob: Job? = null
    private var pollingJob: Job? = null

    fun start() {
        scope.launch {
            combine(authManager.authState, connectivityMonitor.isOnline) { auth, online -> auth to online }
                .collect { (auth, online) ->
                    when (auth) {
                        is AuthState.Authenticated if online -> {
                            stopPolling()
                            startInitialSync()
                        }

                        is AuthState.Authenticated if !online -> {
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
            // Immediate sync on connect, then keep polling while online+authenticated.
            // Job is cancelled by stopRealtime() when auth/connectivity changes.
            while (isActive) {
                try {
                    withRetry {
                        _syncState.value = SyncState.Syncing()
                        syncEngine.fullSync()
                        _syncState.value = SyncState.Idle
                    }
                } catch (e: Exception) {
                    _syncState.value = SyncState.Error(e.message ?: "Sync failed")
                }
                delay(config.value.pollIntervalMs)
            }
        }
    }

    private fun stopRealtime() {
        realtimeJob?.cancel()
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                delay(config.value.pollIntervalMs)
                if (connectivityMonitor.isOnline.value) {
                    try {
                        withRetry {
                            _syncState.value = SyncState.Syncing()
                            syncEngine.fullSync()
                            _syncState.value = SyncState.Idle
                        }
                    } catch (e: Exception) {
                        _syncState.value = SyncState.Error(e.message ?: "Sync failed")
                    }
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }

    suspend fun triggerSync() {
        withRetry {
            _syncState.value = SyncState.Syncing()
            syncEngine.fullSync()
        }
        _syncState.value = if (connectivityMonitor.isOnline.value) SyncState.Idle else SyncState.Offline
    }

    fun stop() {
        scope.cancel()
    }

    private suspend fun withRetry(block: suspend () -> Unit) {
        val cfg = config.value
        var attempt = 1
        var backoff = cfg.initialBackoffMs
        while (attempt <= cfg.maxRetries) {
            runCatching { block() }
                .onSuccess { return }
                .onFailure { e ->
                    attempt++
                    if (attempt > cfg.maxRetries) throw e
                    delay(backoff)
                    backoff = minOf(backoff * 2, cfg.maxBackoffMs)
                }
        }
    }
}
