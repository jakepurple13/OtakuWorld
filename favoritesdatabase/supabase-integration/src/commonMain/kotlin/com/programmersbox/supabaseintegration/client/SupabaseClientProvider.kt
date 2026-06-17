package com.programmersbox.supabaseintegration.client

import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SupabaseClientProvider(private val credentialManager: CredentialManager) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var _client: SupabaseClient? = null

    val clientState: StateFlow<SupabaseClient?> = credentialManager.hasCredentials()
        .map { hasCredentials -> if (hasCredentials) getOrCreate() else null }
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun getOrCreate(): SupabaseClient? {
        val credentials = credentialManager.getCredentials() ?: return null
        if (_client == null) _client = buildClient(credentials)
        return _client
    }

    suspend fun recreate(): SupabaseClient? {
        _client?.close()
        _client = null
        return getOrCreate()
    }

    suspend fun close() {
        _client?.close()
        _client = null
    }

    private fun buildClient(credentials: SupabaseCredentials): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = credentials.projectUrl,
            supabaseKey = credentials.anonKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
}
