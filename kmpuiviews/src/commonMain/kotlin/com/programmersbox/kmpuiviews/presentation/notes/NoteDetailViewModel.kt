package com.programmersbox.kmpuiviews.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Backs the full-screen note opened from the Notes list. Edits keep the note's sync fields. */
class NoteDetailViewModel(
    val itemUrl: String,
    private val notesDao: NotesDao,
) : ViewModel() {

    val note: StateFlow<NoteItem?> = notesDao
        .getNote(itemUrl)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /**
     * Saves on leaving the screen, which is also when the ViewModel is cleared, so the write runs
     * outside viewModelScope's cancellation. Does nothing once the note is gone (deleted elsewhere).
     */
    fun save(content: String) {
        val current = note.value ?: return
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            if (content.isBlank()) {
                notesDao.deleteNote(itemUrl)
            } else if (content != current.content) {
                notesDao.upsertNote(
                    current.copy(
                        content = content,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
    }

    fun delete() {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            notesDao.deleteNote(itemUrl)
        }
    }
}
