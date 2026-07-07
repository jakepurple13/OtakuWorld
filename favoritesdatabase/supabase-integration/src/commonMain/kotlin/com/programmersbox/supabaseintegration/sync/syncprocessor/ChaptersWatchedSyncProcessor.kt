package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.supabaseintegration.database.ChaptersWatchedManagedTable
import com.programmersbox.supabaseintegration.database.ManagedTable
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.ChapterWatchedRow
import com.programmersbox.supabaseintegration.sync.toChapterRow
import com.programmersbox.supabaseintegration.sync.toChapterWatched
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow

class ChaptersWatchedSyncProcessor(
    private val itemDao: ItemDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<ChapterWatched, ChapterWatchedRow>(tableName = "chapters_watched"),
    ManagedTable by ChaptersWatchedManagedTable(itemDao) {
    override val displayName: String = "Chapters Watched"

    // ==========================================
    // Push Implementations
    // ==========================================

    override suspend fun getDirtyItems(): List<ChapterWatched> =
        itemDao.getDirtyChapters()

    override fun observeDirtyItems(): Flow<Int> = itemDao.observeDirtyChapterCount()

    override fun isLocalDeleted(local: ChapterWatched): Boolean =
        local.isDeleted

    override fun getLocalUpdatedAt(local: ChapterWatched): Long =
        local.updatedAt

    override fun toRemoteRow(local: ChapterWatched, uid: String, timestamp: Long): ChapterWatchedRow =
        local.toChapterRow(uid, timestamp)

    override suspend fun markLocalSynced(local: ChapterWatched, timestamp: Long) {
        itemDao.markChapterSynced(local.url, timestamp)
    }

    override suspend fun deleteLocal(local: ChapterWatched) {
        itemDao.deleteChapter(local)
    }

    override suspend fun performUpsert(client: SupabaseClient, items: List<ChapterWatchedRow>) {
        client.postgrest[tableName].upsert(items) {
            onConflict = "user_id,url"
        }
    }

    // ==========================================
    // Pull Implementations
    // ==========================================

    override fun isRemoteDeleted(remote: ChapterWatchedRow): Boolean =
        remote.isDeleted

    override fun getRemoteUpdatedAt(remote: ChapterWatchedRow): Long =
        remote.updatedAt

    override suspend fun getLocalEquivalent(remote: ChapterWatchedRow): ChapterWatched? =
        itemDao.getChapterByUrl(remote.url)

    override suspend fun upsertLocal(remote: ChapterWatchedRow) {
        itemDao.insertChapterWatched(remote.toChapterWatched())
    }

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<ChapterWatchedRow> {
        return postgrestResult.decodeList<ChapterWatchedRow>()
    }
}