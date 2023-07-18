package com.programmersbox.uiviews.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.RemoteSources
import com.programmersbox.models.SourceInformation
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ExtensionListViewModel(
    sourceRepository: SourceRepository
) : ViewModel() {
    val installedSources = mutableStateListOf<SourceInformation>()
    val remoteSources = mutableStateMapOf<String, List<RemoteSources>>()

    init {
        sourceRepository.sources
            .onEach {
                installedSources.clear()
                installedSources.addAll(it)
            }
            .onEach {
                remoteSources.clear()
                remoteSources.putAll(
                    it.asSequence().map { it.catalog }
                        .filterIsInstance<ExternalApiServicesCatalog>()
                        .filter { it.hasRemoteSources }
                        .distinct()
                        .toList()
                        .map { it.name to it.getRemoteSources() }
                        .toMap()
                )
            }
            .launchIn(viewModelScope)
    }
}