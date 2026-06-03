package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryItem
import okio.BufferedSink
import okio.BufferedSource

class HistoryBackupProcessor(
    private val historyDao: HistoryDao,
) : BackupProcessor() {
    override val fileName: String
        get() = "history.json"

    override suspend fun backup(sink: BufferedSink) {
        historyDao
            .getAllHistorySync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<HistoryItem>>().forEach { historyDao.insertHistory(it) }
    }
}