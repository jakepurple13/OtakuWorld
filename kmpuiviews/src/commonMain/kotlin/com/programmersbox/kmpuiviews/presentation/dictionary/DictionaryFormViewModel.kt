package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionaryRepository
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DictionaryFormViewModel(
    form: Screen.DictionaryScreen.Form,
    private val repository: DictionaryRepository,
) : ViewModel() {

    val entry: StateFlow<DictionaryEntry?> = (form.id?.let { repository.getById(it) } ?: flowOf(null))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun save(
        term: String,
        definition: String?,
        reading: String?,
        category: String?,
        notes: String?,
        language: String?,
    ) {
        val existing = entry.value
        val toSave = existing?.copy(
            term = term,
            definition = definition,
            reading = reading,
            category = category,
            notes = notes,
            language = language,
        ) ?: DictionaryEntry(
            term = term,
            definition = definition,
            reading = reading,
            category = category,
            notes = notes,
            language = language,
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.save(toSave)
        }
    }
}
