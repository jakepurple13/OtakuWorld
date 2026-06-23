package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class IncognitoBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor() {
    override val fileName: String
        get() = "incognito_sources.json"

    override suspend fun backup(sink: BufferedSink) {
        itemDao
            .getAllIncognitoSourcesSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<IncognitoSource>>()
            .forEach { itemDao.insertIncognitoSource(it) }
    }
}