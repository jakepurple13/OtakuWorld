@file:OptIn(DelicateCoroutinesApi::class)

package com.programmersbox.uiviews.checkers

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMaxBy
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.perf.trace
import com.programmersbox.extensionloader.SourceLoader
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.helpfulutils.intersect
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.datastore.DataStoreHandling
import com.programmersbox.uiviews.utils.logFirebaseMessage
import com.programmersbox.uiviews.utils.recordFirebaseException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class UpdateFlowWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val genericInfo: GenericInfo,
    private val sourceRepository: SourceRepository,
    private val sourceLoader: SourceLoader,
    private val update: UpdateNotification,
    database: ItemDatabase,
    private val dataStoreHandling: DataStoreHandling,
) : CoroutineWorker(context, workerParams) {

    private val dao = database.itemDao()

    companion object {
        const val CHECK_ALL = "check_all"
    }

    override suspend fun doWork(): Result = trace("update_checker") {
        try {
            update.sendRunningNotification(100, 0, applicationContext.getString(R.string.startingCheck))
            logFirebaseMessage("Starting check here")
            dataStoreHandling.updateCheckingStart.set(System.currentTimeMillis())

            logFirebaseMessage("Start")

            val checkAll = inputData.getBoolean(CHECK_ALL, false)

            //Getting all favorites
            val list = listOf(
                if (checkAll) dao.getAllFavoritesSync() else dao.getAllNotifyingFavoritesSync(),
                FirebaseDb.getAllShows().requireNoNulls()
                    .let { firebase -> if (checkAll) firebase else firebase.filter { it.shouldCheckForUpdate } }
            )
                .flatten()
                .groupBy(DbModel::url)
                .map { it.value.fastMaxBy(DbModel::numChapters)!! }

            //Making sure we have our sources
            if (sourceRepository.list.isEmpty()) {
                sourceLoader.blockingLoad()
            }

            logFirebaseMessage("Sources: ${sourceRepository.apiServiceList.joinToString { it.serviceName }}")

            val sourceSize = sourceRepository.apiServiceList.size

            putMetric("sourceSize", sourceSize.toLong())

            // Getting all recent updates
            val newList = list.intersect(
                sourceRepository.apiServiceList
                    .filter { s -> list.fastAny { m -> m.source == s.serviceName } }
                    .mapIndexedNotNull { index, m ->
                        logFirebaseMessage("Checking ${m.serviceName}")
                        //TODO: Make this easier to handle
                        setProgress(
                            workDataOf(
                                "max" to sourceSize,
                                "progress" to index,
                                "source" to m.serviceName,
                            )
                        )
                        runCatching {
                            withTimeoutOrNull(10000) {
                                m.getRecentFlow()
                                    .catch { emit(emptyList()) }
                                    .firstOrNull()
                            }
                            /*withTimeoutOrNull(10000) { m.getRecentFlow().firstOrNull() }*/
                            //getRecents(m)
                        }
                            .onFailure {
                                recordFirebaseException(it)
                                it.printStackTrace()
                            }
                            .getOrNull()
                            .also { logFirebaseMessage("Finished checking ${m.serviceName} with ${it?.size}") }
                    }.flatten()
            ) { o, n -> o.url == n.url }
                .distinctBy { it.url }

            putMetric("updateCheckSize", newList.size.toLong())

            // Checking if any have updates
            println("Checking for updates")
            val items = newList.mapIndexedNotNull { index, model ->
                update.sendRunningNotification(newList.size, index, model.title)
                setProgress(
                    workDataOf(
                        "max" to newList.size,
                        "progress" to index,
                        "source" to model.title,
                    )
                )
                runCatching {
                    val newData = sourceRepository.toSourceByApiServiceName(model.source)
                        ?.apiService
                        ?.let {
                            withTimeout(10000) {
                                model.toItemModel(it)
                                    .toInfoModel()
                                    .firstOrNull()
                                    ?.getOrNull()
                            }
                        }
                    logFirebaseMessage("Old: ${model.numChapters} New: ${newData?.chapters?.size}")
                    // To test notifications, comment the takeUnless out
                    Pair(newData, model)
                        .takeUnless { it.second.numChapters >= (it.first?.chapters?.size ?: -1) }
                }
                    .onFailure {
                        recordFirebaseException(it)
                        it.printStackTrace()
                    }
                    .getOrNull()
            }

            // Saving updates
            update.updateManga(dao, items)
            update.onEnd(
                update.mapDbModel(dao, items, genericInfo),
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
    private suspend fun getRecents(service: ApiService): List<ItemModel>? = runCatching {
        callbackFlow {
            var thread: Thread? = null
            val job = launch {
                delay(10000)
                println("Cancelling")
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
