package com.programmersbox.uiviews

import com.programmersbox.models.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CurrentSourceRepository {
    private val sourceFlow = MutableStateFlow<ApiService?>(null)

    suspend fun emit(apiService: ApiService?) {
        sourceFlow.emit(apiService)
    }

    fun tryEmit(apiService: ApiService?) {
        sourceFlow.tryEmit(apiService)
    }

    fun asFlow() = sourceFlow.asStateFlow()

}