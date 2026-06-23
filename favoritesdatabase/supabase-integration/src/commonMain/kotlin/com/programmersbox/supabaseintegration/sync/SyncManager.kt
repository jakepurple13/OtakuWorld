@file:OptIn(kotlinx.coroutines.FlowPreview::class)

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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

    val syncConnectedStatus: StateFlow<SyncConnectedStatus>
        field = MutableStateFlow<SyncConnectedStatus>(SyncConnectedStatus.Idle)

    private var realtimeJob: Job? = null
    private var pollingJob: Job? = null

    // 0L → full pull on first sync; updated to now() after each successful sync.
    private var lastSyncTimestamp = 0L

    fun start() {
        scope.launch {
            combine(
                authManager.authState,
                connectivityMonitor.isOnline,
                connectivityMonitor.isMetered,
            ) { auth, online, metered -> Triple(auth, online, metered) }
                .collect { (auth, online, metered) ->
                    when (auth) {
                        is AuthState.Authenticated if online && !metered -> {
                            // WiFi: use Realtime for reactive updates, no polling needed.
                            stopPolling()
                            startWifi()
                            syncConnectedStatus.update { SyncConnectedStatus.Realtime }
                        }

                        is AuthState.Authenticated if online && metered -> {
                            // Cellular: fall back to polling to conserve bandwidth.
                            stopRealtime()
                            startPolling()
                            syncConnectedStatus.update { SyncConnectedStatus.Polling }
                        }

                        is AuthState.Authenticated if !online -> {
                            stopRealtime()
                            stopPolling()
                            _syncState.value = SyncState.Offline
                            syncConnectedStatus.update { SyncConnectedStatus.Offline }
                        }

                        else -> {
                            stopRealtime()
                            stopPolling()
                            lastSyncTimestamp = 0L  // reset so next sign-in does a full pull
                            _syncState.value = SyncState.Idle
                            syncConnectedStatus.update { SyncConnectedStatus.Idle }
                        }
                    }
                }
        }
    }

    private fun startWifi() {
        println("Starting realtime listening")
        realtimeJob?.cancel()
        realtimeJob = scope.launch {
            // Immediate full sync, then hand off to Realtime for incremental updates.
            try {
                withRetry { doSync() }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            }

            // Push local changes reactively: any is_dirty row → debounce 1s → push only.
            launch {
                syncEngine
                    .observeLocalChanges()
                    .debounce(1.seconds)
                    .collect {
                        try {
                            withRetry { syncEngine.pushLocalChanges() }
                        } catch (e: Exception) {
                            _syncState.value = SyncState.Error(e.message ?: "Push failed")
                        }
                    }
            }

            // Realtime subscription — onEvent receives only the tables that changed.
            syncEngine.subscribeRealtime(this) { tables ->
                try {
                    withRetry { doSync(tables) }
                } catch (e: Exception) {
                    _syncState.value = SyncState.Error(e.message ?: "Sync failed")
                }
            }
        }
    }

    private fun stopRealtime() {
        println("Stopping realtime listening")
        realtimeJob?.cancel()
    }

    private fun startPolling() {
        println("Starting polling")
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                delay(config.value.pollIntervalMs.milliseconds)
                if (connectivityMonitor.isOnline.value) {
                    try {
                        withRetry { doSync() }
                    } catch (e: Exception) {
                        _syncState.value = SyncState.Error(e.message ?: "Sync failed")
                    }
                }
            }
        }
    }

    private fun stopPolling() {
        println("Stopping polling")
        pollingJob?.cancel()
    }

    /**
     * Push all dirty rows, then pull remote changes.
     * [tables] restricts the pull to specific tables (Realtime path); null = all tables (polling / manual).
     */
    private suspend fun doSync(tables: Set<String>? = null) {
        _syncState.value = SyncState.Syncing()
        syncEngine.pushLocalChanges()
        syncEngine.pullRemoteChanges(since = lastSyncTimestamp, tables = tables)
        lastSyncTimestamp = Clock.System.now().toEpochMilliseconds()
        _syncState.value = SyncState.Idle
    }

    suspend fun triggerSync() {
        try {
            withRetry { doSync() }
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
        if (!connectivityMonitor.isOnline.value) _syncState.value = SyncState.Offline
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
                    delay(backoff.milliseconds)
                    backoff = minOf(backoff * 2, cfg.maxBackoffMs)
                }
        }
    }
}
