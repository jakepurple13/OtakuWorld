package com.programmersbox.supabaseintegration.database

import com.programmersbox.favoritesdatabase.ItemDao

class FavoritesManagedTable(
    private val favoritesDao: ItemDao,
) : ManagedTable() {
    override val tableName: String = "favorites"
    override val displayName: String = "Favorites"

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
                favoritesDao
                    .getAllFavoritesSync()
                    .forEach { favoritesDao.deleteFavorite(it) }
            }

            SupportedTableAction.PURGE_DELETED -> {
                favoritesDao.deleteAllDeletedItems()
            }

            SupportedTableAction.RESTORE_DELETED -> {
                favoritesDao.resetAllIsDeleted()
            }
        }
    }
}