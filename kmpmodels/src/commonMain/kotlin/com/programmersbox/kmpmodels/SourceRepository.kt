package com.programmersbox.kmpmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SourceRepository {
    private val sourcesList = MutableStateFlow<List<KmpSourceInformation>>(emptyList())
    val sources = sourcesList.asStateFlow()
    val list get() = sourcesList.value
    val apiServiceList get() = sourcesList.value.map { it.apiService }

    fun setSources(sourceList: List<KmpSourceInformation>) {
        sourcesList.update { sourceList }
    }

    fun addSource(sourceInformation: KmpSourceInformation) {
        sourcesList.update { it + sourceInformation }
    }

    fun addSources(sourceInformation: List<KmpSourceInformation>) {
        sourcesList.update { it + sourceInformation }
    }

    fun removeSource(sourceInformation: KmpSourceInformation) {
        sourcesList.update { it - sourceInformation }
    }

    fun toSource(name: String) = sourcesList.value.find { it.name == name }
    fun toSourceByApiServiceName(name: String) = sourcesList.value.find { it.apiService.serviceName == name }
}