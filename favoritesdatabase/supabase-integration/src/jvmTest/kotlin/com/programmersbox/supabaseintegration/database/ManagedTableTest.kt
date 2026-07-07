package com.programmersbox.supabaseintegration.database

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FakeManagedTable(
    val tableName: String,
    override val displayName: String,
    override val supportedActions: List<SupportedTableAction>,
    override val defaultAction: SupportedTableAction,
    val databaseName: String = "test_db",
) : ManagedTable {
    var clearAllCalled = 0
    var purgeDeletedCalled = 0
    var restoreDeletedCalled = 0

    override suspend fun executeAction(action: SupportedTableAction) {
        when (action) {
            SupportedTableAction.NONE -> {}
            SupportedTableAction.CLEAR_ALL -> clearAllCalled++
            SupportedTableAction.PURGE_DELETED -> purgeDeletedCalled++
            SupportedTableAction.RESTORE_DELETED -> restoreDeletedCalled++
        }
    }
}

class ManagedTableTest {

    @Test
    fun `init throws when defaultAction is not NONE and not in supportedActions`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            FakeManagedTable(
                tableName = "users",
                displayName = "Users",
                supportedActions = listOf(SupportedTableAction.CLEAR_ALL),
                defaultAction = SupportedTableAction.PURGE_DELETED,
            )
        }
        assertTrue(exception.message!!.contains("users"))
    }

    @Test
    fun `init succeeds when defaultAction is NONE or is in supportedActions`() {
        FakeManagedTable(
            tableName = "settings",
            displayName = "Settings",
            supportedActions = listOf(SupportedTableAction.CLEAR_ALL, SupportedTableAction.PURGE_DELETED),
            defaultAction = SupportedTableAction.NONE,
        )
        FakeManagedTable(
            tableName = "projects",
            displayName = "Projects",
            supportedActions = listOf(SupportedTableAction.CLEAR_ALL),
            defaultAction = SupportedTableAction.CLEAR_ALL,
        )
    }
}
