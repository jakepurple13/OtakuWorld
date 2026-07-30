package com.programmersbox.supabaseintegration.credentials

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.dataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.tink.AeadSerializer
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class AndroidCredentialManager(private val context: Context) : CredentialManager {

    init {
        AeadConfig.register()
    }

    private val keysetHandle = AndroidKeysetManager.Builder()
        .withSharedPref(context, "keyset", "keyset_prefs")
        .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
        .withMasterKeyUri("android-keystore://master_key")
        .build()
        .keysetHandle

    private val aeadSerializer = AeadSerializer(
        // Use tink APIs to create an Aead object to encrypt/decrypt data.
        aead = keysetHandle.getPrimitive(
            RegistryConfiguration.get(),
            Aead::class.java,
        ),
        // AeadSerializer can wrap an existing serializer.
        wrappedSerializer = PreferencesFileSerializer,
        // Specify a unique name to prevent a ciphertext swapping attack.
        associatedData = KEY_CREDENTIALS_JSON.encodeToByteArray(),
    )

    private val Context.dataStore by dataStore(
        fileName = KEY_CREDENTIALS_JSON,
        serializer = aeadSerializer,
        scope = GlobalScope,
        produceMigrations = {
            listOf(
                SharedPreferencesMigration(
                    it,
                    "supabase_credentials", // Your old SharedPrefs name
                )
            )
        }
    )

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

    init {
        GlobalScope.launch(Dispatchers.IO) {
            if (prefs.contains(KEY_CREDENTIALS)) {
                val current = prefs.getString(KEY_CREDENTIALS, null) ?: return@launch
                context.dataStore.updateData { prefs -> prefs.copy { it[key] = current } }
                prefs.edit { remove(KEY_CREDENTIALS) }
            }
        }
    }

    override fun hasCredentials(): Flow<Boolean> = context
        .dataStore
        .data
        .map { !it[key].isNullOrBlank() }

    override suspend fun saveCredentials(credentials: SupabaseCredentials) {
        context.dataStore.updateData { prefs ->
            prefs.copy { it[key] = Json.encodeToString(credentials) }
        }
    }

    override suspend fun getCredentials(): SupabaseCredentials? {
        val json = context
            .dataStore
            .data
            .map { it[key] }
            .firstOrNull()
            ?: return null
        return runCatching { Json.decodeFromString<SupabaseCredentials>(json) }.getOrNull()
    }

    override suspend fun clearCredentials() {
        context.dataStore.updateData { prefs -> prefs.copy { it.remove(key) } }
    }

    companion object {
        private const val KEY_CREDENTIALS = "credentials_json"
        private const val KEY_CREDENTIALS_JSON = "${KEY_CREDENTIALS}.json"
        private val key = stringPreferencesKey(KEY_CREDENTIALS)
    }
}
