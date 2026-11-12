package com.programmersbox.kmpuiviews.domain

import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.updatecheck.FailureInfo
import com.programmersbox.kmpuiviews.domain.updatecheck.ItemFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.SourceFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.SourceProgress
import com.programmersbox.kmpuiviews.domain.updatecheck.SourceState
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckMode
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckProgress
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckResult
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdatedItem
import com.programmersbox.kmpuiviews.domain.updatecheck.chapterRange
import com.programmersbox.kmpuiviews.domain.updatecheck.classifyFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.mergeRetry
import com.programmersbox.kmpuiviews.domain.updatecheck.newChapters
import com.programmersbox.kmpuiviews.domain.updatecheck.updateTitle
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.recordFirebaseException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class MediaUpdateChecker(
    private val dao: ItemDao,
    private val sourceRepository: SourceRepository,
    private val sourceLoader: SourceLoader,
    private val exceptionDao: ExceptionDao,
    private val reportStore: UpdateCheckReportStore,
) {

    // Configurable network dispatcher based on the provided parallelism value
    private val networkDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(DEFAULT_NETWORK_PARALLELISM)

    companion object {
        private const val DEFAULT_NETWORK_PARALLELISM = 5
        private const val DEFAULT_TIMEOUT_MS = 15000L // 15 seconds
    }

    /**
     * Main implementation for checking favorites that need updates.
     *
     * Reports per-source progress and failures through [onProgress] and stores an [UpdateCheckReport] for the run.
     * A cancelled run stores nothing; a run that crashes stores what it has with [UpdateCheckReport.stoppedEarly] set.
     *
     * @param checkAll Whether to check all favorites or only those with notifications enabled
     * @param mode [UpdateCheckMode.RetryFailed] only checks the failed items of the stored report and merges the
     * result into it; it does nothing when there is no stored report or the report has no failures
     * @param putMetric Function to record metrics
     * @param onProgress Called with a new snapshot after every progress change
     * @param setProgress Function to update overall progress
     * @return The updated items and this run's own report (before any retry merge)
     */
    suspend fun getFavoritesThatNeedUpdates(
        checkAll: Boolean,
        mode: UpdateCheckMode = UpdateCheckMode.Full,
        putMetric: suspend (name: String, value: Long) -> Unit,
        onProgress: suspend (UpdateCheckProgress) -> Unit = {},
        setProgress: suspend (max: Int, progress: Int, source: String) -> Unit,
    ): UpdateCheckResult {
        val previous = if (mode == UpdateCheckMode.RetryFailed) {
            reportStore.current()
                ?.takeIf { it.itemFailures.isNotEmpty() || it.sourceFailures.isNotEmpty() }
                ?: return UpdateCheckResult(emptyList(), null)
        } else {
            null
        }
        val onlyUrls = previous?.itemFailures?.mapTo(HashSet()) { it.url }

        val run = CheckRun(onProgress)

        suspend fun save(report: UpdateCheckReport) {
            if (previous != null && onlyUrls != null) {
                // A full check that finished while retrying has replaced the report; don't overwrite it.
                val latest = reportStore.current()?.takeIf { it.startedAt == previous.startedAt } ?: return
                reportStore.save(
                    latest.mergeRetry(
                        retry = report,
                        retriedUrls = onlyUrls,
                        retriedSources = previous.itemFailures.mapTo(HashSet()) { it.source },
                    )
                )
            } else {
                reportStore.save(report)
            }
        }

        return try {
            // 1. Ensure Sources are Loaded (skipped extensions resolve to their sources)
            ensureSourcesLoaded()

            // 2. Consolidate Favorites (only the failed ones when retrying)
            val favoriteItems = getConsolidatedFavoriteItems(checkAll)
                .let { all -> if (onlyUrls == null) all else all.filter { it.url in onlyUrls } }

            logFirebaseMessage("Sources: ${sourceRepository.apiServiceList.joinToString { it.serviceName }}")
            val sourceSize = sourceRepository.apiServiceList.size
            putMetric("sourceSize", sourceSize.toLong())

            // 3. Fetch Recent Updates from only sources in our favorite's Sources Concurrently
            val favoritesBySource = favoriteItems.groupBy { it.source }
            val sourcesToCheckRecentsFrom = favoritesBySource
                .keys
                .mapNotNull {
                    sourceRepository
                        .apiServiceList
                        .find { s -> s.serviceName == it }
                }

            run.start(
                favoriteCount = favoriteItems.size,
                sources = sourcesToCheckRecentsFrom.map {
                    SourceProgress(
                        name = sourceRepository.displayNameFor(it.serviceName),
                        state = SourceState.Pending,
                        favorites = favoritesBySource[it.serviceName].orEmpty().size,
                        toCheck = 0,
                        checked = 0,
                        updates = 0,
                        failure = null,
                    )
                }
            )

            // 4. Intersect Favorites with Recent Items, per source
            val itemsToCheckForUpdates = fetchRecentItemsFromAllSources(
                sourcesToCheckRecentsFrom = sourcesToCheckRecentsFrom,
                favoritesBySource = favoritesBySource,
                run = run,
                setProgress = setProgress
            )
            run.recentsLoaded(itemsToCheckForUpdates.size)

            putMetric("updateCheckSize", itemsToCheckForUpdates.size.toLong())
            logFirebaseMessage("Found ${itemsToCheckForUpdates.size} items to check for updates.")

            // 5. Check Individual Items for Updates Concurrently
            val updatedItems = checkItemsForActualUpdates(
                items = itemsToCheckForUpdates,
                run = run,
                setProgress = setProgress
            )

            // 6. Save Updates to Database
            saveUpdatedItems(updatedItems)

            val report = run.report(stoppedEarly = null)
            save(report)
            UpdateCheckResult(updatedItems, report)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            save(run.report(stoppedEarly = e.message ?: e::class.simpleName))
            throw e
        }
    }

    /**
     * Maps updated items to notification models for display.
     *
     * @param list The updated items
     * @param notificationUpdate Function to update notification progress
     * @return List of update models for notifications
     */
    suspend fun mapDbModel(
        list: List<UpdatedItem>,
        unit: UpdateUnit = UpdateUnit.Chapters,
        notificationUpdate: suspend (max: Int, progress: Int, source: String) -> Unit,
    ) = list.mapIndexed { index, updated ->
        val dbModel = updated.dbModel
        notificationUpdate(list.size, index, dbModel.title)
        val item = dao.getNotificationItem(dbModel.url)
        val isShowing = item?.isShowing == true

        val notificationId = if (isShowing)
            item.id
        else
            dbModel.hashCode()

        // Replace any existing row so a repeat update refreshes createdAt and the summary
        dao.upsertNotification(
            NotificationItem(
                id = notificationId,
                url = dbModel.url,
                summaryText = "${updateTitle(dbModel.title, updated.newChapters.size, unit)}: ${chapterRange(updated.newChapters)}",
                notiTitle = dbModel.title,
                imageUrl = dbModel.imageUrl,
                source = dbModel.source,
                contentTitle = dbModel.title,
                isShowing = true,
                supabaseId = item?.supabaseId
            )
        )

        UpdateModel(
            notificationId = notificationId,
            item = updated,
        )
    }

    private suspend fun getConsolidatedFavoriteItems(checkAll: Boolean): List<DbModel> {
        val skippedPackages = dao.getSkippedUpdateSources().first().toSet()
        val skippedServices = sourceRepository.list
            .filter { it.packageName in skippedPackages }
            .map { it.apiService.serviceName }
        return dao.getFavoritesToCheckSync(checkAll, skippedServices)
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

    /**
     * Fetches each source's recent list and returns the favorites found in it, paired with the source's index.
     * A source whose recent list fails is marked failed and all of its favorites are recorded as not checked.
     */
    private suspend fun fetchRecentItemsFromAllSources(
        sourcesToCheckRecentsFrom: List<KmpApiService>,
        favoritesBySource: Map<String, List<DbModel>>,
        run: CheckRun,
        setProgress: suspend (max: Int, progress: Int, source: String) -> Unit,
    ): List<Pair<Int, DbModel>> = coroutineScope {
        sourcesToCheckRecentsFrom
            .mapIndexed { index, sourceApiService ->
                setProgress(sourcesToCheckRecentsFrom.size, index + 1, sourceApiService.serviceName) // Progress 1 to N
                async(networkDispatcher, start = CoroutineStart.LAZY) { // Use LAZY to control start
                    run.sourceRunning(index)
                    val favorites = favoritesBySource[sourceApiService.serviceName].orEmpty()
                    fetchRecentItems(sourceApiService).fold(
                        onSuccess = { recents ->
                            val recentUrls = recents.mapTo(HashSet()) { it.url }
                            val toCheck = favorites.filter { it.url in recentUrls }
                            run.sourceLoaded(index, toCheck.size)
                            toCheck.map { index to it }
                        },
                        onFailure = { e ->
                            val failure = classifyFailure(e)
                            run.sourceFailed(
                                index = index,
                                failure = failure,
                                sourceFailure = SourceFailure(
                                    source = sourceApiService.serviceName,
                                    kind = failure.kind,
                                    httpCode = failure.httpCode,
                                    detail = failure.detail,
                                ),
                                itemFailures = favorites.map { it.toItemFailure(failure, sourceFailed = true) },
                            )
                            emptyList()
                        }
                    )
                }
            }
            .awaitAll()
            .flatten()
    }

    private suspend fun fetchRecentItems(sourceApiService: KmpApiService): Result<List<KmpItemModel>> {
        logFirebaseMessage("Fetching recent items from ${sourceApiService.serviceName}")
        return attempt {
            withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) { // Timeout per source
                sourceApiService
                    .getRecentFlow()
                    // Explicitly ensure flow collection happens on networkDispatcher
                    // if getRecentFlow itself doesn't specify its context.
                    .flowOn(networkDispatcher)
                    .firstOrNull() // Takes the first emission (should be a List<KmpItemModel>)
                    .orEmpty()
            }
        }.onFailure { e ->
            exceptionDao.insertException(e)
            logFirebaseMessage("Exception during recent fetch for ${sourceApiService.serviceName}: ${e.message}")
            recordFirebaseException(e)
        }
    }

    private suspend fun checkItemsForActualUpdates(
        items: List<Pair<Int, DbModel>>,
        run: CheckRun,
        setProgress: suspend (max: Int, progress: Int, source: String) -> Unit,
    ): List<UpdatedItem> = coroutineScope {
        items.mapIndexed { index, (sourceIndex, model) ->
            setProgress(items.size, index + 1, model.title) // Progress 1 to N

            async(networkDispatcher, start = CoroutineStart.LAZY) {
                run.itemStarted(model.title)
                logFirebaseMessage("Checking for update: ${model.title} from ${model.source}")
                val apiService = sourceRepository
                    .toSourceByApiServiceName(model.source)
                    ?.apiService
                if (apiService == null) {
                    logFirebaseMessage("Source API service not found for ${model.source}")
                    run.itemFinished(sourceIndex, updated = false, failure = null)
                    return@async null
                }

                attempt {
                    withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) { // Timeout for fetching full info
                        model
                            .toItemModel(apiService) // This likely involves network I/O
                            .toInfoModel() // Flow<Result<KmpInfoModel>>
                            .first()
                            .getOrThrow()
                    }
                }.fold(
                    onSuccess = { infoModel ->
                        logFirebaseMessage("Old chapters: ${model.numChapters}, New chapters: ${infoModel.chapters.size} for ${model.title}")
                        // newChapters must be computed before saveUpdatedItems overwrites numChapters
                        val updated = if (infoModel.chapters.size > model.numChapters) {
                            UpdatedItem(infoModel, model, newChapters(infoModel, model.numChapters))
                        } else {
                            null // No update needed
                        }
                        run.itemFinished(sourceIndex, updated = updated != null, failure = null)
                        updated
                    },
                    onFailure = { e ->
                        exceptionDao.insertException(e)
                        logFirebaseMessage("Error checking update for ${model.title}: ${e.message}")
                        recordFirebaseException(e)
                        run.itemFinished(
                            sourceIndex = sourceIndex,
                            updated = false,
                            failure = model.toItemFailure(classifyFailure(e), sourceFailed = false),
                        )
                        null
                    }
                )
            }
        }
            .awaitAll()
            .filterNotNull() // Filter out nulls (items with no updates or errors)
    }

    private suspend fun saveUpdatedItems(updatedItems: List<UpdatedItem>) {
        // Perform database operations on a dedicated IO dispatcher
        withContext(Dispatchers.IO) {
            updatedItems.forEach { (infoModel, dbModel) ->
                val oldNumChapters = dbModel.numChapters
                dbModel.numChapters = infoModel.chapters.size
                try {
                    // This should be an update if the item exists. Dirty with a new timestamp so the count syncs.
                    dao.insertFavorite(dbModel.copy(isDirty = true, updatedAt = Clock.System.now().toEpochMilliseconds()))
                    logFirebaseMessage("Saved update for ${dbModel.title}. Chapters: $oldNumChapters -> ${dbModel.numChapters}")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    recordFirebaseException(e)
                    logFirebaseMessage("Database save failed for ${dbModel.title}: ${e.message}")
                    // Potentially revert dbModel.numChapters if the save fails and you need atomicity
                }
            }
        }
    }
}

