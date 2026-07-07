package com.programmersbox.supabaseintegration.database

class DatabaseRepository(
    val managedTables: List<ManagedTable>,
) {
    suspend fun executeActions(selections: Map<ManagedTable, SupportedTableAction>) {
        selections.forEach { (table, action) -> table.executeAction(action) }
    }
}
