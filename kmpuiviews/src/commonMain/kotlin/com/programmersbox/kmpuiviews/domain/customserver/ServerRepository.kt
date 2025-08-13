package com.programmersbox.kmpuiviews.domain.customserver

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ServerRepository : KoinComponent {

    private val settings = MutableStateFlow(true)

    val customServerHandle = MutableStateFlow<CustomServerHandle?>(null)

    suspend fun init() = coroutineScope {
        settings
            .map { get<CustomServerHandle>() }
            .onEach { customServerHandle.emit(it) }
            .onEach { it.listenToSSE() }
            .launchIn(this)
    }
}