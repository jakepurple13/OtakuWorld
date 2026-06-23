package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.supabaseintegration.sync.HistoryItemRow
import com.programmersbox.supabaseintegration.sync.toHistoryItem
import com.programmersbox.supabaseintegration.sync.toHistoryItemRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow

class HistorySyncProcessor(
    private val historyDao: HistoryDao,
) : SyncProcessor<HistoryItem, HistoryItemRow>(
    tableName = "history"
) {

    // ==========================================
    // Push Implementations
    // ==========================================

    override suspend fun getDirtyItems(): List<HistoryItem> =
        historyDao.getDirtyHistory()

    override fun observeDirtyItems(): Flow<Int> = historyDao.observeDirtyHistoryCount()

    override fun isLocalDeleted(local: HistoryItem): Boolean =
        local.isDeleted

    override fun getLocalUpdatedAt(local: HistoryItem): Long =
        local.updatedAt

    override fun toRemoteRow(local: HistoryItem, uid: String, timestamp: Long): HistoryItemRow =
        local.toHistoryItemRow(uid, timestamp)

    override suspend fun markLocalSynced(local: HistoryItem, timestamp: Long) {
        historyDao.markHistorySynced(local.searchText, timestamp)
    }

    override suspend fun deleteLocal(local: HistoryItem) {
        historyDao.deleteHistory(local)
    }

    override suspend fun performUpsert(client: SupabaseClient, items: List<HistoryItemRow>) {
        client.postgrest[tableName].upsert(items) {
            onConflict = "user_id,search_text"
        }
    }

    // ==========================================
    // Pull Implementations
    // ==========================================

    override fun isRemoteDeleted(remote: HistoryItemRow): Boolean =
        remote.isDeleted

    override fun getRemoteUpdatedAt(remote: HistoryItemRow): Long =
        remote.updatedAt

    override suspend fun getLocalEquivalent(remote: HistoryItemRow): HistoryItem? =
        historyDao.getHistoryByKey(remote.searchText)

    override suspend fun upsertLocal(remote: HistoryItemRow) {
        historyDao.insertHistory(remote.toHistoryItem())
    }

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<HistoryItemRow> {
        return postgrestResult.decodeList<HistoryItemRow>()
    }
}