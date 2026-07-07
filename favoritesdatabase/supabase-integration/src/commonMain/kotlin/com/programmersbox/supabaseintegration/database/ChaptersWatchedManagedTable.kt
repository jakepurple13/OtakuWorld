package com.programmersbox.supabaseintegration.database

import com.programmersbox.favoritesdatabase.ItemDao

class ChaptersWatchedManagedTable(
    private val itemDao: ItemDao,
) : ManagedTable {
    override val displayName: String = "Chapters Watched"
    override val supportedActions: List<SupportedTableAction> = listOf(
        SupportedTableAction.NONE,
        SupportedTableAction.CLEAR_ALL,
        SupportedTableAction.PURGE_DELETED,
        SupportedTableAction.RESTORE_DELETED
    )
    override val defaultAction: SupportedTableAction = SupportedTableAction.NONE

    override suspend fun executeAction(action: SupportedTableAction) {
        when (action) {
            SupportedTableAction.NONE -> Unit
            SupportedTableAction.CLEAR_ALL -> {
                itemDao
                    .getAllChaptersSync()
                    .forEach { itemDao.deleteChapter(it) }
            }

            SupportedTableAction.PURGE_DELETED -> {
                itemDao.deleteAllDeletedChapters()
            }

            SupportedTableAction.RESTORE_DELETED -> {
                itemDao.resetAllChaptersIsDeleted()
            }
        }
    }
}