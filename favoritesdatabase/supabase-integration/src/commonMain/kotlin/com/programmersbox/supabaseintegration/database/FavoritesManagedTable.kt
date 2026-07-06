package com.programmersbox.supabaseintegration.database

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.ManagedTable
import com.programmersbox.kmpmodels.SupportedTableAction

class FavoritesManagedTable(
    private val favoritesDao: ItemDao,
) : ManagedTable() {
    override val databaseName: String = "favorites"
    override val tableName: String = "favorites"
    override val displayName: String = "Favorites"

    override val defaultAction: SupportedTableAction = SupportedTableAction.NONE

    override val supportedActions: List<SupportedTableAction> = listOf(
        SupportedTableAction.NONE,
        SupportedTableAction.CLEAR_ALL,
        SupportedTableAction.PURGE_DELETED,
        SupportedTableAction.RESTORE_DELETED
    )

    override suspend fun clearAll() {
        favoritesDao.getAllFavoritesSync().forEach {
            favoritesDao.deleteFavorite(it)
        }
    }

    override suspend fun purgeDeleted() {
        TODO("Not yet implemented")
    }

    override suspend fun restoreDeleted() {
        TODO("Not yet implemented")
    }

}