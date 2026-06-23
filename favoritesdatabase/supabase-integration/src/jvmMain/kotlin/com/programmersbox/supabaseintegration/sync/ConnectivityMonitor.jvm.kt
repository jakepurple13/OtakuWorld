package com.programmersbox.supabaseintegration.sync

import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class JvmConnectivityMonitor : ConnectivityMonitor {
    override val isOnline: StateFlow<Connectivity.Status> = Connectivity { autoStart = true }
        .statusUpdates
        .stateIn(GlobalScope, SharingStarted.Eagerly, Connectivity.Status.Connected(false))
}

actual fun createConnectivityMonitor(context: Any?): ConnectivityMonitor = JvmConnectivityMonitor()
