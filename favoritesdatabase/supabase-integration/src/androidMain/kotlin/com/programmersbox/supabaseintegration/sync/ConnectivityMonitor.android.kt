package com.programmersbox.supabaseintegration.sync

import android.content.Context
import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {
    private val connectivity = Connectivity()
    private val scope = CoroutineScope(Dispatchers.Default)
    override val isOnline: StateFlow<Boolean> = connectivity.statusUpdates
        .map { it.isConnected }
        .stateIn(scope, SharingStarted.Eagerly, true)
    override val isMetered: StateFlow<Boolean> = connectivity.statusUpdates
        .map { (it as? Connectivity.Status.Connected)?.metered ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)
    override fun observe(): Flow<Boolean> = isOnline
}

actual fun createConnectivityMonitor(context: Any?): ConnectivityMonitor =
    AndroidConnectivityMonitor(context as Context)
