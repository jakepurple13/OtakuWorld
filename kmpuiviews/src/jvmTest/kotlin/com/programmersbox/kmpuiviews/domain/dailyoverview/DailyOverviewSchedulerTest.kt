package com.programmersbox.kmpuiviews.domain.dailyoverview

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DailyOverviewSchedulerTest {
    private val utc = TimeZone.UTC
    private val newYork = TimeZone.of("America/New_York")
    private val eight = 8 * 60

    private fun at(hour: Int, minute: Int, day: Int = 11, month: Int = 11) =
        LocalDateTime(2026, month, day, hour, minute)

    @Test
    fun `before the time waits until the time`() {
        assertEquals(1.hours, initialDelay(at(7, 0), eight, zone = utc))
    }

    @Test
    fun `a few minutes before the time waits for it instead of running now`() {
        assertEquals(5.minutes, initialDelay(at(7, 55), eight, zone = utc))
    }

    @Test
    fun `after the time waits for tomorrow`() {
        assertEquals(23.hours, initialDelay(at(9, 0), eight, zone = utc))
    }

    @Test
    fun `exactly at the time waits for tomorrow`() {
        assertEquals(24.hours, initialDelay(at(8, 0), eight, zone = utc))
    }

    @Test
    fun `a time just after midnight runs that night`() {
        assertEquals(15.minutes, initialDelay(at(23, 50), 5, zone = utc))
    }

    @Test
    fun `spring forward keeps the wall-clock time`() {
        // DST starts 2026-03-08 02:00 in New York, so that day has 23 hours.
        assertEquals(22.hours, initialDelay(at(9, 0, day = 7, month = 3), eight, zone = newYork))
    }

    @Test
    fun `fall back keeps the wall-clock time`() {
        // DST ends 2026-11-01 02:00 in New York, so that day has 25 hours.
        assertEquals(24.hours, initialDelay(at(9, 0, day = 31, month = 10), eight, zone = newYork))
    }

    @Test
    fun `a next run within a minute of the target is on schedule`() {
        val now = 900_000_000L
        val target = 1_000_000_000L
        assertTrue(isOnSchedule(nextRunMillis = target, targetMillis = target, nowMillis = now))
        assertTrue(isOnSchedule(nextRunMillis = target + 30.seconds.inWholeMilliseconds, targetMillis = target, nowMillis = now))
    }

    @Test
    fun `a next run that drifted from the target is off schedule`() {
        val now = 900_000_000L
        val target = 1_000_000_000L
        assertFalse(isOnSchedule(nextRunMillis = target + 3.minutes.inWholeMilliseconds, targetMillis = target, nowMillis = now))
        assertFalse(isOnSchedule(nextRunMillis = target - 10.minutes.inWholeMilliseconds, targetMillis = target, nowMillis = now))
    }

    @Test
    fun `an overdue run is left alone so it still runs`() {
        val target = 1_000_000_000L
        val now = 500_000_000L
        assertTrue(isOnSchedule(nextRunMillis = now - 5.minutes.inWholeMilliseconds, targetMillis = target, nowMillis = now))
    }
}
