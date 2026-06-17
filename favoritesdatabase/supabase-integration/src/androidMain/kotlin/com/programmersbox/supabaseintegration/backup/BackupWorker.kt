package com.programmersbox.supabaseintegration.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params), KoinComponent {
    private val backupManager: BackupManager by inject()

    override suspend fun doWork(): Result {
        val dbPath = applicationContext.getDatabasePath("item_database.db").absolutePath
        return backupManager.uploadBackup(dbPath)
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}
