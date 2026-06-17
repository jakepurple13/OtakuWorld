package com.programmersbox.supabaseintegration.credentials

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

class AndroidCredentialManager(context: Context) : CredentialManager {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "supabase_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val _hasCredentials = MutableStateFlow(prefs.contains(KEY_CREDENTIALS))

    override fun hasCredentials(): Flow<Boolean> = _hasCredentials

    override suspend fun saveCredentials(credentials: SupabaseCredentials) {
        prefs.edit()
            .putString(KEY_CREDENTIALS, Json.encodeToString(credentials))
            .apply()
        _hasCredentials.value = true
    }

    override fun getCredentials(): SupabaseCredentials? {
        val json = prefs.getString(KEY_CREDENTIALS, null) ?: return null
        return runCatching { Json.decodeFromString<SupabaseCredentials>(json) }.getOrNull()
    }

    override suspend fun clearCredentials() {
        prefs.edit().remove(KEY_CREDENTIALS).apply()
        _hasCredentials.value = false
    }

    companion object {
        private const val KEY_CREDENTIALS = "credentials_json"
    }
}

actual fun createCredentialManager(context: Any?): CredentialManager =
    AndroidCredentialManager(context as Context)
