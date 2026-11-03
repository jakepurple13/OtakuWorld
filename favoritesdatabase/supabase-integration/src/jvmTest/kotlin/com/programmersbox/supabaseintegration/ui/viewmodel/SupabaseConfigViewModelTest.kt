package com.programmersbox.supabaseintegration.ui.viewmodel

import com.programmersbox.supabaseintegration.client.SupabaseClientEngine
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
import com.programmersbox.supabaseintegration.sync.SyncConfig
import com.programmersbox.supabaseintegration.sync.SyncConfigDataStore
import com.programmersbox.supabaseintegration.sync.SyncConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private object NoCredentialManager : CredentialManager {
    override fun hasCredentials(): Flow<Boolean> = flowOf(false)
    override suspend fun saveCredentials(credentials: SupabaseCredentials) = Unit
    override suspend fun getCredentials(): SupabaseCredentials? = null
    override suspend fun clearCredentials() = Unit
}

private class InMemorySyncConfigStore {
    val pollIntervalMs = MutableStateFlow(SyncConfig().pollIntervalMs)
    val maxRetries = MutableStateFlow(SyncConfig().maxRetries)
    val initialBackoffMs = MutableStateFlow(SyncConfig().initialBackoffMs)
    val maxBackoffMs = MutableStateFlow(SyncConfig().maxBackoffMs)

    fun toDataStore() = SyncConfigDataStore(
        pollIntervalMs = pollIntervalMs,
        maxRetries = maxRetries,
        initialBackoffMs = initialBackoffMs,
        maxBackoffMs = maxBackoffMs,
        setPollIntervalMs = { pollIntervalMs.value = it },
        setMaxRetries = { maxRetries.value = it },
        setInitialBackoffMs = { initialBackoffMs.value = it },
        setMaxBackoffMs = { maxBackoffMs.value = it },
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseConfigViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(store: InMemorySyncConfigStore) = SupabaseConfigViewModel(
        credentialManager = NoCredentialManager,
        clientProvider = SupabaseClientProvider(NoCredentialManager, SupabaseClientEngine()),
        syncConfigRepository = SyncConfigRepository(store.toDataStore()),
    )

    @Test
    fun scannedSyncConfigFillsFieldsInDisplayUnits() = runTest {
        val vm = viewModel(InMemorySyncConfigStore())

        vm.onScannedSyncConfig(
            SyncConfig(
                pollIntervalMs = 15 * 60_000L,
                maxRetries = 7,
                initialBackoffMs = 2_000L,
                maxBackoffMs = 45_000L,
            )
        )

        assertEquals("15", vm.pollIntervalMinutes.value)
        assertEquals("7", vm.maxRetries.value)
        assertEquals("2", vm.initialBackoffSeconds.value)
        assertEquals("45", vm.maxBackoffSeconds.value)
    }

    @Test
    fun scannedSyncConfigRoundTripsThroughApply() = runTest {
        val store = InMemorySyncConfigStore()
        val vm = viewModel(store)
        val scanned = SyncConfig(
            pollIntervalMs = 15 * 60_000L,
            maxRetries = 7,
            initialBackoffMs = 2_000L,
            maxBackoffMs = 45_000L,
        )

        vm.onScannedSyncConfig(scanned)
        vm.saveSyncConfig()

        assertEquals(scanned.pollIntervalMs, store.pollIntervalMs.value)
        assertEquals(scanned.maxRetries, store.maxRetries.value)
        assertEquals(scanned.initialBackoffMs, store.initialBackoffMs.value)
        assertEquals(scanned.maxBackoffMs, store.maxBackoffMs.value)
    }
}
