package com.programmersbox.kmpuiviews.domain

import androidx.compose.ui.util.fastMaxBy
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class MediaUpdateChecker(
    private val dao: ItemDao,
    private val sourceRepository: SourceRepository,
    private val sourceLoader: SourceLoader,
    private val firebaseDb: KmpFirebaseConnection,
) {

    suspend fun getFavoritesThatNeedUpdates(
        checkAll: Boolean,
        putMetric: suspend (name: String, value: Long) -> Unit,
        notificationUpdate: suspend (max: Int, progress: Int, source: String) -> Unit,
        setProgress: suspend (max: Int, progress: Int, source: String) -> Unit,
    ): List<Pair<KmpInfoModel?, DbModel>> {
        val list = listOf(
            if (checkAll) dao.getAllFavoritesSync() else dao.getAllNotifyingFavoritesSync(),
            firebaseDb.getAllShows().requireNoNulls()
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
            sourceRepository
                .apiServiceList
                .filter { s -> list.any { m -> m.source == s.serviceName } }
                .mapIndexedNotNull { index, m ->
                    logFirebaseMessage("Checking ${m.serviceName}")
                    //TODO: Make this easier to handle
                    setProgress(sourceSize, index, m.serviceName)
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
            notificationUpdate(newList.size, index, model.title)
            setProgress(newList.size, index, model.title)
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
        items.forEach { (first, second) ->
            second.numChapters = first?.chapters?.size ?: second.numChapters
            dao.insertFavorite(second)
            firebaseDb.updateShowFlow(second)
                .catch {
                    recordFirebaseException(it)
                    println("Something went wrong: ${it.message}")
                }
                .collect()
        }

        return items
    }

    private fun <T, R> Iterable<T>.intersect(uList: Iterable<R>, filterPredicate: (T, R) -> Boolean) =
        filter { m -> uList.any { filterPredicate(m, it) } }
}