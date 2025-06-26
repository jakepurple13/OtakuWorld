package com.programmersbox.uiviews.checkers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull

class LocalToCloudSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val dao: ItemDao,
    private val kmpFirebaseConnection: KmpFirebaseConnection,
) : CoroutineWorker(appContext, params) {
    private val dispatchers = Dispatchers.IO.limitedParallelism(5)
    override suspend fun doWork(): Result {
        return coroutineScope {
            runCatching {
                val allShows = dao.getAllFavoritesSync()
                val cloudShows = kmpFirebaseConnection.getAllShows()
                val newShows = allShows.filter { cloudShows.any { s -> s.url != it.url } }
                newShows.mapIndexed { index, it ->
                    setProgress(
                        workDataOf(
                            "progress" to index,
                            "max" to newShows.size,
                            "source" to it.title
                        )
                    )
                    async(dispatchers, start = CoroutineStart.LAZY) {
                        kmpFirebaseConnection
                            .insertShowFlow(it)
                            .firstOrNull()
                    }
                }.awaitAll()
            }.fold(
                onSuccess = { Result.success() },
                onFailure = { Result.failure() }
            )
        }
    }
}

class CloudToLocalSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val dao: ItemDao,
    private val kmpFirebaseConnection: KmpFirebaseConnection,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return runCatching {
            val allShows = dao.getAllFavoritesSync()
            val cloudShows = kmpFirebaseConnection.getAllShows()
            val newShows = cloudShows.filter { allShows.any { s -> s.url != it.url } }
            newShows.forEachIndexed { index, it ->
                setProgress(
                    workDataOf(
                        "progress" to index,
                        "max" to newShows.size,
                        "source" to it.title
                    )
                )
                dao.insertFavorite(it)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.failure() }
        )
    }
}