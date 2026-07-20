package com.programmersbox.kmpmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SourceRepository {
    private val sourcesList = MutableStateFlow<List<KmpSourceInformation>>(emptyList())
    val sources = sourcesList.asStateFlow()
    val list get() = sourcesList.value
    val apiServiceList get() = sourcesList.value.map { it.apiService }

    private var previouslyManagedPackageNames: Set<String> = emptySet()

    /**
     * Replaces the set of sources this caller manages, diffed against what it managed on its
     * previous call - it only adds/updates/removes entries by [KmpSourceInformation.packageName]
     * within that managed set. Entries added via [addSource] by some other caller (never part of
     * any [setSources] call) are left alone, no matter how many times this is called.
     */
    fun setSources(sourceList: List<KmpSourceInformation>) {
        val newPackageNames = sourceList.map { it.packageName }.toSet()
        sourcesList.update { current ->
            current.filterNot { it.packageName in previouslyManagedPackageNames } + sourceList
        }
        previouslyManagedPackageNames = newPackageNames
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