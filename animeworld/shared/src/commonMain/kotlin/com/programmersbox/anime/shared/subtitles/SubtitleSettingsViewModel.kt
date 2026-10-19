package com.programmersbox.anime.shared.subtitles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubtitleSettingsViewModel(
    private val repository: SubtitleSettingsRepository,
) : ViewModel() {

    val style: StateFlow<SubtitleStyle> = repository.style
        .stateIn(viewModelScope, SharingStarted.Eagerly, SubtitleStyle())

    fun update(transform: (SubtitleStyle) -> SubtitleStyle) {
        viewModelScope.launch { repository.update { transform(it).coerced() } }
    }

    fun reset() {
        viewModelScope.launch { repository.reset() }
    }
}
