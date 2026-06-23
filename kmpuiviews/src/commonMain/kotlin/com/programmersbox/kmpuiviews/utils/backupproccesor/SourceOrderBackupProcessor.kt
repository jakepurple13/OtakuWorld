package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class SourceOrderBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor() {
    override val fileName: String
        get() = "source_order.json"

    override suspend fun backup(sink: BufferedSink) {
        itemDao
            .getSourceOrderSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<SourceOrder>>()
            .forEach { itemDao.insertSourceOrder(it) }
    }
}