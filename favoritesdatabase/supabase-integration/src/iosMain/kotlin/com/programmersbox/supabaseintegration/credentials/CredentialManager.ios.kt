package com.programmersbox.supabaseintegration.credentials

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

// Production upgrade: replace NSUserDefaults with Security.framework kSecClass/SecItemAdd calls
class IosCredentialManager : CredentialManager {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val _hasCredentials = MutableStateFlow(defaults.stringForKey(KEY) != null)

    override fun hasCredentials(): Flow<Boolean> = _hasCredentials

    override suspend fun saveCredentials(credentials: SupabaseCredentials) {
        defaults.setObject(Json.encodeToString(credentials), KEY)
        _hasCredentials.value = true
    }

    override fun getCredentials(): SupabaseCredentials? {
        val json = defaults.stringForKey(KEY) ?: return null
        return runCatching { Json.decodeFromString<SupabaseCredentials>(json) }.getOrNull()
    }

    override suspend fun clearCredentials() {
        defaults.removeObjectForKey(KEY)
        _hasCredentials.value = false
    }

    companion object {
        private const val KEY = "supabase_credentials_json"
    }
}

actual fun createCredentialManager(context: Any?): CredentialManager = IosCredentialManager()
