package com.programmersbox.uiviews.repository

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.ModelMapper
import com.programmersbox.models.ApiService
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

    suspend fun emit(apiService: ApiService?) {
        sourceFlow.emit(apiService?.let(ModelMapper::mapApiService))
    }

    fun tryEmit(apiService: ApiService?) {
        sourceFlow.tryEmit(apiService?.let(ModelMapper::mapApiService))
    }
}