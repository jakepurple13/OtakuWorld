package com.programmersbox.kmpuiviews.presentation.quickactions

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.programmersbox.favoritesdatabase.HeatMapDatabase
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.kmpuiviews.testing.createTestHeatMapDatabase
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.time.TimeSource

class QuickActionCountsTest {
    private val zone = TimeZone.of("America/Phoenix")
    private val midnight = LocalDateTime(2026, 11, 12, 0, 0).toInstant(zone)

    private lateinit var items: ItemDatabase
    private lateinit var lists: ListDatabase
    private lateinit var heatMap: HeatMapDatabase

    @BeforeTest
    fun setUp() {
        items = createTestItemDatabase()
        val listFile = File.createTempFile("quick-action-counts-lists", ".db").also { it.deleteOnExit() }
        lists = Room.databaseBuilder<ListDatabase>(name = listFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        heatMap = createTestHeatMapDatabase()
    }

    @AfterTest
    fun tearDown() {
        items.close()
        lists.close()
        heatMap.close()
    }

    /** Clock that follows the test scheduler's virtual time from [start]. */
    private fun TestScope.virtualClock(start: Instant) = object : Clock {
        override fun now(): Instant = start + testScheduler.currentTime.milliseconds
    }

    @Test
    fun localDatesEmitsAgainAtMidnight() = runTest {
        val clock = virtualClock(midnight - 60_000.milliseconds)
        localDates(clock) { zone }.test {
            assertEquals(LocalDate(2026, 11, 11), awaitItem())
            advanceTimeBy(59_999)
            runCurrent()
            expectNoEvents()
            advanceTimeBy(2)
            runCurrent()
            assertEquals(LocalDate(2026, 11, 12), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun yesterdayReadsMovesToNewDayAfterMidnight() = runTest {
        heatMap.heatMapDao().insertHeatMap(HeatMapItem(time = LocalDate(2026, 11, 10), count = 5))
        heatMap.heatMapDao().insertHeatMap(HeatMapItem(time = LocalDate(2026, 11, 11), count = 9))
        // Room emits on its own threads, so this runs on real time: midnight comes 1.5s in
        val started = TimeSource.Monotonic.markNow()
        val start = midnight - 1_500.milliseconds
        val clock = object : Clock {
            override fun now(): Instant = start + started.elapsedNow()
        }
        val counts = DaoQuickActionCounts(items.itemDao(), lists.listDao(), heatMap.heatMapDao(), clock) { zone }

        counts.yesterdayReads.flowOn(Dispatchers.Default).test {
            assertEquals(5, awaitItem())
            assertEquals(9, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
