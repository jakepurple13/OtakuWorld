package com.programmersbox.extensionloader

import com.programmersbox.models.SourceInformation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

//TODO: Move to kmpuiviews
// In the SourceLoader, it will map to the kmp versions before sending them here
class SourceRepository {
    private val sourcesList = MutableStateFlow<List<SourceInformation>>(emptyList())
    val sources = sourcesList.asStateFlow()
    val list get() = sourcesList.value
    val apiServiceList get() = sourcesList.value.map { it.apiService }

    fun setSources(sourceList: List<SourceInformation>) {
        sourcesList.value = sourceList
    }

    fun removeSource(sourceInformation: SourceInformation) {
        sourcesList.update {
            sourcesList.value.toMutableList().apply { remove(sourceInformation) }
        }
    }

    fun toSource(name: String) = sourcesList.value.find { it.name == name }
    fun toSourceByApiServiceName(name: String) = sourcesList.value.find { it.apiService.serviceName == name }
}