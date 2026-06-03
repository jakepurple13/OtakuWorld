package com.programmersbox.kmpuiviews.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
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

private fun String.toFtsQuery(): String =
    trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.joinToString(" ") { "$it*" }

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AllNotesViewModel(
    private val notesDao: NotesDao,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val notes: StateFlow<List<NoteItem>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) notesDao.getAllNotes()
            else notesDao.searchNotes(query.toFtsQuery())
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
