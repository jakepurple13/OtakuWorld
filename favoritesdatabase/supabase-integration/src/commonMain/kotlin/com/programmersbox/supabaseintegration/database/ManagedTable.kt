package com.programmersbox.supabaseintegration.database

enum class SupportedTableAction(val isDestructive: Boolean) {
    NONE(false),
    CLEAR_ALL(true),
    PURGE_DELETED(true),
    RESTORE_DELETED(false),
}

abstract class ManagedTable {
    abstract val databaseName: String
    abstract val tableName: String
    abstract val displayName: String
    abstract val supportedActions: List<SupportedTableAction>
    abstract val defaultAction: SupportedTableAction
    abstract suspend fun clearAll()
    abstract suspend fun purgeDeleted()
    abstract suspend fun restoreDeleted()
}
