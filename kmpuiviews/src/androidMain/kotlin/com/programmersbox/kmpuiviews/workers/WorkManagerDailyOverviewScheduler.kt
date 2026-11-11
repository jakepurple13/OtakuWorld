package com.programmersbox.kmpuiviews.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewPrefs
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewScheduler
import com.programmersbox.kmpuiviews.domain.dailyoverview.initialDelay
import com.programmersbox.kmpuiviews.domain.dailyoverview.isOnSchedule
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * Unique periodic (24h) work that runs at the chosen time, [initialDelay] from now.
 *
 * The work is tagged with the time and the zone offset it was scheduled for. Sync re-enqueues when
 * that tag changes (new time, or a DST/zone change), or when the next run has drifted off the
 * chosen time ([isOnSchedule]): each period starts when the last run finished, so it creeps later.
 * Re-enqueueing uses UPDATE plus a next-run override, because UPDATE alone keeps the old schedule
 * once the work has run.
 */
class WorkManagerDailyOverviewScheduler(
    context: Context,
) : DailyOverviewScheduler {

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun sync(prefs: DailyOverviewPrefs) {
        if (!prefs.enabled || !prefs.notify) {
            workManager.cancelUniqueWork(DailyOverviewWorker.WORK_NAME)
            return
        }

        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val tag = "${DailyOverviewWorker.WORK_NAME}:${prefs.minuteOfDay}:${zone.offsetAt(now)}"
        val delay = initialDelay(now.toLocalDateTime(zone), prefs.minuteOfDay, zone = zone)
        val nowMillis = now.toEpochMilliseconds()
        val target = nowMillis + delay.inWholeMilliseconds
        val scheduled = workManager.getWorkInfosForUniqueWork(DailyOverviewWorker.WORK_NAME).get()
        val current = scheduled.any {
            !it.state.isFinished && tag in it.tags &&
                (it.state == WorkInfo.State.RUNNING || isOnSchedule(it.nextScheduleTimeMillis, target, nowMillis))
        }
        if (current) return

        workManager.enqueueUniquePeriodicWork(
            DailyOverviewWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<DailyOverviewWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .setNextScheduleTimeOverride(target)
                .addTag(tag)
                .build()
        )
    }
}