/**
 * Runs [block], turning a timeout or any other exception into a failure. Cancellation of the caller is rethrown;
 * a [CancellationException] a source throws while the caller is still active is a failure too.
 */
private suspend inline fun <T> attempt(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: TimeoutCancellationException) {
    Result.failure(e)
} catch (e: CancellationException) {
    currentCoroutineContext().ensureActive()
    Result.failure(e)
} catch (e: Exception) {
    Result.failure(e)
}

private fun DbModel.toItemFailure(failure: FailureInfo, sourceFailed: Boolean) = ItemFailure(
    url = url,
    title = title,
    imageUrl = imageUrl,
    source = source,
    kind = failure.kind,
    httpCode = failure.httpCode,
    // The SourceFailure carries the detail for a failed source; repeating it per favorite adds nothing.
    detail = if (sourceFailed) "" else failure.detail,
    sourceFailed = sourceFailed,
)

/** Progress and failures of one run. Every change is serialized and published through [onProgress]. */
private class CheckRun(
    private val onProgress: suspend (UpdateCheckProgress) -> Unit,
) {
    private val startedAt = Clock.System.now().toEpochMilliseconds()
    private val mutex = Mutex()
    private val sourceFailures = mutableListOf<SourceFailure>()
    private val itemFailures = mutableListOf<ItemFailure>()
    private var progress = UpdateCheckProgress(
        favoriteCount = 0,
        loadingRecents = true,
        total = 0,
        checked = 0,
        current = null,
        sources = emptyList(),
        updatesFound = 0,
        itemFailures = 0,
    )

    private suspend fun update(change: UpdateCheckProgress.() -> UpdateCheckProgress) = mutex.withLock {
        progress = progress.change()
        onProgress(progress)
    }

    private fun UpdateCheckProgress.source(index: Int, change: SourceProgress.() -> SourceProgress) =
        copy(sources = sources.mapIndexed { i, s -> if (i == index) s.change() else s })

    suspend fun start(favoriteCount: Int, sources: List<SourceProgress>) = update {
        copy(favoriteCount = favoriteCount, sources = sources)
    }

    suspend fun sourceRunning(index: Int) = update { source(index) { copy(state = SourceState.Running) } }

    suspend fun sourceLoaded(index: Int, toCheck: Int) = update {
        source(index) { copy(state = if (toCheck == 0) SourceState.Done else SourceState.Running, toCheck = toCheck) }
    }

    suspend fun sourceFailed(
        index: Int,
        failure: FailureInfo,
        sourceFailure: SourceFailure,
        itemFailures: List<ItemFailure>,
    ) = update {
        sourceFailures += sourceFailure
        this@CheckRun.itemFailures += itemFailures
        source(index) { copy(state = SourceState.Failed, failure = failure) }
    }

    suspend fun recentsLoaded(total: Int) = update { copy(loadingRecents = false, total = total) }

    suspend fun itemStarted(title: String) = update { copy(current = title) }

    suspend fun itemFinished(sourceIndex: Int, updated: Boolean, failure: ItemFailure?) = update {
        if (failure != null) this@CheckRun.itemFailures += failure
        val updateCount = if (updated) 1 else 0
        copy(
            checked = checked + 1,
            updatesFound = updatesFound + updateCount,
            itemFailures = itemFailures + if (failure != null) 1 else 0,
        ).source(sourceIndex) {
            copy(
                checked = checked + 1,
                updates = updates + updateCount,
                state = if (checked + 1 >= toCheck) SourceState.Done else state,
            )
        }
    }

    suspend fun report(stoppedEarly: String?): UpdateCheckReport = mutex.withLock {
        UpdateCheckReport(
            startedAt = startedAt,
            finishedAt = Clock.System.now().toEpochMilliseconds(),
            favoriteCount = progress.favoriteCount,
            checkedCount = progress.checked,
            updatedCount = progress.updatesFound,
            sourceFailures = sourceFailures.toList(),
            itemFailures = itemFailures.toList(),
            stoppedEarly = stoppedEarly,
        )
    }
}

data class UpdateModel(
    val notificationId: Int,
    val item: UpdatedItem,
)
