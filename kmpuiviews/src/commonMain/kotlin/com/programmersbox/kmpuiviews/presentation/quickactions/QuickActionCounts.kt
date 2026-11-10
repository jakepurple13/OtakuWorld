package com.programmersbox.kmpuiviews.presentation.quickactions

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import kotlinx.coroutines.flow.Flow

interface QuickActionCounts {
    val lists: Flow<Int>
    val savedNotifications: Flow<Int>
}

class DaoQuickActionCounts(
    itemDao: ItemDao,
    listDao: ListDao,
) : QuickActionCounts {
    override val lists: Flow<Int> = listDao.getAllListsCount()
    override val savedNotifications: Flow<Int> = itemDao.getAllNotificationCount()
}
