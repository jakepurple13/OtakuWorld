package com.programmersbox.uiviews.di.kmpinterop

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import com.programmersbox.uiviews.checkers.AppCheckWorker
import com.programmersbox.uiviews.checkers.AppCleanupWorker
import com.programmersbox.uiviews.checkers.CloudToLocalSyncWorker
import com.programmersbox.uiviews.checkers.LocalToCloudSyncWorker
import com.programmersbox.uiviews.checkers.SourceUpdateChecker
import com.programmersbox.uiviews.checkers.UpdateFlowWorker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.TimeUnit

class BackgroundWorkHandlerImpl(
    context: Context,
    private val dataStoreHandling: DataStoreHandling,
) : BackgroundWorkHandler {

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = combine(
        workManager.getWorkInfosForUniqueWorkFlow("sourceChecks"),
        workManager.getWorkInfosForUniqueWorkFlow("updateFlowChecks"),
        workManager.getWorkInfosForUniqueWorkFlow("appChecks"),
        workManager.getWorkInfosForUniqueWorkFlow("local_to_cloud"),
        workManager.getWorkInfosForUniqueWorkFlow("cloud_to_local"),
        workManager.getWorkInfosForUniqueWorkFlow("appCleanup"),
    ) { array ->
        array
            .map { list ->
                list.map { item ->
                    WorkerInfoModel(
                        id = item.id.toString(),
                        progress = item.progress.keyValueMap,
                        status = item.state.toString(),
                        nextScheduleTimeMillis = item.nextScheduleTimeMillis.toLocalDateTime(),
                        tags = item
                            .tags
                            .map { it.removePrefix("com.programmersbox.") }
                            .toSet(),
                        isPeriodic = item.periodicityInfo != null
                    )
                }
            }.flatten()
    }

    override fun localToCloudListener(): Flow<List<WorkInfoKmp>> = workManager
        .getWorkInfosForUniqueWorkFlow("local_to_cloud")
        .map { list ->
            list.map {
                WorkInfoKmp(
                    state = it.state.toString(),
                    source = it.progress.getString("source").orEmpty(),
                    progress = it.progress.getInt("progress", 0),
                    max = it.progress.getInt("max", 0),
                    nextScheduleTimeMillis = it.nextScheduleTimeMillis.toLocalDateTime()
                )
            }
        }

    override fun cloudToLocalListener(): Flow<List<WorkInfoKmp>> = workManager
        .getWorkInfosForUniqueWorkFlow("cloud_to_local")
        .map { list ->
            list.map {
                WorkInfoKmp(
                    state = it.state.toString(),
                    source = it.progress.getString("source").orEmpty(),
                    progress = it.progress.getInt("progress", 0),
                    max = it.progress.getInt("max", 0),
                    nextScheduleTimeMillis = it.nextScheduleTimeMillis.toLocalDateTime()
                )
            }
        }

    override fun syncLocalToCloud() {
        workManager.enqueueUniqueWork(
            "local_to_cloud",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<LocalToCloudSyncWorker>()
                .build()
        )
    }

    override fun syncCloudToLocal() {
        workManager.enqueueUniqueWork(
            "cloud_to_local",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<CloudToLocalSyncWorker>()
                .build()
        )
    }

    override fun setupPeriodicCheckers() {
        AppCleanupWorker.setupWorker(workManager)

        workManager.enqueueUniquePeriodicWork(
            "appChecks",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequest.Builder(
                workerClass = AppCheckWorker::class.java,
                repeatInterval = 7,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setRequiresStorageNotLow(false)
                        .build()
                )
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build()
        )

        workManager.enqueueUniquePeriodicWork(
            "sourceChecks",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(
                workerClass = SourceUpdateChecker::class.java,
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setRequiresStorageNotLow(false)
                        .build()
                )
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build()
        )

        combine(
            dataStoreHandling
                .shouldCheck
                .asFlow()
                .distinctUntilChanged(),
            dataStoreHandling
                .updateHourCheck
                .asFlow()
                .distinctUntilChanged()
        ) { should, interval -> should to interval }
            .distinctUntilChanged()
            .onEach { check ->
                if (check.first) {
                    workManager.enqueueUniquePeriodicWork(
                        "updateFlowChecks",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        PeriodicWorkRequestBuilder<UpdateFlowWorker>(
                            check.second, TimeUnit.HOURS,
                            5, TimeUnit.MINUTES
                        )
                            .setInputData(workDataOf(UpdateFlowWorker.CHECK_ALL to false))
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build()
                            )
                            .setInitialDelay(10, TimeUnit.SECONDS)
                            .build()
                    )
                } else {
                    workManager.cancelUniqueWork("updateFlowChecks")
                }
            }
            .launchIn(GlobalScope)

        /*dataStoreHandling
            .shouldCheck
            .asFlow()
            .distinctUntilChanged()
            .onEach { check ->
                if (check) {
                    work.enqueueUniquePeriodicWork(
                        "updateFlowChecks",
                        ExistingPeriodicWorkPolicy.KEEP,
                        PeriodicWorkRequestBuilder<UpdateFlowWorker>(
                            1, TimeUnit.HOURS,
                            5, TimeUnit.MINUTES
                        )
                            .setInputData(workDataOf(UpdateFlowWorker.CHECK_ALL to false))
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build()
                            )
                            .setInitialDelay(10, TimeUnit.SECONDS)
                            .build()
                    )
                } else {
                    work.cancelUniqueWork("updateFlowChecks")
                }
            }
            .launchIn(GlobalScope)*/
    }
}