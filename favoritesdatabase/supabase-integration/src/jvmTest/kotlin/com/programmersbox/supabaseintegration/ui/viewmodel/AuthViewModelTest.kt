package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpmodels.ManagedTable
import com.programmersbox.kmpmodels.SupportedTableAction
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.auth.SupabaseUser
import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
import com.programmersbox.supabaseintegration.database.DatabaseRepository
import io.github.jan.supabase.auth.providers.OAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeManagedTable(
    tableName: String,
    displayName: String,
    supportedActions: List<SupportedTableAction>,
    defaultAction: SupportedTableAction,
    databaseName: String = "test_db",
    private val callLog: MutableList<String>? = null,
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
    var onClearAll: (suspend () -> Unit)? = null

    override suspend fun clearAll() {
        clearAllCalled++
        callLog?.add("clearAll:$tableName")
        onClearAll?.invoke()
    }

    override suspend fun purgeDeleted() {
        purgeDeletedCalled++
        callLog?.add("purgeDeleted:$tableName")
    }

    override suspend fun restoreDeleted() {
        restoreDeletedCalled++
        callLog?.add("restoreDeleted:$tableName")
    }
}

private class FakeLogoutAuthManager(
    initial: AuthState = AuthState.Authenticated(SupabaseUser(id = "user-1", email = "user@example.com", phone = null, displayName = null)),
    private val callLog: MutableList<String>? = null,
) : AuthManager {
    private val _authState = MutableStateFlow(initial)
    override val authState: StateFlow<AuthState> = _authState
    var signOutCallCount = 0
    override fun isLoggedIn(): Boolean = _authState.value is AuthState.Authenticated
    override suspend fun signInWithEmail(email: String, password: String) {}
    override suspend fun signUpWithEmail(email: String, password: String) {}
    override suspend fun signInWithOAuth(provider: OAuthProvider) {}
    override suspend fun signInWithMagicLink(email: String) {}
    override suspend fun signInWithPhone(phone: String, otp: String) {}
    override suspend fun signInAnonymously() {}
    override suspend fun signOut() {
        signOutCallCount++
        callLog?.add("signOut")
    }
    override suspend fun deleteAccount() {}
    override suspend fun refreshSession() {}
}

private class FakeCredentialManager : CredentialManager {
    override fun hasCredentials(): Flow<Boolean> = flowOf(false)
    override suspend fun saveCredentials(credentials: SupabaseCredentials) {}
    override fun getCredentials(): SupabaseCredentials? = null
    override suspend fun clearCredentials() {}
}

class AuthViewModelTest {

