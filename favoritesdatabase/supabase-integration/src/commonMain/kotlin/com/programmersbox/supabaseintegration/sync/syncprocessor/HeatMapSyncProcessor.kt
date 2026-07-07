package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.supabaseintegration.database.HeatMapManagedTable
import com.programmersbox.supabaseintegration.database.ManagedTable
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.HeatMapItemRow
import com.programmersbox.supabaseintegration.sync.toHeatMapItem
import com.programmersbox.supabaseintegration.sync.toHeatMapItemRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class HeatMapSyncProcessor(
    private val heatMapDao: HeatMapDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<HeatMapItem, HeatMapItemRow>(
    tableName = "heatmap_items"
),
    ManagedTable by HeatMapManagedTable(heatMapDao) {
    override val displayName: String = "Activity Heat Map"

    // ==========================================
    // Push Implementations
    // ==========================================

    override suspend fun getDirtyItems(): List<HeatMapItem> =
        heatMapDao.getDirtyHeatMapItems()

    override fun observeDirtyItems(): Flow<Int> = heatMapDao.observeDirtyHeatMapCount()

    override fun isLocalDeleted(local: HeatMapItem): Boolean =
        local.isDeleted

    override fun getLocalUpdatedAt(local: HeatMapItem): Long =
        local.updatedAt

    override fun toRemoteRow(local: HeatMapItem, uid: String, timestamp: Long): HeatMapItemRow =
        local.toHeatMapItemRow(uid, timestamp)

    override suspend fun markLocalSynced(local: HeatMapItem, timestamp: Long) {
        heatMapDao.markHeatMapItemSynced(local.time, timestamp)
    }

    override suspend fun deleteLocal(local: HeatMapItem) {
        heatMapDao.deleteHeatMap(local)
    }

    override suspend fun performUpsert(client: SupabaseClient, items: List<HeatMapItemRow>) {
        client.postgrest[tableName].upsert(items) {
            onConflict = "user_id,time"
        }
    }

    // ==========================================
    // Pull Implementations
    // ==========================================

    override fun isRemoteDeleted(remote: HeatMapItemRow): Boolean =
        remote.isDeleted

    override fun getRemoteUpdatedAt(remote: HeatMapItemRow): Long =
        remote.updatedAt

    override suspend fun getLocalEquivalent(remote: HeatMapItemRow): HeatMapItem? {
        val localDate = LocalDate.parse(remote.time)
        return heatMapDao.getHeatMapItemByTime(localDate)
    }

    override suspend fun upsertLocal(remote: HeatMapItemRow) {
        heatMapDao.insertHeatMap(remote.toHeatMapItem())
    }

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<HeatMapItemRow> {
        return postgrestResult.decodeList<HeatMapItemRow>()
    }
}