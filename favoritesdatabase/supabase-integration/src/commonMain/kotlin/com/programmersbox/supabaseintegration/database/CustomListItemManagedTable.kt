package com.programmersbox.supabaseintegration.database

import com.programmersbox.favoritesdatabase.ListDao

class CustomListItemManagedTable(
    private val listDao: ListDao,
) : ManagedTable {
    override val displayName: String = "Custom List Items"

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
                listDao
                    .getAllCustomListItemsSync()
                    .forEach { listDao.removeList(it) }
            }

            SupportedTableAction.PURGE_DELETED -> {
                listDao.deleteAllDeletedCustomListItems()
            }

            SupportedTableAction.RESTORE_DELETED -> {
                listDao.resetAllCustomListItemsIsDeleted()
            }
        }
    }
}
