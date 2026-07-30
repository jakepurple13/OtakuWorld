package com.programmersbox.supabaseintegration.credentials

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.tink.AeadSerializer
import ca.gosyer.appdirs.AppDirs
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.File

class JvmCredentialManager(
    appDirs: AppDirs,
) : CredentialManager {
    init {
        AeadConfig.register()
    }

    private val dataStore by lazy {
        // 2. Generate or load a KeysetHandle (AES256_GCM is recommended)
        val keysetHandle = KeysetHandle.generateNew(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
        val aead: Aead = keysetHandle.getPrimitive(
            RegistryConfiguration.get(),
            Aead::class.java,
        )

        val encryptedSerializer = AeadSerializer(
            aead = aead, // Pass the initialized Tink AEAD primitive
            wrappedSerializer = PreferencesFileSerializer,
        )

        DataStoreFactory.create(
            serializer = encryptedSerializer,
            produceFile = { File(appDirs.getUserDataDir(), "supabase_credentials.pb") },
        )
    }

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
