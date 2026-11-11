package com.programmersbox.kmpuiviews.domain.dailyoverview

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.math.abs
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
 * How long from [now] until the next [minuteOfDay]. The work runs at that time: it generates the
 * summary, then posts. Exactly at the time means tomorrow. Times are wall-clock times in [zone],
 * so a DST change still lands on the chosen local time.
 */
fun initialDelay(
    now: LocalDateTime,
    minuteOfDay: Int,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Duration {
    val nowInstant = now.toInstant(zone)
    val time = LocalTime(minuteOfDay / 60, minuteOfDay % 60)
    val postToday = LocalDateTime(now.date, time).toInstant(zone)
    val post = if (nowInstant < postToday) postToday
    else LocalDateTime(now.date.plus(1, DateTimeUnit.DAY), time).toInstant(zone)
    return post - nowInstant
}

/**
 * Periodic work drifts: each 24h period starts when the last run finished, not at the chosen time.
 * True while the scheduled [nextRunMillis] is still within a minute of [targetMillis], or when the
 * run is overdue at [nowMillis] (the system delayed it), so rescheduling doesn't skip it.
 */
fun isOnSchedule(nextRunMillis: Long, targetMillis: Long, nowMillis: Long): Boolean =
    nextRunMillis <= nowMillis || abs(nextRunMillis - targetMillis) <= 1.minutes.inWholeMilliseconds
