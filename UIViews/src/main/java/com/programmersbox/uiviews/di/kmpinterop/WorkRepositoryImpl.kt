package com.programmersbox.uiviews.di.kmpinterop

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.hasKeyWithValueOfType
import androidx.work.workDataOf
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.kmpuiviews.repository.WorkRepository
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import com.programmersbox.kmpuiviews.workers.UpdateFlowWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class WorkRepositoryImpl(
    context: Context,
) : WorkRepository {

    private val workManager by lazy {
        WorkManager.getInstance(context)
    }

    override val manualCheck: Flow<List<WorkInfoKmp>>
        get() = combine(
            workManager.getWorkInfosByTagFlow("ManualCheck"),
            workManager.getWorkInfosForUniqueWorkFlow("oneTimeUpdate"),
            workManager.getWorkInfosFlow(WorkQuery.fromUniqueWorkNames("oneTimeUpdate")),
            workManager.getWorkInfosFlow(WorkQuery.fromTags("ManualCheck"))
        ) { workers -> workers.flatMap { it }.distinctBy { it.id } }
            .map { list -> list.map { it.toWorkInfoKmp() } }

    override val allWorkCheck: Flow<List<WorkInfoKmp>>
        get() = workManager
            .getWorkInfosForUniqueWorkFlow("updateFlowChecks")
            .map { list -> list.map { it.toWorkInfoKmp() } }

    private fun WorkInfo.toWorkInfoKmp(): WorkInfoKmp {
        val (progressProgress, max) = if (
            progress.hasKeyWithValueOfType<Int>("progress") &&
            progress.hasKeyWithValueOfType<Int>("max")
        ) {
            progress.getInt("progress", 0) to progress.getInt("max", 0)
        } else null to null

        return WorkInfoKmp(
            state = state.toString(),
            source = progress.getString("source").orEmpty(),
            progress = progressProgress,
            max = max,
            nextScheduleTimeMillis = nextScheduleTimeMillis.toLocalDateTime()
        )
    }

    override fun pruneWork() {
        workManager.cancelUniqueWork("updateFlowChecks")
        workManager.pruneWork()
    }

    override fun checkManually() {
        workManager.enqueueUniqueWork(
            "oneTimeUpdate",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<UpdateFlowWorker>()
                .setInputData(workDataOf(UpdateFlowWorker.CHECK_ALL to true))
                .addTag("ManualCheck")
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
}