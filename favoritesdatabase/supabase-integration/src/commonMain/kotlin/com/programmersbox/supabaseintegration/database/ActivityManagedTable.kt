package com.programmersbox.supabaseintegration.database

import com.programmersbox.favoritesdatabase.ActivityDao
import kotlin.time.Clock

class ActivityManagedTable(private val activityDao: ActivityDao) : ManagedTable {
    override val displayName: String = "Activity Timer"
    override val supportedActions: List<SupportedTableAction> = listOf(SupportedTableAction.CLEAR_ALL)
    override val defaultAction: SupportedTableAction = SupportedTableAction.CLEAR_ALL

    override suspend fun executeAction(action: SupportedTableAction) {
        if (action == SupportedTableAction.CLEAR_ALL) {
            activityDao.upsertSynced(0L, Clock.System.now().toEpochMilliseconds())
        }
    }
}
