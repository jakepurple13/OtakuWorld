package com.programmersbox.kmpmodels

enum class SupportedTableAction {
    NONE,
    CLEAR_ALL,
    PURGE_DELETED,
    RESTORE_DELETED,
}

abstract class ManagedTable {
    abstract val databaseName: String
    abstract val tableName: String
    abstract val displayName: String
    abstract val supportedActions: List<SupportedTableAction>
    abstract val defaultAction: SupportedTableAction

    init {
        require(defaultAction == SupportedTableAction.NONE || defaultAction in supportedActions) {
            "defaultAction must be NONE or one of supportedActions for table '$tableName'"
        }
    }

    abstract suspend fun clearAll()
    abstract suspend fun purgeDeleted()
    abstract suspend fun restoreDeleted()
}
