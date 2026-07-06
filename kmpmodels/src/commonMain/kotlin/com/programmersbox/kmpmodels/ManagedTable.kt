package com.programmersbox.kmpmodels

enum class SupportedTableAction {
    NONE,
    CLEAR_ALL,
    PURGE_DELETED,
    RESTORE_DELETED,
}

abstract class ManagedTable(
    val databaseName: String,
    val tableName: String,
    val displayName: String,
    val supportedActions: List<SupportedTableAction>,
    val defaultAction: SupportedTableAction,
) {
    init {
        require(defaultAction == SupportedTableAction.NONE || defaultAction in supportedActions) {
            "defaultAction must be NONE or one of supportedActions for table '$tableName'"
        }
    }

    abstract suspend fun clearAll()
    abstract suspend fun purgeDeleted()
    abstract suspend fun restoreDeleted()
}
