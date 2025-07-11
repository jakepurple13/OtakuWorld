package com.programmersbox.kmpuiviews.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.workDataOf
import com.programmersbox.datastore.MediaCheckerNetworkType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import com.programmersbox.kmpuiviews.workers.AppCheckWorker
import com.programmersbox.kmpuiviews.workers.AppCleanupWorker
import com.programmersbox.kmpuiviews.workers.CloudToLocalSyncWorker
import com.programmersbox.kmpuiviews.workers.LocalToCloudSyncWorker
import com.programmersbox.kmpuiviews.workers.SourceUpdateChecker
import com.programmersbox.kmpuiviews.workers.UpdateFlowWorker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.UUID
import java.util.concurrent.TimeUnit

class BackgroundWorkHandlerImpl(
    context: Context,
    private val settingsHandling: NewSettingsHandling,
) : BackgroundWorkHandler {

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = combine(
        workManager.getWorkInfosForUniqueWorkFlow("sourceChecks"),
        workManager.getWorkInfosForUniqueWorkFlow("updateFlowChecks"),
        workManager.getWorkInfosForUniqueWorkFlow("appChecks"),
        workManager.getWorkInfosForUniqueWorkFlow("local_to_cloud"),
        workManager.getWorkInfosForUniqueWorkFlow("cloud_to_local"),
        workManager.getWorkInfosForUniqueWorkFlow("appCleanup"),
        workManager.getWorkInfosFlow(WorkQuery.fromTags("ManualCheck", "oneTimeUpdate", "downloadAndInstall"))
    ) { array ->
        array
            .map { workers -> workers.distinctBy { it.id } }
            .map { list ->
                list.map { item ->
                    WorkerInfoModel(
                        id = item.id.toString(),
                        progress = item.progress.keyValueMap,
                        status = item.state.toString(),
                        nextScheduleTimeMillis = item.nextScheduleTimeMillis.toLocalDateTime(),
                        isPeriodic = item.periodicityInfo != null,
                        tags = item
                            .tags
                            .map { it.removePrefix("com.programmersbox.") }
                            .toSet(),
                        workerName = item
                            .tags
                            .find { it.contains("com.programmersbox.") }
                            ?: "Worker"
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

    @OptIn(DelicateCoroutinesApi::class)
    override fun setupPeriodicCheckers() {
        AppCleanupWorker.setupWorker(workManager)

        workManager.enqueueUniquePeriodicWork(
            "appChecks",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<AppCheckWorker>(
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
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SourceUpdateChecker>(
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

        settingsHandling
            .mediaCheckerSettings
            .asFlow()
            .onEach {
                if (it.shouldRun) {
                    workManager.enqueueUniquePeriodicWork(
                        "updateFlowChecks",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        PeriodicWorkRequestBuilder<UpdateFlowWorker>(
                            it.interval, TimeUnit.HOURS,
                            5, TimeUnit.MINUTES
                        )
                            .setInputData(workDataOf(UpdateFlowWorker.CHECK_ALL to false))
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(
                                        when (it.networkType) {
                                            MediaCheckerNetworkType.Connected -> NetworkType.CONNECTED
                                            MediaCheckerNetworkType.Metered -> NetworkType.METERED
                                            MediaCheckerNetworkType.Unmetered -> NetworkType.UNMETERED
                                        }
                                    )
                                    .setRequiresCharging(it.requiresCharging)
                                    .setRequiresBatteryNotLow(it.requiresBatteryNotLow)
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

        /*combine(
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
                                    // metered is mobile data
                                    // unmetered is wifi only
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .setRequiresCharging(false) //default is false
                                    .setRequiresBatteryNotLow(false) //default is false
                                    .build()
                            )
                            .setInitialDelay(10, TimeUnit.SECONDS)
                            .build()
                    )
                } else {
                    workManager.cancelUniqueWork("updateFlowChecks")
                }
            }
            .launchIn(GlobalScope)*/

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

    override fun sourceUpdate() {
        workManager
            .enqueueUniqueWork(
                "sourceCheck",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SourceUpdateChecker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(false)
                            .setRequiresCharging(false)
                            .setRequiresDeviceIdle(false)
                            .setRequiresStorageNotLow(false)
                            .build()
                    )
                    .build()
            )
    }

    override fun cancel(uuid: String) {
        workManager.cancelWorkById(UUID.fromString(uuid))
    }
}