package com.programmersbox.supabaseintegration.database

class DatabaseRepository(
    val managedTables: List<ManagedTable>,
) {
    suspend fun executeAction(table: ManagedTable, action: SupportedTableAction) {
        when (action) {
            SupportedTableAction.NONE -> Unit
            SupportedTableAction.CLEAR_ALL -> table.clearAll()
            SupportedTableAction.PURGE_DELETED -> table.purgeDeleted()
            SupportedTableAction.RESTORE_DELETED -> table.restoreDeleted()
        }
    }

    suspend fun executeActions(selections: Map<ManagedTable, SupportedTableAction>) {
        selections.forEach { (table, action) -> executeAction(table, action) }
    }
}
