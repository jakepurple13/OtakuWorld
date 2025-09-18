package com.programmersbox.otakuworld

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class MultiprocessDataStoreHandler(
    context: Context,
) {
    val multiprocessDataStore = MultiProcessDataStoreFactory.create(
        serializer = SettingsSerializer(),
        produceFile = { context.preferencesDataStoreFile("otakuworld_mps") },
        corruptionHandler = ReplaceFileCorruptionHandler {
            it.printStackTrace()
            OtakuSettings(0)
        }
    )

    fun getFlow() = multiprocessDataStore.data

    suspend fun updateData(transform: suspend (t: OtakuSettings) -> OtakuSettings) {
        multiprocessDataStore.updateData(transform)
    }

    suspend fun get() = multiprocessDataStore
        .data
        .firstOrNull()
        ?: OtakuSettings(0)
}

@Serializable
data class OtakuSettings(
    val lastFavoritesSync: Long,
)

class SettingsSerializer : Serializer<OtakuSettings> {

    override val defaultValue = OtakuSettings(lastFavoritesSync = 0)

    override suspend fun readFrom(input: InputStream): OtakuSettings = try {
        Json.decodeFromString(
            OtakuSettings.serializer(),
            input.readBytes().decodeToString()
        )
    } catch (serialization: SerializationException) {
        throw CorruptionException("Unable to read Settings", serialization)
    }

    override suspend fun writeTo(t: OtakuSettings, output: OutputStream) {
        output.write(
            Json.encodeToString(OtakuSettings.serializer(), t)
                .encodeToByteArray()
        )
    }
}