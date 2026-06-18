package com.programmersbox.supabaseintegration.sync

import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class JvmConnectivityMonitor : ConnectivityMonitor {
    private val _isOnline = MutableStateFlow(false)
    override val isOnline: StateFlow<Boolean> = _isOnline
    override fun observe(): Flow<Boolean> = _isOnline

    init {
        Connectivity { autoStart = true }
            .statusUpdates
            .map { it is Connectivity.Status.Connected }
            .onEach { _isOnline.value = it }
            .launchIn(GlobalScope)
    }
}

actual fun createConnectivityMonitor(context: Any?): ConnectivityMonitor = JvmConnectivityMonitor()
