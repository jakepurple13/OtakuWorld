package com.programmersbox.supabaseintegration.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.programmersbox.supabaseintegration.backup.FullSyncWorker
import java.util.concurrent.TimeUnit

actual class FullSyncHandler(
    private val context: Context,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }
    actual fun startWorker() {
        workManager.enqueueUniquePeriodicWork(
            "full_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FullSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresCharging(true)
                        .setRequiresBatteryNotLow(true)
                        .setRequiresStorageNotLow(true)
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        )
    }
}