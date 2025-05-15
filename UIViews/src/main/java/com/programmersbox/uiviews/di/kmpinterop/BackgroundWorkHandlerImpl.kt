package com.programmersbox.uiviews.di.kmpinterop

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import com.programmersbox.uiviews.checkers.CloudToLocalSyncWorker
import com.programmersbox.uiviews.checkers.LocalToCloudSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BackgroundWorkHandlerImpl(
    context: Context,
) : BackgroundWorkHandler {

    private val workManager by lazy { WorkManager.getInstance(context) }

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
}