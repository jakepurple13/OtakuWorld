package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import okio.BufferedSink
import okio.BufferedSource

class NotesBackupProcessor(
    private val notesDao: NotesDao,
) : BackupProcessor() {
    override val fileName: String
        get() = "notes.json"

    override suspend fun backup(sink: BufferedSink) {
        notesDao
            .getAllNotesSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        bufferedSource
            .readUtf8()
            .fromJson<List<NoteItem>>()
            .forEach { notesDao.upsertNote(it) }
    }
}