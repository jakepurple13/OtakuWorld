package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class ListBackupProcessor(
    private val listRepository: ListRepository,
    private val listDao: ListDao,
) : BackupProcessor() {
    override val fileName: String
        get() = "lists.json"

    override suspend fun backup(sink: BufferedSink) {
        listDao
            .getAllListsSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<CustomList>>()
            .forEach {
                listRepository.createList(it.item)
                it.list.forEach { listItem -> listRepository.addItem(listItem) }
            }
    }
}