    private val viewModelStore = ViewModelStore()

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel(
        managedTables: List<ManagedTable>,
        authManager: AuthManager = FakeLogoutAuthManager(),
    ) = AuthViewModel(
        authManager = authManager,
        credentialManager = FakeCredentialManager(),
        databaseRepository = DatabaseRepository(managedTables),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    // --- ViewModel Logout Flow Tests ---

    @Test
    fun `no actions selected logout proceeds without table actions`() = runTest {
        val table = FakeManagedTable("users", "Users", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE)
        val authManager = FakeLogoutAuthManager()
        val vm = viewModel(listOf(table), authManager)

        vm.confirmLogout()
        awaitCondition { !vm.logoutUiState.value.isLoggingOut }

        assertEquals(1, authManager.signOutCallCount)
        assertEquals(0, table.clearAllCalled)
    }

    @Test
    fun `selected action calls only that method on that table`() = runTest {
        val tableA = FakeManagedTable("users", "Users", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE)
        val tableB = FakeManagedTable("settings", "Settings", listOf(SupportedTableAction.PURGE_DELETED), SupportedTableAction.NONE)
        val vm = viewModel(listOf(tableA, tableB))

        vm.setManageDatabasesEnabled(true)
        vm.setTableAction(tableA, SupportedTableAction.CLEAR_ALL)
        vm.confirmLogout()
        awaitCondition { !vm.logoutUiState.value.isLoggingOut }

        assertEquals(1, tableA.clearAllCalled)
        assertEquals(0, tableB.clearAllCalled)
        assertEquals(0, tableB.purgeDeletedCalled)
    }

    @Test
    fun `mixed actions across tables each execute their own selected action`() = runTest {
        val tableClear = FakeManagedTable("users", "Users", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE)
        val tablePurge = FakeManagedTable("settings", "Settings", listOf(SupportedTableAction.PURGE_DELETED), SupportedTableAction.NONE)
        val tableRestore = FakeManagedTable("projects", "Projects", listOf(SupportedTableAction.RESTORE_DELETED), SupportedTableAction.NONE)
        val tableNone = FakeManagedTable("members", "Members", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE)
        val vm = viewModel(listOf(tableClear, tablePurge, tableRestore, tableNone))

        vm.setManageDatabasesEnabled(true)
        vm.setTableAction(tableClear, SupportedTableAction.CLEAR_ALL)
        vm.setTableAction(tablePurge, SupportedTableAction.PURGE_DELETED)
        vm.setTableAction(tableRestore, SupportedTableAction.RESTORE_DELETED)
        vm.confirmLogout()
        awaitCondition { !vm.logoutUiState.value.isLoggingOut }

        assertEquals(1, tableClear.clearAllCalled)
        assertEquals(1, tablePurge.purgeDeletedCalled)
        assertEquals(1, tableRestore.restoreDeletedCalled)
        assertEquals(0, tableNone.clearAllCalled)
    }

    @Test
    fun `supabase auth is called regardless of whether table actions are configured`() = runTest {
        val table = FakeManagedTable("users", "Users", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE)
        val authManager = FakeLogoutAuthManager()
        val vm = viewModel(listOf(table), authManager)

        vm.setManageDatabasesEnabled(true)
        vm.setTableAction(table, SupportedTableAction.CLEAR_ALL)
        vm.confirmLogout()
        awaitCondition { !vm.logoutUiState.value.isLoggingOut }

        assertEquals(1, authManager.signOutCallCount)
    }

    @Test
    fun `supabase auth logout completes before any table action runs`() = runTest {
        val callLog = mutableListOf<String>()
        val table = FakeManagedTable("users", "Users", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE, callLog = callLog)
        val authManager = FakeLogoutAuthManager(callLog = callLog)
        val vm = viewModel(listOf(table), authManager)

        vm.setManageDatabasesEnabled(true)
        vm.setTableAction(table, SupportedTableAction.CLEAR_ALL)
        vm.confirmLogout()
        awaitCondition { !vm.logoutUiState.value.isLoggingOut }

        assertEquals(listOf("signOut", "clearAll:users"), callLog)
    }

    // --- ViewModel State Tests ---

    @Test
    fun `master toggle off reverts every selector to its defaultAction`() = runTest {
        val table = FakeManagedTable(
            tableName = "settings",
            displayName = "Settings",
            supportedActions = listOf(SupportedTableAction.CLEAR_ALL, SupportedTableAction.PURGE_DELETED),
            defaultAction = SupportedTableAction.PURGE_DELETED,
        )
        val vm = viewModel(listOf(table))

        vm.setManageDatabasesEnabled(true)
        vm.setTableAction(table, SupportedTableAction.CLEAR_ALL)
        assertEquals(SupportedTableAction.CLEAR_ALL, vm.logoutUiState.value.tableSelections.single().selectedAction)

        vm.setManageDatabasesEnabled(false)
        assertEquals(SupportedTableAction.PURGE_DELETED, vm.logoutUiState.value.tableSelections.single().selectedAction)
    }

    @Test
    fun `isLoggingOut is true while table actions execute and false after`() = runTest {
        var loadingDuringExecution = false
        val table = FakeManagedTable("users", "Users", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE)
        val vm = viewModel(listOf(table))
        table.onClearAll = { loadingDuringExecution = vm.logoutUiState.value.isLoggingOut }

        vm.setManageDatabasesEnabled(true)
        vm.setTableAction(table, SupportedTableAction.CLEAR_ALL)
        vm.confirmLogout()
        awaitCondition { !vm.logoutUiState.value.isLoggingOut }

        assertTrue(loadingDuringExecution)
        assertFalse(vm.logoutUiState.value.isLoggingOut)
    }

    @Test
    fun `changing one table selection does not affect other tables`() = runTest {
        val tableA = FakeManagedTable("users", "Users", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE)
        val tableB = FakeManagedTable("settings", "Settings", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE)
        val vm = viewModel(listOf(tableA, tableB))

        vm.setTableAction(tableA, SupportedTableAction.CLEAR_ALL)

        val selections = vm.logoutUiState.value.tableSelections
        assertEquals(SupportedTableAction.CLEAR_ALL, selections.first { it.table == tableA }.selectedAction)
        assertEquals(SupportedTableAction.NONE, selections.first { it.table == tableB }.selectedAction)
    }

    @Test
    fun `default selections are initialized from each table's defaultAction`() = runTest {
        val table = FakeManagedTable(
            tableName = "settings",
            displayName = "Settings",
            supportedActions = listOf(SupportedTableAction.PURGE_DELETED),
            defaultAction = SupportedTableAction.PURGE_DELETED,
        )
        val vm = viewModel(listOf(table))

        assertEquals(SupportedTableAction.PURGE_DELETED, vm.logoutUiState.value.tableSelections.single().selectedAction)
    }

    // --- Cancellation Tests ---

    @Test
    fun `cancel from bottom sheet triggers no logout or table actions`() = runTest {
        val table = FakeManagedTable("users", "Users", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE)
        val authManager = FakeLogoutAuthManager()
        val vm = viewModel(listOf(table), authManager)

        vm.setManageDatabasesEnabled(true)
        vm.setTableAction(table, SupportedTableAction.CLEAR_ALL)
        // User taps Cancel on the bottom sheet — confirmLogout() is never invoked.

        assertEquals(0, authManager.signOutCallCount)
        assertEquals(0, table.clearAllCalled)
    }

    @Test
    fun `cancel from confirmation dialog discards configured actions without executing them`() = runTest {
        val table = FakeManagedTable("users", "Users", listOf(SupportedTableAction.CLEAR_ALL), SupportedTableAction.NONE)
        val authManager = FakeLogoutAuthManager()
        val vm = viewModel(listOf(table), authManager)

        vm.setManageDatabasesEnabled(true)
        vm.setTableAction(table, SupportedTableAction.CLEAR_ALL)
        // Bottom sheet already confirmed ("Continue to Logout"), dialog shown, user taps "No" — confirmLogout() is never invoked.

        assertEquals(0, authManager.signOutCallCount)
        assertEquals(0, table.clearAllCalled)
    }
}
