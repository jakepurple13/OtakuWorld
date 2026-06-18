package com.programmersbox.supabaseintegration.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BackupWorker(
    ctx: Context,
    params: WorkerParameters,
    private val backupManager: BackupManager,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val dbPath = applicationContext.getDatabasePath("favoriteItems.db").absolutePath
        return backupManager
            .uploadBackup(dbPath)
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}
