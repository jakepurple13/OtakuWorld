package com.programmersbox.supabaseintegration.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ConnectivityMonitor {
    val isOnline: StateFlow<Boolean>
    /** true = metered (cellular), false = unmetered (WiFi / Ethernet / Desktop) */
    val isMetered: StateFlow<Boolean>
    fun observe(): Flow<Boolean>
}

expect fun createConnectivityMonitor(context: Any?): ConnectivityMonitor
