package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.supabaseintegration.database.CustomListInfoManagedTable
import com.programmersbox.supabaseintegration.database.CustomListItemManagedTable
import com.programmersbox.supabaseintegration.database.ManagedTable
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.CustomListInfoRow
import com.programmersbox.supabaseintegration.sync.CustomListItemRow
import com.programmersbox.supabaseintegration.sync.toCustomListInfo
import com.programmersbox.supabaseintegration.sync.toCustomListInfoRow
import com.programmersbox.supabaseintegration.sync.toCustomListItem
import com.programmersbox.supabaseintegration.sync.toCustomListItemRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow

class CustomListInfoSyncProcessor(
    private val listDao: ListDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<CustomListInfo, CustomListInfoRow>(tableName = "custom_list_info"),
    ManagedTable by CustomListInfoManagedTable(listDao) {
    override val displayName: String = "Custom Lists"

    // ==========================================
    // Push Implementations
    // ==========================================
    override suspend fun getDirtyItems(): List<CustomListInfo> = listDao.getDirtyCustomListInfo()
    override fun observeDirtyItems(): Flow<Int> = listDao.observeDirtyCustomListInfoCount()
    override fun isLocalDeleted(local: CustomListInfo): Boolean = local.isDeleted
    override fun getLocalUpdatedAt(local: CustomListInfo): Long = local.updatedAt

    override fun toRemoteRow(local: CustomListInfo, uid: String, timestamp: Long): CustomListInfoRow =
        local.toCustomListInfoRow(uid, timestamp)

    override suspend fun markLocalSynced(local: CustomListInfo, timestamp: Long) {
        listDao.markCustomListInfoSynced(local.uniqueId, timestamp)
    }

    override suspend fun deleteLocal(local: CustomListInfo) {
        listDao.removeItem(local)
    }

    override suspend fun performUpsert(client: SupabaseClient, items: List<CustomListInfoRow>) {
        client.postgrest[tableName].upsert(items) {
            onConflict = "user_id,unique_id"
        }
    }

    // ==========================================
    // Pull Implementations
    // ==========================================
    override fun isRemoteDeleted(remote: CustomListInfoRow): Boolean = remote.isDeleted
    override fun getRemoteUpdatedAt(remote: CustomListInfoRow): Long = remote.updatedAt

    override suspend fun getLocalEquivalent(remote: CustomListInfoRow): CustomListInfo? =
        listDao.getCustomListInfoByUniqueId(remote.uniqueId)

    override suspend fun upsertLocal(remote: CustomListInfoRow) {
        val local = listDao.getCustomListInfoByUniqueId(remote.uniqueId)
        val info = remote.toCustomListInfo()
        if (local == null) {
            listDao.addItem(info)
        } else {
            listDao.updateCustomListInfo(info)
        }
    }

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<CustomListInfoRow> {
        return postgrestResult.decodeList<CustomListInfoRow>()
    }
}

class CustomListItemSyncProcessor(
    private val listDao: ListDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<CustomListItem, CustomListItemRow>(
    tableName = "custom_list_items"
),
    ManagedTable by CustomListItemManagedTable(listDao) {
    override val displayName: String = "Custom List Items"

    // ==========================================
    // Push Implementations
    // ==========================================
    override suspend fun getDirtyItems(): List<CustomListItem> = listDao.getDirtyCustomListItems()
    override fun observeDirtyItems(): Flow<Int> = listDao.observeDirtyCustomListItemCount()
    override fun isLocalDeleted(local: CustomListItem): Boolean = local.isDeleted
    override fun getLocalUpdatedAt(local: CustomListItem): Long = local.updatedAt

    override fun toRemoteRow(local: CustomListItem, uid: String, timestamp: Long): CustomListItemRow =
        local.toCustomListItemRow(uid, timestamp)

    override suspend fun markLocalSynced(local: CustomListItem, timestamp: Long) {
        listDao.markCustomListItemSynced(local.uuid, timestamp)
    }

    override suspend fun deleteLocal(local: CustomListItem) {
        listDao.removeList(local)
    }

    override suspend fun performUpsert(client: SupabaseClient, items: List<CustomListItemRow>) {
        client.postgrest[tableName].upsert(items) {
            onConflict = "user_id,uuid"
        }
    }

    // ==========================================
    // Pull Implementations
    // ==========================================
    override fun isRemoteDeleted(remote: CustomListItemRow): Boolean = remote.isDeleted
    override fun getRemoteUpdatedAt(remote: CustomListItemRow): Long = remote.updatedAt

    override suspend fun getLocalEquivalent(remote: CustomListItemRow): CustomListItem? =
        listDao.getCustomListItemByUuid(remote.uuid)

    override suspend fun upsertLocal(remote: CustomListItemRow) {
        val local = listDao.getCustomListItemByUuid(remote.uuid)
        val item = remote.toCustomListItem()
        if (local == null) {
            listDao.createList(item)
        } else {
            listDao.updateCustomListItem(item)
        }
    }

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<CustomListItemRow> {
        return postgrestResult.decodeList<CustomListItemRow>()
    }
}
