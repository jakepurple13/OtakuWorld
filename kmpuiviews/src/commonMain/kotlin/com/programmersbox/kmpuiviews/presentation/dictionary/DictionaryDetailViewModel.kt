package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.kmpuiviews.repository.DictionaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DictionaryDetailViewModel(
    private val id: Long,
    private val repository: DictionaryRepository,
) : ViewModel() {

    val entry: StateFlow<DictionaryEntry?> = repository
        .getById(id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun delete() {
        val current = entry.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(current)
        }
    }
}
