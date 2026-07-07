package com.programmersbox.supabaseintegration.database

import com.programmersbox.favoritesdatabase.BookmarkDao

class BookmarksManagedTable(
    private val bookmarkDao: BookmarkDao,
) : ManagedTable {
    override val displayName: String = "Bookmarks"

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
                bookmarkDao
                    .getAllBookmarksSync()
                    .forEach { bookmarkDao.deleteBookmark(it) }
            }

            SupportedTableAction.PURGE_DELETED -> {
                bookmarkDao.deleteAllDeletedBookmarks()
            }

            SupportedTableAction.RESTORE_DELETED -> {
                bookmarkDao.resetAllBookmarksIsDeleted()
            }
        }
    }
}
