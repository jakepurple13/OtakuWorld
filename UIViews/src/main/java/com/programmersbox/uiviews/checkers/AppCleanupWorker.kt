package com.programmersbox.uiviews.checkers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.perf.trace
import com.programmersbox.kmpuiviews.logFirebaseMessage
import java.util.concurrent.TimeUnit

class AppCleanupWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = trace("app_cleanup") {
        logFirebaseMessage("Starting App Cleanup")
        var count = 0
        runCatching {
            applicationContext
                .cacheDir
                ?.listFiles { file -> file.extension == "apk" }
                ?.forEach {
                    count++
                    incrementMetric("appCleaning", 1)
                    it.delete()
                }
        }
            .also { logFirebaseMessage("Deleted $count files") }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.failure() }
            )
    }

    companion object {
        fun setupWorker(workManager: WorkManager) {
            workManager.enqueueUniquePeriodicWork(
                "appCleanup",
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<AppCleanupWorker>(
                    repeatInterval = 1,
                    repeatIntervalTimeUnit = TimeUnit.DAYS
                ).build()
            )
        }
    }
}