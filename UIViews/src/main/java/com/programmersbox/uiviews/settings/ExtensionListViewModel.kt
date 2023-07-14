package com.programmersbox.uiviews.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.extensionloader.SourceInformation
import com.programmersbox.extensionloader.SourceRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ExtensionListViewModel(
    private val sourceRepository: SourceRepository
) : ViewModel() {
    val installedSources = mutableStateListOf<SourceInformation>()
    val remoteSources = mutableStateListOf<SourceInformation>()

    init {
        sourceRepository.sources
            .onEach {
                installedSources.clear()
                installedSources.addAll(it)
            }
            .launchIn(viewModelScope)
    }
}