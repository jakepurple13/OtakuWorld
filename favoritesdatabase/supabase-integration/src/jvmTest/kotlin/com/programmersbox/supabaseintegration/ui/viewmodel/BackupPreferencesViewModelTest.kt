package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.SyncPreferences
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.syncprocessor.SyncProcessor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.postgrest.result.PostgrestResult
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
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeSyncProcessor(
    tableName: String,
    override val displayName: String,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<String, String>(tableName = tableName) {
    override suspend fun getDirtyItems(): List<String> = emptyList()
    override fun observeDirtyItems(): Flow<Int> = flowOf(0)
    override fun isLocalDeleted(local: String) = false
    override fun getLocalUpdatedAt(local: String) = 0L
    override fun toRemoteRow(local: String, uid: String, timestamp: Long) = local
    override suspend fun markLocalSynced(local: String, timestamp: Long) {}
    override suspend fun deleteLocal(local: String) {}
    override suspend fun performUpsert(client: SupabaseClient, items: List<String>) {}
    override fun isRemoteDeleted(remote: String) = false
    override fun getRemoteUpdatedAt(remote: String) = 0L
    override suspend fun getLocalEquivalent(remote: String): String? = null
    override suspend fun upsertLocal(remote: String) {}
    override suspend fun performSelect(postgrestResult: PostgrestResult): List<String> = emptyList()
}

private class FakeAuthManager(initial: AuthState = AuthState.Unauthenticated) : AuthManager {
    private val _authState = MutableStateFlow(initial)
    override val authState: StateFlow<AuthState> = _authState
    override fun isLoggedIn(): Boolean = _authState.value is AuthState.Authenticated
    override suspend fun signInWithEmail(email: String, password: String, context: Any?) {}
    override suspend fun signUpWithEmail(email: String, password: String, context: Any?) {}
    override suspend fun signInWithOAuth(provider: OAuthProvider) {}
    override suspend fun signInWithMagicLink(email: String) {}
    override suspend fun signInWithPhone(phone: String, otp: String) {}
    override suspend fun signInAnonymously() {}
    override suspend fun signOut() {}
    override suspend fun deleteAccount() {}
    override suspend fun refreshSession() {}

    /*override suspend fun startPasskeyRegistration(): PasskeyRegistrationResponse =
        throw NotImplementedError()
    override suspend fun verifyPasskeyRegistration(challengeId: String, credentialJson: String): PasskeyRegistrationVerifyResponse =
        throw NotImplementedError()*/
    override fun reportError(message: String) {}

    fun setAuthState(state: AuthState) {
        _authState.value = state
    }
}

class BackupPreferencesViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: SyncPreferences
    private lateinit var repository: BackupPreferenceRepository
    private lateinit var authManager: FakeAuthManager

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel(processors: List<SyncProcessor<*, *>>) = BackupPreferencesViewModel(
        backupPreferenceRepository = repository,
        syncProcessors = processors,
        authManager = authManager,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("backup-prefs-vm-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<SyncPreferences>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = BackupPreferenceRepository(database.backupPreferenceDao())
        authManager = FakeAuthManager()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
        dbFile.delete()
    }

    @Test
    fun `items default to enabled when no preference is stored`() = runTest {
        val processors = listOf(FakeSyncProcessor("notes", "Notes", repository))
        val vm = viewModel(processors)

        val sub = backgroundScope.launch { vm.uiState.collect {} }
        awaitCondition { vm.uiState.value.items.isNotEmpty() }

        assertEquals(1, vm.uiState.value.items.size)
        assertTrue(vm.uiState.value.items.single().enabled)
        assertEquals("Notes", vm.uiState.value.items.single().displayName)
    }

    @Test
    fun `toggling a table off is reflected in uiState`() = runTest {
        val processors = listOf(FakeSyncProcessor("notes", "Notes", repository))
        val vm = viewModel(processors)

        val sub = backgroundScope.launch { vm.uiState.collect {} }
        awaitCondition { vm.uiState.value.items.isNotEmpty() }

        vm.setBackupEnabled("notes", false)
        awaitCondition { vm.uiState.value.items.single().enabled.not() }

        assertFalse(vm.uiState.value.items.single().enabled)
    }

    @Test
    fun `isLoggedIn reflects the current auth state`() = runTest {
        val processors = listOf(FakeSyncProcessor("notes", "Notes", repository))
        val vm = viewModel(processors)

        val sub = backgroundScope.launch { vm.uiState.collect {} }
        awaitCondition { vm.uiState.value.items.isNotEmpty() }
        assertFalse(vm.uiState.value.isLoggedIn)

        authManager.setAuthState(
            AuthState.Authenticated(
                com.programmersbox.supabaseintegration.auth.SupabaseUser(id = "user-1", email = null, phone = null, displayName = null)
            )
        )
        awaitCondition { vm.uiState.value.isLoggedIn }

        assertTrue(vm.uiState.value.isLoggedIn)
    }
}
