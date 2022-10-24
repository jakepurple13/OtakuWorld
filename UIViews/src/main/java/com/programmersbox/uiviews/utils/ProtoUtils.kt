package com.programmersbox.uiviews.utils

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.GeneratedMessageLite
import com.google.protobuf.InvalidProtocolBufferException
import com.programmersbox.uiviews.Settings
import com.programmersbox.uiviews.SystemThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

suspend fun <DS : DataStore<MessageType>, MessageType : GeneratedMessageLite<MessageType, BuilderType>, BuilderType : GeneratedMessageLite.Builder<MessageType, BuilderType>> DS.update(
    statsBuilder: suspend BuilderType.() -> BuilderType
) = updateData { statsBuilder(it.toBuilder()).build() }

interface GenericSerializer<MessageType, BuilderType> : Serializer<MessageType>
        where MessageType : GeneratedMessageLite<MessageType, BuilderType>,
              BuilderType : GeneratedMessageLite.Builder<MessageType, BuilderType> {

    /**
     * Call MessageType::parseFrom here!
     */
    val parseFrom: (input: InputStream) -> MessageType

    override suspend fun readFrom(input: InputStream): MessageType =
        withContext(Dispatchers.IO) {
            try {
                parseFrom(input)
            } catch (exception: InvalidProtocolBufferException) {
                throw CorruptionException("Cannot read proto.", exception)
            }
        }

    override suspend fun writeTo(t: MessageType, output: OutputStream) =
        withContext(Dispatchers.IO) { t.writeTo(output) }
}

val Context.settings: DataStore<Settings> by dataStore(
    fileName = "Settings",
    serializer = SettingsSerializer
)

object SettingsSerializer : GenericSerializer<Settings, Settings.Builder> {
    override val defaultValue: Settings
        get() = Settings.getDefaultInstance()
            .toBuilder()
            .build()
    override val parseFrom: (input: InputStream) -> Settings get() = Settings::parseFrom
}

class SettingsHandling(context: Context) {
    private val preferences by lazy { context.settings }
    private val all: Flow<Settings> get() = preferences.data

    val systemThemeMode = all.map { it.themeSetting }
    suspend fun setSystemThemeMode(mode: SystemThemeMode) = preferences.update { setThemeSetting(mode) }
}