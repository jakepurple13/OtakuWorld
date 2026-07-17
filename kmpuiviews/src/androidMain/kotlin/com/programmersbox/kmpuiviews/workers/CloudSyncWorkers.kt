package com.programmersbox.kmpuiviews.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.favoritesdatabase.ItemDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope

class LocalToCloudSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val dao: ItemDao,
) : CoroutineWorker(appContext, params) {
    private val dispatchers = Dispatchers.IO.limitedParallelism(5)
    override suspend fun doWork(): Result {
        return coroutineScope {
            runCatching {
                val allShows = dao.getAllFavoritesSync()
                allShows.mapIndexed { index, it ->
                    setProgress(
                        workDataOf(
                            "progress" to index,
                            "max" to allShows.size,
                            "source" to it.title
                        )
                    )
                }
            }.workerReturn()
        }
    }
}

class CloudToLocalSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val dao: ItemDao,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return runCatching {
            val allShows = dao.getAllFavoritesSync()
            /*val newShows = cloudShows
                .filter { allShows.any { s -> s.url != it.url } }
                .chunked(10)

            newShows.forEachIndexed { index, it ->
                setProgress(
                    workDataOf(
                        "progress" to index,
                        "max" to newShows.size,
                    )
                )
                dao.insertFavorites(*it.toTypedArray())
            }*/
        }.workerReturn()
    }
}