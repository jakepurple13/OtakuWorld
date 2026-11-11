package com.programmersbox.kmpuiviews.domain.dailyoverview

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Keeps the morning notification work in step with [DailyOverviewPrefs]. */
interface DailyOverviewScheduler {
    /** Schedules the work when `enabled && notify`, and cancels it otherwise. */
    fun sync(prefs: DailyOverviewPrefs)
}

/** Platforms without a morning notification. */
object NoOpDailyOverviewScheduler : DailyOverviewScheduler {
    override fun sync(prefs: DailyOverviewPrefs) = Unit
}

/**
 * How long from [now] until the work should start: [leadMinutes] before the next [minuteOfDay].
 * Inside the lead window the work runs now. Times are wall-clock times in [zone], so a DST change
 * still lands on the chosen local time.
 */
fun initialDelay(
    now: LocalDateTime,
    minuteOfDay: Int,
    leadMinutes: Int = 10,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Duration {
    val nowInstant = now.toInstant(zone)
    val time = LocalTime(minuteOfDay / 60, minuteOfDay % 60)
    val postToday = LocalDateTime(now.date, time).toInstant(zone)
    val lead = leadMinutes.minutes
    val post = if (nowInstant < postToday) postToday
    else LocalDateTime(now.date.plus(1, DateTimeUnit.DAY), time).toInstant(zone)
    return (post - lead - nowInstant).coerceAtLeast(Duration.ZERO)
}
