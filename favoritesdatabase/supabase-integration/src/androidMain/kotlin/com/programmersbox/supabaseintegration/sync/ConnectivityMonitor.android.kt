package com.programmersbox.supabaseintegration.sync

import android.content.Context
import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {
    private val connectivity = Connectivity { autoStart = true }
    private val scope = CoroutineScope(Dispatchers.Default)
    override val isOnline: StateFlow<Connectivity.Status> = connectivity
        .statusUpdates
        .stateIn(scope, SharingStarted.Eagerly, Connectivity.Status.Connected(false))
}

actual fun createConnectivityMonitor(context: Any?): ConnectivityMonitor =
    AndroidConnectivityMonitor(context as Context)
