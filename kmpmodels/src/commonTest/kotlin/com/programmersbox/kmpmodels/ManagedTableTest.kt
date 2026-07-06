package com.programmersbox.kmpmodels

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

private class FakeManagedTable(
    override val tableName: String,
    override val displayName: String,
    override val supportedActions: List<SupportedTableAction>,
    override val defaultAction: SupportedTableAction,
    override val databaseName: String = "test_db",
) : ManagedTable() {
    override suspend fun clearAll() {}
    override suspend fun purgeDeleted() {}
    override suspend fun restoreDeleted() {}
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
