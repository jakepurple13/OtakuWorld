@file:OptIn(DelicateCoroutinesApi::class)

package com.programmersbox.uiviews.checkers

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.perf.trace
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.MediaUpdateChecker
import com.programmersbox.kmpuiviews.utils.printLogs
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.logFirebaseMessage
import com.programmersbox.uiviews.utils.recordFirebaseException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class UpdateFlowWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val genericInfo: GenericInfo,
    private val sourceRepository: SourceRepository,
    private val sourceLoader: SourceLoader,
    private val update: UpdateNotification,
    private val dataStoreHandling: DataStoreHandling,
    private val mediaUpdateChecker: MediaUpdateChecker,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHECK_ALL = "check_all"
    }

    override suspend fun doWork(): Result = trace("update_checker") {
        try {
            update.sendRunningNotification(100, 0, applicationContext.getString(R.string.startingCheck))
            logFirebaseMessage("Starting check here")
            dataStoreHandling.updateCheckingStart.set(System.currentTimeMillis())
            logFirebaseMessage("Start")

            val items = mediaUpdateChecker.getFavoritesThatNeedUpdates(
                checkAll = inputData.getBoolean(CHECK_ALL, false),
                putMetric = { name, value -> putMetric(name, value) },
                notificationUpdate = { max, progress, source ->
                    update.sendRunningNotification(max, progress, source)
                },
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

            update.onEnd(
                update.mapDbModel(items, genericInfo),
                info = genericInfo
            )/* { id, notification -> setForegroundInfo(id, notification) }*/
            logFirebaseMessage("Finished!")

            Result.success()
        } catch (e: Exception) {
            recordFirebaseException(e)
            dataStoreHandling.updateCheckingEnd.set(System.currentTimeMillis())
            update.sendFinishedNotification()
            Result.success()
        } finally {
            dataStoreHandling.updateCheckingEnd.set(System.currentTimeMillis())
            update.sendFinishedNotification()
            Result.success()
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
}
