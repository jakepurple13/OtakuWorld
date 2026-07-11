package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class ListBackupProcessor(
    private val listRepository: ListRepository,
    private val listDao: ListDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "lists.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Custom Lists"
    override val description: String? get() = "User-created custom lists"
    override val icon get() = Icons.Default.FormatListBulleted

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

    override suspend fun currentSummary() = BackupDataSummary(itemCount = listDao.getAllListsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<CustomList>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
