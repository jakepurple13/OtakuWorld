package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /** When non-null, only lists whose [com.programmersbox.favoritesdatabase.CustomListItem.uuid] is in this set are backed up/restored. */
    var listIdFilter: Set<String>? = null

    private val listFilterMutex = Mutex()

    /**
     * Runs [block] with [listIdFilter] set to [ids], resetting it afterward — serialized by a
     * mutex so a shared (e.g. JVM singleton-scoped) Zipper never lets one call's filter leak into
     * another's in-flight backup/restore.
     */
    suspend fun <R> withListFilter(ids: Set<String>?, block: suspend () -> R): R = listFilterMutex.withLock {
        listIdFilter = ids
        try {
            block()
        } finally {
            listIdFilter = null
        }
    }

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val lists = filterByListId(listDao.getAllListsSync())
        lists.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = lists.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult {
        val lists = filterByListId(json.fromJson<List<CustomList>>())
        return lists.restoreEachCatching(idOf = { it.item.name }) {
            listRepository.createList(it.item)
            it.list.forEach { listItem -> listRepository.addItem(listItem) }
        }
    }

    /** Parses a raw `lists.json` entry's contents, for previewing a zip's lists before restoring. */
    fun parseLists(json: String): List<CustomList> = json.fromJson()

    private fun filterByListId(lists: List<CustomList>): List<CustomList> =
        listIdFilter?.let { ids -> lists.filter { it.item.uuid in ids } } ?: lists

    override suspend fun currentSummary() = BackupDataSummary(itemCount = listDao.getAllListsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<CustomList>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
