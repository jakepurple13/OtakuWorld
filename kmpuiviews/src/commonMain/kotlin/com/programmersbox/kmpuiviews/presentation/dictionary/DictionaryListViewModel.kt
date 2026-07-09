package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionaryRepository
import com.programmersbox.favoritesdatabase.DictionarySort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DictionaryListViewModel(
    private val repository: DictionaryRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sort = MutableStateFlow(DictionarySort.Term)

    val entries: StateFlow<List<DictionaryEntry>> = searchQuery
        .debounce(300)
        .combine(sort) { query, sort -> query to sort }
        .flatMapLatest { (query, sort) ->
            if (query.isBlank()) {
                repository.getAll(sort)
            } else {
                repository.search(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun updateQuery(q: String) {
        searchQuery.value = q
    }

    fun updateSort(newSort: DictionarySort) {
        sort.value = newSort
    }

    fun delete(entry: DictionaryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(entry)
        }
    }
}
