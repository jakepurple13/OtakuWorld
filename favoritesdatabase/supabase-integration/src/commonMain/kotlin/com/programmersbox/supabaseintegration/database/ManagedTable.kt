package com.programmersbox.supabaseintegration.database

enum class SupportedTableAction(val isDestructive: Boolean) {
    NONE(false),
    CLEAR_ALL(true),
    PURGE_DELETED(true),
    RESTORE_DELETED(false),
}

abstract class ManagedTable {
    abstract val tableName: String
    abstract val displayName: String
    abstract val supportedActions: List<SupportedTableAction>
    abstract val defaultAction: SupportedTableAction
    abstract suspend fun executeAction(action: SupportedTableAction)
}
