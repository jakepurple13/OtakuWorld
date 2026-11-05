package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl.Companion.ManualSyncId
import dev.nucleusframework.scheduler.TaskData
import dev.nucleusframework.scheduler.testing.TestTaskRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

class WorkerRepositoryImpl : WorkRepository {
    override val manualCheck: Flow<List<WorkInfoKmp>>
        get() = emptyFlow()
    override val allWorkCheck: Flow<List<WorkInfoKmp>>
        get() = emptyFlow()

    override fun pruneWork() {

    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private var manualRun: Job? = null

    override fun checkManually() {
        manualRun = scope.launch {
            TestTaskRunner.runTask(
                SyncCheckWorker(),
                ManualSyncId,
                inputData = TaskData.of(SyncCheckWorker.SyncCheckData(cancel = true))
            )
        }
    }

    override fun retryFailedUpdates(): Boolean {
        if (manualRun?.isActive == true) return false
        manualRun = scope.launch {
            TestTaskRunner.runTask(
                SyncCheckWorker(),
                ManualSyncId,
                inputData = TaskData.of(SyncCheckWorker.SyncCheckData(cancel = true, retryFailed = true))
            )
        }
        return true
    }
}