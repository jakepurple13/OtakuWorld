package com.programmersbox.kmpuiviews.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.favoritesdatabase.ftsMatch
import com.programmersbox.supabaseintegration.auth.AuthManager
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
    private val authManager: AuthManager,
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
                notesDao.removeNote(note.itemUrl, softDelete = authManager.isLoggedIn())
            } else {
                val now = Clock.System.now().toEpochMilliseconds()
                notesDao.upsertNote(
                    note.copy(
                        content = content,
                        timestamp = now,
                        updatedAt = now,
                        isDirty = true,
                    )
                )
            }
        }
    }

    fun deleteNote(itemUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            notesDao.removeNote(itemUrl, softDelete = authManager.isLoggedIn())
        }
    }
}
