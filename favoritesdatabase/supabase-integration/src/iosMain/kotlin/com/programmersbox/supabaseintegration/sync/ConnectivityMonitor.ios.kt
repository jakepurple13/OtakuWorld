package com.programmersbox.supabaseintegration.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Production: replace with NWPathMonitor via platform.Network
class IosConnectivityMonitor : ConnectivityMonitor {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline
    override fun observe(): Flow<Boolean> = _isOnline
}

actual fun createConnectivityMonitor(context: Any?): ConnectivityMonitor = IosConnectivityMonitor()
