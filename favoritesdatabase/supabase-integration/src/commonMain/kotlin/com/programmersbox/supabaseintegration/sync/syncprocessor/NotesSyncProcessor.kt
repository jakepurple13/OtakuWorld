package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.NoteItemRow
import com.programmersbox.supabaseintegration.sync.toNoteItem
import com.programmersbox.supabaseintegration.sync.toNoteItemRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow

class NotesSyncProcessor(
    private val notesDao: NotesDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<NoteItem, NoteItemRow>(
    tableName = "notes"
) {
    override val displayName: String = "Notes"

    // ==========================================
    // Push Implementations
    // ==========================================

    override suspend fun getDirtyItems(): List<NoteItem> =
        notesDao.getDirtyNotes()

    override fun observeDirtyItems(): Flow<Int> = notesDao.observeDirtyNoteCount()

    override fun isLocalDeleted(local: NoteItem): Boolean =
        local.isDeleted

    override fun getLocalUpdatedAt(local: NoteItem): Long =
        local.updatedAt

    override fun toRemoteRow(local: NoteItem, uid: String, timestamp: Long): NoteItemRow =
        local.toNoteItemRow(uid, timestamp)

    override suspend fun markLocalSynced(local: NoteItem, timestamp: Long) {
        notesDao.markNoteSynced(local.itemUrl, timestamp)
    }

    override suspend fun deleteLocal(local: NoteItem) {
        notesDao.deleteNote(local.itemUrl)
    }

    override suspend fun performUpsert(client: SupabaseClient, items: List<NoteItemRow>) {
        client.postgrest[tableName].upsert(items) {
            onConflict = "user_id,item_url"
        }
    }

    // ==========================================
    // Pull Implementations
    // ==========================================

    override fun isRemoteDeleted(remote: NoteItemRow): Boolean =
        remote.isDeleted

    override fun getRemoteUpdatedAt(remote: NoteItemRow): Long =
        remote.updatedAt

    override suspend fun getLocalEquivalent(remote: NoteItemRow): NoteItem? =
        notesDao.getNoteByUrl(remote.itemUrl)

    override suspend fun upsertLocal(remote: NoteItemRow) {
        notesDao.upsertNote(remote.toNoteItem())
    }

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<NoteItemRow> {
        return postgrestResult.decodeList<NoteItemRow>()
    }
}