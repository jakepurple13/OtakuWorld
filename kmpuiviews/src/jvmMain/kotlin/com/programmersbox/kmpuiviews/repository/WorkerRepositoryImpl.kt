package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl.Companion.ManualSyncId
import dev.nucleusframework.scheduler.TaskData
import dev.nucleusframework.scheduler.TaskResult
import dev.nucleusframework.scheduler.testing.TestTaskRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

class WorkerRepositoryImpl(
    private val tracker: DesktopManualCheckTracker,
) : WorkRepository {
    override val manualCheck: Flow<List<WorkInfoKmp>>
        get() = tracker.state
    override val allWorkCheck: Flow<List<WorkInfoKmp>>
        get() = emptyFlow()

    override fun pruneWork() {

    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun checkManually() {
        run(SyncCheckWorker.SyncCheckData(cancel = true))
    }

    override fun retryFailedUpdates(): Boolean =
        run(SyncCheckWorker.SyncCheckData(cancel = true, retryFailed = true))

    private fun run(data: SyncCheckWorker.SyncCheckData): Boolean {
        if (!tracker.tryStart()) return false
        scope.launch {
            try {
                val result = TestTaskRunner.runTask(
                    SyncCheckWorker(),
                    ManualSyncId,
                    inputData = TaskData.of(data)
                )
                tracker.finish(result !is TaskResult.Failure)
            } catch (e: Exception) {
                tracker.finish(false)
            }
        }
        return true
    }
}
