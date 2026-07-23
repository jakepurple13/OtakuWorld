package com.programmersbox.supabaseintegration.repository

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.supabaseintegration.sync.SyncManager
import kotlin.time.Clock

class ActivityRepository(
    private val activityDao: ActivityDao,
    private val dataStoreHandling: DataStoreHandling,
    private val syncManager: SyncManager,
) {
    suspend fun incrementSeconds() {
        activityDao.incrementSeconds()
    }

    suspend fun onActivityStop() {
        activityDao.markDirtyNow(Clock.System.now().toEpochMilliseconds())
    }

    suspend fun migrateFromDataStoreIfNeeded() {
        val existing = dataStoreHandling.timeSpentDoing.getOrNull() ?: 0L
        if (existing == 0L) return
        dataStoreHandling.timeSpentDoing.set(0L)
        activityDao.incrementSeconds(existing)
    }
}
