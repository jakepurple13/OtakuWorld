package com.programmersbox.uiviews.settings

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.RemoteSources
import com.programmersbox.models.SourceInformation
import com.programmersbox.uiviews.OtakuWorldCatalog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ExtensionListViewModel(
    sourceRepository: SourceRepository,
    otakuWorldCatalog: OtakuWorldCatalog
) : ViewModel() {
    private val installedSources = mutableStateListOf<SourceInformation>()
    val remoteSources = mutableStateMapOf<String, RemoteViewState>()

    val installed by derivedStateOf {
        installedSources
            .groupBy { it.catalog }
            .mapValues { InstalledViewState(it.value) }
    }

    init {
        sourceRepository.sources
            .onEach {
                installedSources.clear()
                installedSources.addAll(it)
            }
            .onEach { sources ->
                remoteSources.clear()
                remoteSources["${otakuWorldCatalog.name}World"] = RemoteViewState(otakuWorldCatalog.getRemoteSources())
                remoteSources.putAll(
                    sources.asSequence()
                        .map { it.catalog }
                        .filterIsInstance<ExternalApiServicesCatalog>()
                        .filter { it.hasRemoteSources }
                        .distinct()
                        .toList()
                        .associate { it.name to RemoteViewState(it.getRemoteSources()) }
                )
            }
            .launchIn(viewModelScope)
    }
}

class InstalledViewState(
    val sourceInformation: List<SourceInformation>
) {
    var showItems by mutableStateOf(false)
}

class RemoteViewState(
    val sources: List<RemoteSources>
) {
    var showItems by mutableStateOf(false)
}