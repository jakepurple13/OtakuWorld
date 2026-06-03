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

    val note: StateFlow<NoteItem?> = notesDao
        .getNote(itemUrl)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun saveNote(content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (content.isBlank()) {
                notesDao.deleteNote(itemUrl)
            } else {
                notesDao.upsertNote(
                    NoteItem(
                        itemUrl = itemUrl,
                        itemTitle = itemTitle,
                        content = content,
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                    )
                )
            }
        }
    }

    fun deleteNote() {
        viewModelScope.launch(Dispatchers.IO) {
            notesDao.deleteNote(itemUrl)
        }
    }
}
