package com.programmersbox.datastore.encrypted

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
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
        // 1. Define a location to store the encryption key
        val parentFile = File(fileDirectory, "/preferences")
        if (!parentFile.exists()) parentFile.mkdirs()
        val keysetFile = File(parentFile, "tink_keyset.json")

        // 2. Load the existing key, or generate and save a new one if it doesn't exist
        val keysetHandle = if (keysetFile.exists()) {
            CleartextKeysetHandle.read(JsonKeysetReader.withInputStream(keysetFile.inputStream()))
        } else {
            keysetFile.createNewFile()

            val newHandle = KeysetHandle.generateNew(
                KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM)
            )
            CleartextKeysetHandle.write(newHandle, JsonKeysetWriter.withOutputStream(keysetFile.outputStream()))
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