package com.programmersbox.kmpuiviews.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.supabaseintegration.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class DetailsNotesViewModel(
    private val itemUrl: String,
    private val itemTitle: String,
    private val notesDao: NotesDao,
    aiText: AiText,
    private val authManager: AuthManager,
) : ViewModel() {

    val ai = NoteAiAssistant(aiText, viewModelScope)

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
                notesDao.removeNote(itemUrl, softDelete = authManager.isLoggedIn())
                return@launch
            }
            val current = notesDao.getNote(itemUrl).first()
            if (current?.content == content) return@launch
            val now = Clock.System.now().toEpochMilliseconds()
            notesDao.upsertNote(
                // Keep sync fields (supabaseId, createdAt) of an existing note
                current?.copy(
                    content = content,
                    timestamp = now,
                    updatedAt = now,
                    isDeleted = false,
                    isDirty = true,
                ) ?: NoteItem(
                    itemUrl = itemUrl,
                    itemTitle = itemTitle,
                    content = content,
                    timestamp = now,
                    updatedAt = now,
                )
            )
        }
    }

    fun deleteNote() {
        viewModelScope.launch(Dispatchers.IO) {
            notesDao.removeNote(itemUrl, softDelete = authManager.isLoggedIn())
        }
    }
}
