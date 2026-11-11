package com.programmersbox.kmpuiviews.presentation.quickactions

import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
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
    clock: Clock = Clock.System,
    zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : QuickActionCounts {
    override val lists: Flow<Int> = listDao.getAllListsCount()
    override val savedNotifications: Flow<Int> = itemDao.getAllNotificationCount()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val yesterdayReads: Flow<Int> = localDates(clock, zone)
        .flatMapLatest { today -> heatMapDao.observeHeatMapByDate(today.minus(1, DateTimeUnit.DAY)) }
        .map { it?.count ?: 0 }
}

/** Today's date in [zone], emitted again at each local midnight. */
internal fun localDates(clock: Clock, zone: () -> TimeZone): Flow<LocalDate> = flow {
    while (true) {
        val tz = zone()
        val now = clock.now()
        val today = now.toLocalDateTime(tz).date
        emit(today)
        delay(today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz) - now)
    }
}.distinctUntilChanged()
