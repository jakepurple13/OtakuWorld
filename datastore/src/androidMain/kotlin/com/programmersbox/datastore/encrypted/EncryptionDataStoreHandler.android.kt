package com.programmersbox.datastore.encrypted

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.GlobalScope

actual class EncryptedDataStoreFactory(
    private val context: Context,
) {
    init {
        AeadConfig.register()
    }

    private val keysetHandle = AndroidKeysetManager.Builder()
        .withSharedPref(context, "keysetDatastore", "keyset_datastore_prefs")
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
        associatedData = ENCRYPTED_DATASTORE_KEY_JSON.encodeToByteArray(),
    )

    private val Context.dataStore by dataStore(
        fileName = ENCRYPTED_DATASTORE_KEY_JSON,
        serializer = aeadSerializer,
        scope = GlobalScope,
    )

    actual fun create(): DataStore<Preferences> = context.dataStore

    companion object {
        private const val ENCRYPTED_DATASTORE_KEY = "encrypted_datastore_key"
        private const val ENCRYPTED_DATASTORE_KEY_JSON = "${ENCRYPTED_DATASTORE_KEY}.json"
    }
}