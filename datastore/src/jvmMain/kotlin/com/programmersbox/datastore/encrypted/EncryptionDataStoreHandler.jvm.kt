package com.programmersbox.datastore.encrypted

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.SecretKeyAccess
import com.google.crypto.tink.TinkJsonProtoKeysetFormat
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
        // 1. Define a location to store the encryption key
        val parentFile = File(fileDirectory, "/preferences")
        if (!parentFile.exists()) parentFile.mkdirs()
        val keysetFile = File(parentFile, "tink_keyset.json")

        // 2. Load the existing key, or generate and save a new one if it doesn't exist
        val keysetHandle = if (keysetFile.exists()) {
            val storedKeyJson = keysetFile.readText()
            TinkJsonProtoKeysetFormat.parseKeyset(
                storedKeyJson,
                SecretKeyAccess.requireAccess(InsecureSecretKeyAccess.get())
            )
        } else {
            val newHandle = KeysetHandle.generateNew(
                KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM)
            )

            val serializedKey = TinkJsonProtoKeysetFormat.serializeKeyset(
                newHandle,
                SecretKeyAccess.requireAccess(InsecureSecretKeyAccess.get())
            )
            keysetFile.writeText(serializedKey)

            newHandle
        }

        val aead: Aead = keysetHandle.getPrimitive(
            RegistryConfiguration.get(),
            Aead::class.java,
        )

        val encryptedSerializer = AeadSerializer(
            aead = aead,
            wrappedSerializer = PreferencesFileSerializer,
            associatedData = "encrypted_datastore".encodeToByteArray(),
        )

        DataStoreFactory.create(
            serializer = encryptedSerializer,
            produceFile = { File(parentFile, "encrypted_datastore_pb") },
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = {
                    it.printStackTrace()
                    emptyPreferences()
                }
            )
        )
    }

    actual fun create(): DataStore<Preferences> = dataStore
}