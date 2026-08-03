package com.programmersbox.datastore.encrypted

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import java.io.File

actual class EncryptedDataStoreFactory(
    fileDirectory: String,
) {
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
            produceFile = { File(fileDirectory, "encrypted_datastore.pb") },
        )
    }

    actual fun create(): DataStore<Preferences> = dataStore
}