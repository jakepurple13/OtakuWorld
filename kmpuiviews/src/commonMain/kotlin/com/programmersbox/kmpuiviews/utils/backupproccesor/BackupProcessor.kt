package com.programmersbox.kmpuiviews.utils.backupproccesor

import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

abstract class BackupProcessor {
    abstract val fileName: String
    abstract suspend fun backup(sink: BufferedSink)
    abstract suspend fun restore(json: String, bufferedSource: BufferedSource)

    protected inline fun <reified T> T.toJson() = Json.encodeToString(this)
    protected inline fun <reified T> String.fromJson() = Json.decodeFromString<T>(this)
}

