package com.programmersbox.supabaseintegration.credentials

import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.encrypted.EncryptedDataStoreHandling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class JvmCredentialManager(
    encryptedDataStoreHandling: EncryptedDataStoreHandling,
) : CredentialManager {

    private val dataStore = encryptedDataStoreHandling.dataStore

    override fun hasCredentials(): Flow<Boolean> = dataStore
        .data
        .map { !it[key].isNullOrBlank() }

    override suspend fun saveCredentials(credentials: SupabaseCredentials) {
        dataStore.updateData { prefs ->
            prefs.copy { it[key] = Json.encodeToString(credentials) }
        }
    }

    override suspend fun getCredentials(): SupabaseCredentials? {
        return runCatching { dataStore.data.map { it[key] }.firstOrNull()!! }
            .mapCatching { Json.decodeFromString<SupabaseCredentials>(it) }
            .getOrNull()
    }

    override suspend fun clearCredentials() {
        dataStore.updateData { prefs -> prefs.copy { it[key] = "" } }
    }

    companion object {
        private const val KEY_CREDENTIALS = "credentials_json"
        private const val KEY_CREDENTIALS_JSON = "${KEY_CREDENTIALS}.json"
        private val key = stringPreferencesKey(KEY_CREDENTIALS)
    }
}
