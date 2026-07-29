package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class NotesBackupProcessor(
    private val notesDao: NotesDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "notes.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Notes"
    override val description: String? get() = "Per-item notes"
    override val icon get() = Icons.Default.EditNote

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val notes = notesDao.getAllNotesSync()
        notes.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = notes.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        bufferedSource.readUtf8().fromJson<List<NoteItem>>().restoreEachCatching(idOf = { it.itemTitle }) {
            notesDao.upsertNote(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = notesDao.getAllNotesSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<NoteItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
