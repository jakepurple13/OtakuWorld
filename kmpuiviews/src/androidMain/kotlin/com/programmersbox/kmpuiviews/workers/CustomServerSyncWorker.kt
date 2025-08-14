package com.programmersbox.kmpuiviews.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.domain.customserver.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class CustomServerSyncWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val serverRepository: ServerRepository,
    private val listDao: ListDao,
    private val itemDao: ItemDao,
) : CoroutineWorker(context, workerParams) {
    private val dispatchers = Dispatchers.IO.limitedParallelism(5)
    override suspend fun doWork(): Result {
        val customServerHandler = serverRepository.customServerHandle.value ?: return Result.success()
        //TODO: Gotta test this
        return runCatching {
            coroutineScope {
                launch {
                    runCatching {
                        if ((listDao.getAllListsSync().size - 1).coerceAtLeast(0) == 0) {
                            val lists = customServerHandler.getAllLists()
                            lists.forEach {
                                listDao.createList(it.item)
                                it.list.forEach { listInfo ->
                                    listDao.addItem(listInfo)
                                }
                            }
                        } else {
                            listDao.getAllListsSync().forEach {
                                customServerHandler.addList(it.item)
                                it.list.forEach { listInfo ->
                                    customServerHandler.addItem(listInfo)
                                }
                            }
                        }
                    }
                }

                launch {
                    runCatching {
                        if (itemDao.getAllFavoritesSync().isEmpty()) {
                            customServerHandler.getFavorites().map { dbModel ->
                                async(dispatchers) {
                                    itemDao.insertFavorite(dbModel)
                                    customServerHandler.getChapters(dbModel).forEach {
                                        itemDao.insertChapter(it)
                                    }
                                }
                            }.awaitAll()
                        } else {
                            itemDao.getAllFavoritesSync().map {
                                async(dispatchers) {
                                    customServerHandler.addFavorite(it)
                                    itemDao.getAllChaptersSync(it.url).forEach {
                                        customServerHandler.addChapter(it)
                                    }
                                }
                            }.awaitAll()
                        }
                    }
                }
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.failure() }
        )
    }
}