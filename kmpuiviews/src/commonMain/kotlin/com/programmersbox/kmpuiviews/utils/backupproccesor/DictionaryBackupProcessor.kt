package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.DictionaryDao
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class DictionaryBackupProcessor(
    private val dictionaryDao: DictionaryDao,
) : BackupProcessor() {
    override val fileName: String = "dictionary.json"

    override suspend fun backup(sink: BufferedSink) {
        dictionaryDao
            .getAllSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<DictionaryEntry>>()
            .forEach { dictionaryDao.insert(it) }
    }
}