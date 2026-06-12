package com.programmersbox.koogintegration.embedding

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Regenerates favorite embeddings in the background. Koin constructor
 * injection — the app's existing workManagerFactory() resolves it.
 */
class FavoritesEmbeddingWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val repository: FavoritesEmbeddingRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val result = runCatching { repository.refreshEmbeddings() }
            .getOrElse { return Result.retry() }
        return when {
            result.missingApiKey -> Result.success() // nothing to do until the user configures a key
            result.failed > 0 && result.embedded == 0 && result.reused == 0 -> Result.retry()
            else -> Result.success()
        }
    }
}

/**
 * Scheduling helper. The app module is responsible for calling
 * [schedule] (e.g. from Application.onCreate) — it is NOT called here.
 */
object EmbeddingWorkScheduler {
    const val WORK_NAME = "favorites_embedding_refresh"
    private const val REPEAT_INTERVAL_HOURS = 6L

    fun createPeriodicRequest(): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<FavoritesEmbeddingWorker>(REPEAT_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

    fun schedule(workManager: WorkManager) {
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            createPeriodicRequest(),
        )
    }
}
