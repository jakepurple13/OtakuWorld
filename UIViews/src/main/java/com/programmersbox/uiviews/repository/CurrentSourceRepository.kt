package com.programmersbox.uiviews.repository

import com.programmersbox.kmpmodels.KmpApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CurrentSourceRepository {
    private val sourceFlow = MutableStateFlow<KmpApiService?>(null)

    fun asFlow() = sourceFlow.asStateFlow()

    suspend fun emit(apiService: KmpApiService?) {
        sourceFlow.emit(apiService)
    }

    fun tryEmit(apiService: KmpApiService?) {
        sourceFlow.tryEmit(apiService)
    }
}