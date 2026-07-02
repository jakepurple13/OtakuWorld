package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.FavoriteItemRow
import com.programmersbox.supabaseintegration.sync.toDbModel
import com.programmersbox.supabaseintegration.sync.toFavoriteRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult

class FavoritesSyncer(
    private val itemDao: ItemDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<DbModel, FavoriteItemRow>(
    tableName = "favorite_items"
) {
    override val displayName: String = "Favorites"

    override suspend fun getDirtyItems() = itemDao.getDirtyFavorites()
    override fun observeDirtyItems() = itemDao.observeDirtyFavoriteCount()
    override fun isLocalDeleted(local: DbModel) = local.isDeleted
    override fun getLocalUpdatedAt(local: DbModel) = local.updatedAt

    override fun toRemoteRow(local: DbModel, uid: String, timestamp: Long) =
        local.toFavoriteRow(uid, timestamp)

    override suspend fun markLocalSynced(local: DbModel, timestamp: Long) =
        itemDao.markFavoriteSynced(local.url, timestamp)

    override suspend fun deleteLocal(local: DbModel) =
        itemDao.deleteFavorite(local)

    // NEW: Handle the upsert with the reified type here
    override suspend fun performUpsert(client: SupabaseClient, items: List<FavoriteItemRow>) {
        client.postgrest[tableName].upsert(items) {
            onConflict = "user_id,url"
        }
    }

    override fun isRemoteDeleted(remote: FavoriteItemRow) = remote.isDeleted
    override fun getRemoteUpdatedAt(remote: FavoriteItemRow) = remote.updatedAt

    override suspend fun getLocalEquivalent(remote: FavoriteItemRow) =
        itemDao.getFavoriteByUrl(remote.url)

    override suspend fun upsertLocal(remote: FavoriteItemRow) {
        itemDao.insertFavorite(remote.toDbModel())
    }

    // NEW: Handle the select and decodeList with the reified type here
    override suspend fun performSelect(postgrestResult: PostgrestResult): List<FavoriteItemRow> {
        return postgrestResult.decodeList<FavoriteItemRow>()
    }
}