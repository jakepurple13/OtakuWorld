package com.programmersbox.supabaseintegration.database

import com.programmersbox.favoritesdatabase.NotesDao

class NotesManagedTable(
    private val notesDao: NotesDao,
) : ManagedTable {
    override val displayName: String = "Notes"

    override val defaultAction: SupportedTableAction = SupportedTableAction.NONE

    override val supportedActions: List<SupportedTableAction> = listOf(
        SupportedTableAction.NONE,
        SupportedTableAction.CLEAR_ALL,
        SupportedTableAction.PURGE_DELETED,
        SupportedTableAction.RESTORE_DELETED
    )

    override suspend fun executeAction(action: SupportedTableAction) {
        when (action) {
            SupportedTableAction.NONE -> Unit
            SupportedTableAction.CLEAR_ALL -> {
                notesDao
                    .getAllNotesSync()
                    .forEach { notesDao.deleteNote(it.itemUrl) }
            }

            SupportedTableAction.PURGE_DELETED -> {
                notesDao.deleteAllDeletedNotes()
            }

            SupportedTableAction.RESTORE_DELETED -> {
                notesDao.resetAllNotesIsDeleted()
            }
        }
    }
}
