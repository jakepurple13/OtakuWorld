package com.programmersbox.kmpuiviews.domain

import androidx.compose.ui.util.fastMaxBy
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.hadAnUpdate

class MediaUpdateChecker(
    private val dao: ItemDao,
    private val sourceRepository: SourceRepository,
    private val sourceLoader: SourceLoader,
    private val firebaseDb: KmpFirebaseConnection,
) {

    suspend fun getFavoritesThatNeedUpdates2(
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

        val dispatcher = Dispatchers.IO.limitedParallelism(3)

        // Getting all recent updates
        val newList = coroutineScope {
            list.intersect(
                list
                    .groupBy { it.source }
                    .keys
                    .mapNotNull {
                        sourceRepository
                            .apiServiceList
                            .find { s -> s.serviceName == it }
                    }
                    .mapIndexed { index, m ->
                        //TODO: Test this out
                        // The setProgress *might* cause some problems but meh
                        setProgress(sourceSize, index, m.serviceName)
                        async(dispatcher, start = CoroutineStart.LAZY) {
                            logFirebaseMessage("Checking ${m.serviceName}")
                            //TODO: Make this easier to handle
                            runCatching {
                                withTimeoutOrNull(15000) {
                                    m.getRecentFlow()
                                        .catch { emit(emptyList()) }
                                        .firstOrNull()
                                }
                            }
                                .onFailure {
                                    logFirebaseMessage(it.stackTraceToString())
                                    it.printStackTrace()
                                }
                                .getOrNull()
                                .also { logFirebaseMessage("Finished checking ${m.serviceName} with ${it?.size}") }
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
                    .flatten()
            ) { o, n -> o.url == n.url }
                .distinctBy { it.url }
        }

        putMetric("updateCheckSize", newList.size.toLong())

        // Checking if any have updates
        println("Checking for updates")
        val items = coroutineScope {
            newList.mapIndexed { index, model ->
                //TODO: Test this out
                // The setProgress *might* cause some problems but meh
                notificationUpdate(newList.size, index, model.title)
                setProgress(newList.size, index, model.title)
                async(dispatcher, start = CoroutineStart.LAZY) {
                    runCatching {
                        val newData = sourceRepository.toSourceByApiServiceName(model.source)
                            ?.apiService
                            ?.let {
                                withTimeout(15000) {
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
                            logFirebaseMessage(it.stackTraceToString())
                            it.printStackTrace()
                        }
                        .getOrNull()
                }
            }
                .awaitAll()
                .filterNotNull()
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

    suspend fun getFavoritesThatNeedUpdates1(
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
                        //getRecents(m)
                    }
                        .onFailure {
                            logFirebaseMessage(it.stackTraceToString())
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
                    logFirebaseMessage(it.stackTraceToString())
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

    suspend fun mapDbModel(
        list: List<Pair<KmpInfoModel?, DbModel>>,
        notificationUpdate: suspend (max: Int, progress: Int, source: String) -> Unit,
    ) = list.mapIndexed { index, pair ->
        notificationUpdate(list.size, index, pair.second.title)
        val item = dao.getNotificationItem(pair.second.url)
        val isShowing = item?.isShowing == true

        val notificationId = if (isShowing)
            item.id
        else
            pair.second.hashCode()

        dao.insertNotification(
            NotificationItem(
                id = notificationId,
                url = pair.second.url,
                summaryText = getString(
                    Res.string.hadAnUpdate,
                    pair.second.title,
                    pair.first?.chapters?.firstOrNull()?.name ?: ""
                ),
                notiTitle = pair.second.title,
                imageUrl = pair.second.imageUrl,
                source = pair.second.source,
                contentTitle = pair.second.title,
                isShowing = true
            )
        )

        UpdateModel(
            notificationId = notificationId,
            infoModel = pair.first,
            dbModel = pair.second
        )
    }


    private fun <T, R> Iterable<T>.intersect(uList: Iterable<R>, filterPredicate: (T, R) -> Boolean) =
        filter { m -> uList.any { filterPredicate(m, it) } }

    // Consider making this configurable or dynamic based on available cores
    private val networkDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(5)

    //TODO: Test
    suspend fun getFavoritesThatNeedUpdates(
        checkAll: Boolean,
        putMetric: suspend (name: String, value: Long) -> Unit,
        notificationUpdate: suspend (max: Int, progress: Int, source: String) -> Unit,
        setProgress: suspend (max: Int, progress: Int, source: String) -> Unit,
    ): List<Pair<KmpInfoModel?, DbModel>> = coroutineScope { // Use coroutineScope for structured concurrency
        // 1. Consolidate and Deduplicate Favorites
        val favoriteItems = getConsolidatedFavoriteItems(checkAll)

        val sourcesToCheckRecentsFrom = favoriteItems
            .groupBy { it.source }
            .keys
            .mapNotNull {
                sourceRepository
                    .apiServiceList
                    .find { s -> s.serviceName == it }
            }

        // 2. Ensure Sources are Loaded
        ensureSourcesLoaded()

        logFirebaseMessage("Sources: ${sourceRepository.apiServiceList.joinToString { it.serviceName }}")
        val sourceSize = sourceRepository.apiServiceList.size
        putMetric("sourceSize", sourceSize.toLong())

        // 3. Fetch Recent Updates from All Sources Concurrently
        val allRecentItemsFromSources = fetchRecentItemsFromAllSources(
            sourcesToCheckRecentsFrom,
            setProgress
        )

        // 4. Intersect Favorites with Recent Items
        val itemsToCheckForUpdates = favoriteItems
            .intersect(allRecentItemsFromSources.toSet()) { fav, recent -> fav.url == recent.url } // Use Set for efficient lookup
            .distinctBy { it.url } // Should be redundant if intersect is correct, but good safety

        putMetric("updateCheckSize", itemsToCheckForUpdates.size.toLong())
        logFirebaseMessage("Found ${itemsToCheckForUpdates.size} items to check for updates.")

        // 5. Check Individual Items for Updates Concurrently
        val updatedItemsPairs = checkItemsForActualUpdates(itemsToCheckForUpdates, notificationUpdate, setProgress)

        // 6. Save Updates to Database
        saveUpdatedItems(updatedItemsPairs)

        updatedItemsPairs
    }

    private suspend fun getConsolidatedFavoriteItems(checkAll: Boolean): List<DbModel> {
        val localFavorites = if (checkAll) dao.getAllFavoritesSync() else dao.getAllNotifyingFavoritesSync()
        val firebaseFavorites = firebaseDb.getAllShows().requireNoNulls()
            .let { firebase -> if (checkAll) firebase else firebase.filter { it.shouldCheckForUpdate } }

        return (localFavorites + firebaseFavorites)
            .groupBy(DbModel::url)
            .mapNotNull { it.value.fastMaxBy(DbModel::numChapters) } // mapNotNull in case fastMaxBy returns null for an empty list
    }

    private suspend fun ensureSourcesLoaded() {
        if (sourceRepository.list.isEmpty()) {
            // Consider if blockingLoad is truly necessary or if it can be done asynchronously earlier
            // If it must be blocking here, ensure this function is called from a dispatcher that allows blocking
            withContext(Dispatchers.IO) { // Explicitly switch to IO for blocking operations
                sourceLoader.blockingLoad()
            }
        }
    }

    private suspend fun fetchRecentItemsFromAllSources(
        sourcesToCheckRecentsFrom: List<KmpApiService>,
        setProgress: suspend (max: Int, progress: Int, source: String) -> Unit,
    ): List<KmpItemModel> = coroutineScope {
        sourcesToCheckRecentsFrom
            .mapIndexed { index, sourceApiService ->
                setProgress(sourcesToCheckRecentsFrom.size, index + 1, sourceApiService.serviceName) // Progress 1 to N
                async(networkDispatcher, start = CoroutineStart.LAZY) { // Use LAZY to control start
                    logFirebaseMessage("Fetching recent items from ${sourceApiService.serviceName}")
                    try {
                        withTimeoutOrNull(15000) { // 15-second timeout per source
                            sourceApiService
                                .getRecentFlow()
                                // Explicitly ensure flow collection happens on networkDispatcher
                                // if getRecentFlow itself doesn't specify its context.
                                // If getRecentFlow internally uses withContext(Dispatchers.IO) or similar,
                                // this flowOn might be redundant but harmless.
                                .flowOn(networkDispatcher)
                                .catch { e ->
                                    logFirebaseMessage("Error fetching from ${sourceApiService.serviceName}: ${e.message}")
                                    recordFirebaseException(e)
                                    emit(emptyList()) // Emit empty list on error to not break the chain
                                }
                                .firstOrNull() // Takes the first emission (should be a List<DbModel>)
                        } ?: emptyList() // Return empty list if timeout occurs
                    } catch (e: Exception) {
                        logFirebaseMessage("Exception during recent fetch for ${sourceApiService.serviceName}: ${e.message}")
                        recordFirebaseException(e)
                        emptyList()
                    }
                }
            }
            .awaitAll() // Filter out nulls from timeouts or explicit null returns
            .flatten() // Flatten List<List<DbModel>> to List<DbModel>
    }

    private suspend fun checkItemsForActualUpdates(
        items: List<DbModel>,
        notificationUpdate: suspend (max: Int, progress: Int, source: String) -> Unit,
        setProgress: suspend (max: Int, progress: Int, source: String) -> Unit,
    ): List<Pair<KmpInfoModel, DbModel>> = coroutineScope {
        items.mapIndexed { index, model ->
            notificationUpdate(items.size, index + 1, model.title)
            setProgress(items.size, index + 1, model.title) // Progress 1 to N

            async(networkDispatcher, start = CoroutineStart.LAZY) {
                logFirebaseMessage("Checking for update: ${model.title} from ${model.source}")
                try {
                    val apiService = sourceRepository
                        .toSourceByApiServiceName(model.source)
                        ?.apiService
                    if (apiService == null) {
                        logFirebaseMessage("Source API service not found for ${model.source}")
                        return@async null
                    }

                    val infoModel = withTimeout(15000) { // 15-second timeout for fetching full info
                        // Explicitly switch context if toInfoModel might block or do heavy CPU work
                        // However, network operations are usually fine on networkDispatcher
                        model
                            .toItemModel(apiService) // This likely involves network I/O
                            .toInfoModel() // This might involve more I/O or CPU work
                            .firstOrNull()
                            ?.getOrNull() // Assuming toInfoModel returns Flow<Result<KmpInfoModel>>
                    }

                    if (infoModel != null) {
                        logFirebaseMessage("Old chapters: ${model.numChapters}, New chapters: ${infoModel.chapters.size} for ${model.title}")
                        if (infoModel.chapters.size > model.numChapters) {
                            Pair(infoModel, model)
                        } else {
                            null // No update needed
                        }
                    } else {
                        logFirebaseMessage("Failed to fetch KmpInfoModel for ${model.title}")
                        null
                    }
                } catch (e: Exception) {
                    logFirebaseMessage("Error checking update for ${model.title}: ${e.message}")
                    recordFirebaseException(e)
                    null
                }
            }.also { it.start() } // Start the async task immediately after creation if not LAZY or control start if needed. Or awaitAll will start them.
        }
            .awaitAll()
            .filterNotNull() // Filter out nulls (items with no updates or errors)
    }

    private suspend fun saveUpdatedItems(updatedItems: List<Pair<KmpInfoModel, DbModel>>) {
        // Perform database operations on a dedicated IO dispatcher
        withContext(Dispatchers.IO) {
            updatedItems.forEach { (infoModel, dbModel) ->
                val oldNumChapters = dbModel.numChapters
                dbModel.numChapters = infoModel.chapters.size // infoModel is guaranteed to be non-null here
                try {
                    dao.insertFavorite(dbModel) // This should be an update if the item exists
                    logFirebaseMessage("Saved update for ${dbModel.title}. Chapters: $oldNumChapters -> ${dbModel.numChapters}")

                    // firebaseDb.updateShowFlow might also be suspending and I/O bound
                    firebaseDb.updateShowFlow(dbModel)
                        .catch { e ->
                            recordFirebaseException(e)
                            logFirebaseMessage("Firebase update failed for ${dbModel.title}: ${e.message}")
                        }
                        .collect() // Assuming collect is necessary to trigger the update
                } catch (e: Exception) {
                    recordFirebaseException(e)
                    logFirebaseMessage("Database save failed for ${dbModel.title}: ${e.message}")
                    // Potentially revert dbModel.numChapters if the save fails and you need atomicity
                }
            }
        }
    }

    // Keep this utility if used elsewhere, or inline if only used once.
    // Making it more generic:
    private fun <T, R> Iterable<T>.intersect(
        other: Set<R>, // Changed to Set for O(1) average time complexity for lookups
        filterPredicate: (T, R) -> Boolean,
    ): List<T> {
        val result = mutableListOf<T>()
        // Optimized: Iterate over the smaller collection if possible, though here 'this' is iterated.
        for (itemFromThis in this) {
            // This part can be slow if 'other' is a large List (O(N*M)).
            // With 'other' as a Set and if filterPredicate involves hashing or direct comparison on `R`'s key,
            // it becomes much faster.
            // However, the predicate `fav.url == recent.url` implies we need to check each element.
            // The best way for this specific predicate is to convert `other` to a `Map<String, R>` or `Set<String>` of URLs.
            // Let's assume for now that the caller might convert `allRecentItemsFromSources` to a Set of URLs if performance is critical.
            if (other.any { itemFromOther -> filterPredicate(itemFromThis, itemFromOther) }) {
                result.add(itemFromThis)
            }
        }
        return result
    }
}

data class UpdateModel(
    val notificationId: Int,
    val infoModel: KmpInfoModel?,
    val dbModel: DbModel,
)
