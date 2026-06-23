package com.programmersbox.supabaseintegration.sync

import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.flow.StateFlow

interface ConnectivityMonitor {
    val isOnline: StateFlow<Connectivity.Status>
}

expect fun createConnectivityMonitor(context: Any?): ConnectivityMonitor
