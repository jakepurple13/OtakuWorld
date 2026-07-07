package com.programmersbox.supabaseintegration.database

enum class SupportedTableAction(val isDestructive: Boolean) {
    NONE(false),
    CLEAR_ALL(true),
    PURGE_DELETED(true),
    RESTORE_DELETED(false),
}

interface ManagedTable {
    val displayName: String
    val supportedActions: List<SupportedTableAction>
    val defaultAction: SupportedTableAction
    suspend fun executeAction(action: SupportedTableAction)
}
