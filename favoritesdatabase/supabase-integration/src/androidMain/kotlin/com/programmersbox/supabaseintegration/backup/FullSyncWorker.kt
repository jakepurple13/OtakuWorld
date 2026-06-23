package com.programmersbox.supabaseintegration.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programmersbox.supabaseintegration.sync.SyncManager

class FullSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return runCatching { syncManager.triggerSync() }
            .onFailure { it.printStackTrace() }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.failure() }
            )
    }
}