package com.programmersbox.kmpuiviews.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.favoritesdatabase.ftsMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AllNotesViewModel(
    private val notesDao: NotesDao,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val notes: StateFlow<List<NoteItem>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            // ftsMatch quotes each word, so punctuation can't break the FTS5 query
            ftsMatch(query)?.let(notesDao::searchNotes) ?: notesDao.getAllNotes()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun updateQuery(q: String) {
        searchQuery.value = q
    }

    fun saveNote(note: NoteItem, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (content.isBlank()) {
                notesDao.deleteNote(note.itemUrl)
            } else {
                notesDao.upsertNote(
                    note.copy(
                        content = content,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
    }

    fun deleteNote(itemUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            notesDao.deleteNote(itemUrl)
        }
    }
}
