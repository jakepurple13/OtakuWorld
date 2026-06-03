package com.programmersbox.kmpuiviews.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class DetailsNotesViewModel(
    private val itemUrl: String,
    private val itemTitle: String,
    private val notesDao: NotesDao,
) : ViewModel() {

    val notes: StateFlow<List<NoteItem>> = notesDao
        .getNotesForItem(itemUrl)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun saveNote(note: NoteItem?, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when {
                note != null && content.isBlank() -> notesDao.deleteNoteById(note.id)
                note != null -> notesDao.updateNote(
                    note.copy(
                        content = content,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
                content.isNotBlank() -> notesDao.insertNote(
                    NoteItem(
                        itemUrl = itemUrl,
                        itemTitle = itemTitle,
                        content = content,
                    )
                )
            }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            notesDao.deleteNoteById(id)
        }
    }
}
