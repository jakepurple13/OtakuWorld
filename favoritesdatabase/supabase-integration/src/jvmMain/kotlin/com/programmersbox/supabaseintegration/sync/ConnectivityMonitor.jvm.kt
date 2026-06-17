package com.programmersbox.supabaseintegration.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetSocketAddress
import java.net.Socket

class JvmConnectivityMonitor : ConnectivityMonitor {
    private val _isOnline = MutableStateFlow(checkConnection())
    override val isOnline: StateFlow<Boolean> = _isOnline
    override fun observe(): Flow<Boolean> = _isOnline

    private fun checkConnection(): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress("8.8.8.8", 53), 1500) }
        true
    }.getOrDefault(false)
}

actual fun createConnectivityMonitor(context: Any?): ConnectivityMonitor = JvmConnectivityMonitor()
