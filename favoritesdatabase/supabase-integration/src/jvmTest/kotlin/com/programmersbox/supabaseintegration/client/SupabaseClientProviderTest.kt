package com.programmersbox.supabaseintegration.client

import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

private class FakeCredentialManager(
    private var credentials: SupabaseCredentials? = null,
) : CredentialManager {
    private val _hasCredentials = MutableStateFlow(credentials != null)
    override fun hasCredentials(): Flow<Boolean> = _hasCredentials
    override suspend fun saveCredentials(credentials: SupabaseCredentials) {
        this.credentials = credentials
        _hasCredentials.value = true
    }

    override suspend fun getCredentials(): SupabaseCredentials? = credentials
    override suspend fun clearCredentials() {
        credentials = null
        _hasCredentials.value = false
    }
}

class SupabaseClientProviderTest {

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    @Test
    fun `recreate publishes the new client through clientState even when credentials were already present`() = runTest {
        val credentialManager = FakeCredentialManager(
            credentials = SupabaseCredentials("https://example.supabase.co", "anon-key")
        )
        val provider = SupabaseClientProvider(credentialManager, SupabaseClientEngine())

        awaitCondition { provider.clientState.value != null }
        val firstClient = provider.clientState.value

        val recreatedClient = provider.recreate()

        assertNotSame(firstClient, recreatedClient)
        awaitCondition { provider.clientState.value === recreatedClient }
        assertSame(recreatedClient, provider.clientState.value)
    }
}
