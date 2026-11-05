package com.programmersbox.kmpuiviews.workers

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.perf.trace
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.domain.MediaUpdateChecker
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckMode
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckProgress
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.printLogs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.startingCheck

class UpdateFlowWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val genericInfo: PlatformGenericInfo,
    private val update: UpdateNotification,
    private val dataStoreHandling: DataStoreHandling,
    private val mediaUpdateChecker: MediaUpdateChecker,
    private val reportStore: UpdateCheckReportStore,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHECK_ALL = "check_all"
        const val RETRY_FAILED = "retry_failed"
        private const val PROGRESS_NOTIFICATION_INTERVAL_MS = 250L
    }

    override suspend fun doWork(): Result = trace("update_checker") {
        try {
            update.cancelFinishedNotification()
            //update.sendRunningNotification(100, 0, getString(Res.string.startingCheck))
            runCatching { setForeground(createStartingNotification()) }
            logFirebaseMessage("Starting check here")
            dataStoreHandling.updateCheckingStart.set(System.currentTimeMillis())
            logFirebaseMessage("Start")

            val retryFailed = inputData.getBoolean(RETRY_FAILED, false)
            val result = withProgressNotifications { onProgress ->
                mediaUpdateChecker.getFavoritesThatNeedUpdates(
                    // A retry must see every favorite, or failed items that don't notify would be dropped.
                    checkAll = retryFailed || inputData.getBoolean(CHECK_ALL, false),
                    mode = if (retryFailed) UpdateCheckMode.RetryFailed else UpdateCheckMode.Full,
                    putMetric = { name, value -> putMetric(name, value) },
                    onProgress = onProgress,
                    setProgress = { max, progress, source ->
                        setProgress(
                            workDataOf(
                                "max" to max,
                                "progress" to progress,
                                "source" to source,
                            )
                        )
                    }
                )
            }

            update.onEnd(update.mapDbModel(result.updates))/* { id, notification -> setForegroundInfo(id, notification) }*/
            logFirebaseMessage("Finished!")
            dataStoreHandling.updateCheckingEnd.set(System.currentTimeMillis())
            update.cancelProgressNotification()
            update.sendFinishedNotification(result.report)
        } catch (e: CancellationException) {
            // A cancelled run stores nothing, so there is nothing to report.
            withContext(NonCancellable) { dataStoreHandling.updateCheckingEnd.set(System.currentTimeMillis()) }
            update.cancelProgressNotification()
            throw e
        } catch (e: Exception) {
            recordFirebaseException(e)
            dataStoreHandling.updateCheckingEnd.set(System.currentTimeMillis())
            update.cancelProgressNotification()
            // The checker saved a stopped-early report before rethrowing.
            update.sendFinishedNotification(reportStore.current())
        }
        Result.success()
    }

    /**
     * Runs [block] while a separate coroutine mirrors its progress into the foreground notification.
     * The checker only publishes the latest snapshot, so it never waits on a notification update.
     * The notification refreshes at most every [PROGRESS_NOTIFICATION_INTERVAL_MS] and always catches up to the newest snapshot.
     * Returns only after the notifier has stopped, so a late update can't overwrite what is posted next.
     */
    private suspend fun <T> withProgressNotifications(
        block: suspend (onProgress: suspend (UpdateCheckProgress) -> Unit) -> T,
    ): T = coroutineScope {
        val latest = MutableStateFlow<UpdateCheckProgress?>(null)
        val notifier = launch {
            latest.filterNotNull().collect { progress ->
                runCatching { setForeground(createNotification(progress)) }
                delay(PROGRESS_NOTIFICATION_INTERVAL_MS)
            }
        }
        try {
            block { latest.value = it }
        } finally {
            notifier.cancel()
        }
    }

    //TODO: This will be tested out for now.
    // I'll see how it works.
    // If it does a good job, it'll be kept.
    private suspend fun getRecents(service: KmpApiService): List<KmpItemModel>? = runCatching {
        callbackFlow {
            var thread: Thread? = null
            val job = launch {
                delay(10000)
                printLogs { "Cancelling" }
                this@callbackFlow.cancel("Timed out")
                thread?.interrupt()
            }
            try {
                thread = Thread {
                    runCatching {
                        runBlocking {
                            val r = runCatching { service.getRecentFlow().firstOrNull() }.getOrNull()
                            job.cancel()
                            send(r)
                        }
                    }.onFailure { it.printStackTrace() }
                }

                thread.start()
            } catch (e: Exception) {
                e.printStackTrace()
                job.cancel()
                thread?.interrupt()
                send(null)
            }

            awaitClose {
                thread?.interrupt()
                job.cancel()
            }
        }.firstOrNull()
    }.getOrNull()

    override suspend fun getForegroundInfo(): ForegroundInfo = createStartingNotification()

    private suspend fun setForegroundInfo(
        id: Int,
        notification: Notification,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setForeground(
                ForegroundInfo(
                    id,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            )
        } else {
            setForeground(
                ForegroundInfo(
                    id,
                    notification,
                )
            )
        }
    }

    private val cancelIntent by lazy { WorkManager.getInstance(applicationContext).createCancelPendingIntent(id) }

    private suspend fun createStartingNotification(): ForegroundInfo = foregroundInfo(
        update.sendRunningNotificationLongTerm(
            max = 100,
            progress = 0,
            contextText = getString(Res.string.startingCheck)
        ) {
            addAction {
                actionTitle = applicationContext.getString(android.R.string.cancel)
                pendingActionIntent(cancelIntent)
            }
        }
    )

    private suspend fun createNotification(progress: UpdateCheckProgress): ForegroundInfo = foregroundInfo(
        update.buildProgressNotification(progress, cancelIntent)
    )

    private fun foregroundInfo(notification: Notification): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                UPDATE_CHECK_PROGRESS_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(UPDATE_CHECK_PROGRESS_ID, notification)
        }
}
