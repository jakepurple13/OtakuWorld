package com.programmersbox.supabaseintegration.client

import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SupabaseClientProvider(
    private val credentialManager: CredentialManager,
    private val supabaseClientEngine: SupabaseClientEngine,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _clientState = MutableStateFlow<SupabaseClient?>(null)
    val clientState: StateFlow<SupabaseClient?> = _clientState.asStateFlow()

    init {
        scope.launch {
            credentialManager.hasCredentials().collect { hasCredentials ->
                _clientState.value = if (hasCredentials) getOrCreate() else null
            }
        }
    }

    suspend fun getOrCreate(): SupabaseClient? {
        _clientState.value?.let { return it }
        val credentials = credentialManager.getCredentials() ?: return null
        return buildClient(credentials).also { _clientState.value = it }
    }

    suspend fun recreate(): SupabaseClient? {
        _clientState.value?.close()
        _clientState.value = null
        return getOrCreate()
    }

    suspend fun close() {
        _clientState.value?.close()
        _clientState.value = null
    }

    private fun buildClient(credentials: SupabaseCredentials): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = credentials.projectUrl,
            supabaseKey = credentials.anonKey
        ) {
            supabaseClientEngine.engine?.let { httpEngine = it }
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
}

class SupabaseClientEngine(val engine: HttpClientEngine? = null)