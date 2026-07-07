package com.programmersbox.supabaseintegration.database

import com.programmersbox.favoritesdatabase.HeatMapDao

class HeatMapManagedTable(
    private val heatMapDao: HeatMapDao,
) : ManagedTable {
    override val displayName: String = "Activity Heat Map"

    override val defaultAction: SupportedTableAction = SupportedTableAction.NONE

    override val supportedActions: List<SupportedTableAction> = listOf(
        SupportedTableAction.NONE,
        SupportedTableAction.CLEAR_ALL,
        SupportedTableAction.PURGE_DELETED,
        SupportedTableAction.RESTORE_DELETED
    )

    override suspend fun executeAction(action: SupportedTableAction) {
        when (action) {
            SupportedTableAction.NONE -> Unit
            SupportedTableAction.CLEAR_ALL -> {
                heatMapDao
                    .getAllHeatMapsSync()
                    .forEach { heatMapDao.deleteHeatMap(it) }
            }

            SupportedTableAction.PURGE_DELETED -> {
                heatMapDao.deleteAllDeletedHeatMapItems()
            }

            SupportedTableAction.RESTORE_DELETED -> {
                heatMapDao.resetAllHeatMapIsDeleted()
            }
        }
    }
}
