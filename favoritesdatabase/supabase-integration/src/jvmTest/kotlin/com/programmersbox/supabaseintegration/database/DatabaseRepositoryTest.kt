package com.programmersbox.supabaseintegration.database

import com.programmersbox.kmpmodels.ManagedTable
import com.programmersbox.kmpmodels.SupportedTableAction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeManagedTable(
    tableName: String,
    displayName: String,
    supportedActions: List<SupportedTableAction>,
    defaultAction: SupportedTableAction,
    databaseName: String = "test_db",
) : ManagedTable(
    databaseName = databaseName,
    tableName = tableName,
    displayName = displayName,
    supportedActions = supportedActions,
    defaultAction = defaultAction,
) {
    var clearAllCalled = 0
    var purgeDeletedCalled = 0
    var restoreDeletedCalled = 0

    override suspend fun clearAll() { clearAllCalled++ }
    override suspend fun purgeDeleted() { purgeDeletedCalled++ }
    override suspend fun restoreDeleted() { restoreDeletedCalled++ }
}

class DatabaseRepositoryTest {

    private val tableA = FakeManagedTable(
        tableName = "users",
        displayName = "User Profiles",
        supportedActions = listOf(SupportedTableAction.CLEAR_ALL),
        defaultAction = SupportedTableAction.NONE,
    )
    private val tableB = FakeManagedTable(
        tableName = "settings",
        displayName = "User Settings",
        supportedActions = listOf(SupportedTableAction.CLEAR_ALL, SupportedTableAction.PURGE_DELETED, SupportedTableAction.RESTORE_DELETED),
        defaultAction = SupportedTableAction.NONE,
    )

    @Test
    fun `discovers all registered tables`() {
        val repository = DatabaseRepository(listOf(tableA, tableB))
        assertEquals(listOf(tableA, tableB), repository.managedTables)
    }

    @Test
    fun `clear all delegates to clearAll on the correct table only`() = runTest {
        val repository = DatabaseRepository(listOf(tableA, tableB))
        repository.executeActions(mapOf(tableA to SupportedTableAction.CLEAR_ALL, tableB to SupportedTableAction.NONE))
        assertEquals(1, tableA.clearAllCalled)
        assertEquals(0, tableB.clearAllCalled)
    }

    @Test
    fun `purge deleted delegates to purgeDeleted on the correct table only`() = runTest {
        val repository = DatabaseRepository(listOf(tableA, tableB))
        repository.executeActions(mapOf(tableB to SupportedTableAction.PURGE_DELETED))
        assertEquals(1, tableB.purgeDeletedCalled)
        assertEquals(0, tableA.purgeDeletedCalled)
    }

    @Test
    fun `restore deleted delegates to restoreDeleted on the correct table only`() = runTest {
        val repository = DatabaseRepository(listOf(tableA, tableB))
        repository.executeActions(mapOf(tableB to SupportedTableAction.RESTORE_DELETED))
        assertEquals(1, tableB.restoreDeletedCalled)
        assertEquals(0, tableA.restoreDeletedCalled)
    }

    @Test
    fun `displayName is exposed directly from ManagedTable`() {
        val repository = DatabaseRepository(listOf(tableA, tableB))
        assertEquals("User Profiles", repository.managedTables[0].displayName)
        assertEquals("User Settings", repository.managedTables[1].displayName)
    }
}
