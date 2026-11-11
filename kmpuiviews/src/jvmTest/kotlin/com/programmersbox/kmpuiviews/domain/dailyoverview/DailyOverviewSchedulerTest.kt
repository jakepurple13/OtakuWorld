package com.programmersbox.kmpuiviews.domain.dailyoverview

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DailyOverviewSchedulerTest {
    private val utc = TimeZone.UTC
    private val newYork = TimeZone.of("America/New_York")
    private val eight = 8 * 60

    private fun at(hour: Int, minute: Int, day: Int = 11, month: Int = 11) =
        LocalDateTime(2026, month, day, hour, minute)

    @Test
    fun `before the lead window waits until ten minutes before`() {
        assertEquals(50.minutes, initialDelay(at(7, 0), eight, zone = utc))
    }

    @Test
    fun `inside the lead window runs now`() {
        assertEquals(Duration.ZERO, initialDelay(at(7, 55), eight, zone = utc))
    }

    @Test
    fun `after the time waits for tomorrow`() {
        assertEquals(22.hours + 50.minutes, initialDelay(at(9, 0), eight, zone = utc))
    }

    @Test
    fun `exactly at the time waits for tomorrow`() {
        assertEquals(23.hours + 50.minutes, initialDelay(at(8, 0), eight, zone = utc))
    }

    @Test
    fun `a time near midnight leads into the previous day`() {
        // 00:05 posts, so the work starts at 23:55 the day before.
        assertEquals(5.minutes, initialDelay(at(23, 50), 5, zone = utc))
        assertEquals(Duration.ZERO, initialDelay(at(23, 58), 5, zone = utc))
    }

    @Test
    fun `spring forward keeps the wall-clock time`() {
        // DST starts 2026-03-08 02:00 in New York, so that day has 23 hours.
        assertEquals(21.hours + 50.minutes, initialDelay(at(9, 0, day = 7, month = 3), eight, zone = newYork))
    }

    @Test
    fun `fall back keeps the wall-clock time`() {
        // DST ends 2026-11-01 02:00 in New York, so that day has 25 hours.
        assertEquals(23.hours + 50.minutes, initialDelay(at(9, 0, day = 31, month = 10), eight, zone = newYork))
    }

    @Test
    fun `custom lead`() {
        assertEquals(30.minutes, initialDelay(at(7, 0), eight, leadMinutes = 30, zone = utc))
    }
}
