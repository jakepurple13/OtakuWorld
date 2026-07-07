package com.programmersbox.supabaseintegration.database

import com.programmersbox.favoritesdatabase.HistoryDao

class HistoryManagedTable(
    private val historyDao: HistoryDao,
) : ManagedTable {
    override val displayName: String = "History"

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
                historyDao
                    .getAllHistorySync()
                    .forEach { historyDao.deleteHistory(it) }
            }

            SupportedTableAction.PURGE_DELETED -> {
                historyDao.deleteAllDeletedHistory()
            }

            SupportedTableAction.RESTORE_DELETED -> {
                historyDao.resetAllHistoryIsDeleted()
            }
        }
    }
}
