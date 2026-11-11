package com.programmersbox.kmpuiviews.presentation.quickactions

import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

interface QuickActionCounts {
    val lists: Flow<Int>
    val savedNotifications: Flow<Int>

    /** Heat map reads for yesterday in the system time zone. */
    val yesterdayReads: Flow<Int>
}

class DaoQuickActionCounts(
    itemDao: ItemDao,
    listDao: ListDao,
    heatMapDao: HeatMapDao,
) : QuickActionCounts {
    override val lists: Flow<Int> = listDao.getAllListsCount()
    override val savedNotifications: Flow<Int> = itemDao.getAllNotificationCount()

    // Yesterday is resolved on each collection, so reopening the card after midnight moves it on
    override val yesterdayReads: Flow<Int> = flow {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        emitAll(heatMapDao.observeHeatMapByDate(today.minus(1, DateTimeUnit.DAY)).map { it?.count ?: 0 })
    }
}
