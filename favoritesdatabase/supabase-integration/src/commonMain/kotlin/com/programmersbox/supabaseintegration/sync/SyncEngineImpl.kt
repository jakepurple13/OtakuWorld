package com.programmersbox.supabaseintegration.sync

import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.request.SelectRequestBuilder
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.measureTime

class SyncEngineImpl(
    private val clientProvider: SupabaseClientProvider,
    private val authManager: AuthManager,
    private val itemDao: ItemDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val notesDao: NotesDao,
    private val listDao: ListDao,
    private val heatMapDao: HeatMapDao,
) : SyncEngine {

    private val client get() = clientProvider.getOrCreate() ?: error("Client not initialized")
    private val userId
        get() = (authManager.authState.value as? AuthState.Authenticated)?.user?.id
            ?: error("Not authenticated")

    override suspend fun pushLocalChanges(): Unit = coroutineScope {
        if (!connectivityMonitor.isOnline.value) return@coroutineScope
        val uid = userId

        //TODO: Maybe make something similar to the backupprocessor for these?
        pushAndRecordTime("favorites") { pushFavorites(uid) }
        pushAndRecordTime("chapters") { pushChapters(uid) }
        pushAndRecordTime("bookmarks") { pushBookmarks(uid) }
        pushAndRecordTime("notes") { pushNotes(uid) }
        pushAndRecordTime("history") { pushHistory(uid) }
        pushAndRecordTime("customlist") {
            runCatching { pushCustomListItems(uid) }
            runCatching { pushCustomListInfo(uid) }
        }
        pushAndRecordTime("heatmap") { pushHeatMap(uid) }
    }

    private suspend fun pushFavorites(uid: String) {
        val dirty = itemDao.getDirtyFavorites()
        if (dirty.isEmpty()) return
        println("Pushing ${dirty.size} favorites")
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                client.postgrest["favorite_items"].upsert(model.toFavoriteRow(uid, timestamp)) {
                    onConflict = "user_id,url"
                }
                itemDao.markFavoriteSynced(model.url, timestamp)
                if (model.isDeleted) {
                    itemDao.deleteFavorite(model)
                }
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushChapters(uid: String) {
        val dirty = itemDao.getDirtyChapters()
        if (dirty.isEmpty()) return
        val errors = mutableListOf<Throwable>()
        val deletedList = dirty.filter { it.isDeleted }

        println("Pushing ${dirty.size} | deleted: ${deletedList.size} | updated: ${dirty.size - deletedList.size} | chapters")

        // 1. Bulk Upsert ALL items (including tombstones) in chunks
        dirty.chunked(500).forEach { chunk ->
            runCatching {
                val rowsToUpsert = chunk.map { model ->
                    val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                    model.toChapterRow(uid, timestamp)
                }
                client.postgrest["chapters_watched"].upsert(rowsToUpsert) {
                    onConflict = "user_id,url"
                }
                // Mark synced for this chunk
                chunk.forEach { model ->
                    val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                    itemDao.markChapterSynced(model.url, timestamp)
                }
            }.onFailure { errors.add(it) }
        }

        // 2. After successful tombstone upsert, hard-delete the deleted ones locally
        if (errors.isEmpty()) {
            deletedList.forEach { model ->
                runCatching { itemDao.deleteChapter(model) }.onFailure { errors.add(it) }
            }
        }

        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushBookmarks(uid: String) {
        val dirty = bookmarkDao.getDirtyBookmarks()
        if (dirty.isEmpty()) return
        println("Pushing ${dirty.size} bookmarks")
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                client.postgrest["bookmarked_chapters"].upsert(model.toBookmarkedChapterRow(uid, timestamp)) {
                    onConflict = "user_id,chapter_url"
                }
                bookmarkDao.markBookmarkSynced(model.chapterUrl, timestamp)
                if (model.isDeleted) {
                    bookmarkDao.deleteBookmark(model)
                }
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushNotes(uid: String) {
        val dirty = notesDao.getDirtyNotes()
        if (dirty.isEmpty()) return
        println("Pushing ${dirty.size} notes")
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                client.postgrest["notes"].upsert(model.toNoteItemRow(uid, timestamp)) {
                    onConflict = "user_id,item_url"
                }
                notesDao.markNoteSynced(model.itemUrl, timestamp)
                if (model.isDeleted) {
                    notesDao.deleteNote(model.itemUrl)
                }
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushHistory(uid: String) {
        val dirty = historyDao.getDirtyHistory()
        if (dirty.isEmpty()) return
        println("Pushing ${dirty.size} history")
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                client.postgrest["history"].upsert(model.toHistoryItemRow(uid, timestamp)) {
                    onConflict = "user_id,search_text"
                }
                historyDao.markHistorySynced(model.searchText, timestamp)
                if (model.isDeleted) {
                    historyDao.deleteHistory(model)
                }
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushCustomListItems(uid: String) {
        val dirty = listDao.getDirtyCustomListItems()
        if (dirty.isEmpty()) return
        println("Pushing ${dirty.size} custom list items")
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                client.postgrest["custom_list_items"].upsert(model.toCustomListItemRow(uid, timestamp)) {
                    onConflict = "user_id,uuid"
                }
                listDao.markCustomListItemSynced(model.uuid, timestamp)
                if (model.isDeleted) {
                    listDao.removeList(model)
                }
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushCustomListInfo(uid: String) {
        val dirty = listDao.getDirtyCustomListInfo()
        if (dirty.isEmpty()) return
        println("Pushing ${dirty.size} custom list info")
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                client.postgrest["custom_list_info"].upsert(model.toCustomListInfoRow(uid, timestamp)) {
                    onConflict = "user_id,unique_id"
                }
                listDao.markCustomListInfoSynced(model.uniqueId, timestamp)
                if (model.isDeleted) {
                    listDao.removeItem(model)
                }
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushHeatMap(uid: String) {
        val dirty = heatMapDao.getDirtyHeatMapItems()
        if (dirty.isEmpty()) return
        println("Pushing ${dirty.size} heatmap items")
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                client.postgrest["heatmap_items"].upsert(model.toHeatMapItemRow(uid, timestamp)) {
                    onConflict = "user_id,time"
                }
                heatMapDao.markHeatMapItemSynced(model.time, timestamp)
                if (model.isDeleted) {
                    heatMapDao.deleteHeatMap(model)
                }
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    override suspend fun pullRemoteChanges(since: Long, tables: Set<String>?) = coroutineScope {
        if (!connectivityMonitor.isOnline.value) return@coroutineScope
        val uid = userId
        fun wants(table: String) = tables == null || table in tables

        if (wants("favorite_items"))      pullAndRecordTime("favorites") { pullFavorites(uid, since) }
        if (wants("chapters_watched"))    pullAndRecordTime("chapters") { pullChapters(uid, since) }
        if (wants("bookmarked_chapters")) pullAndRecordTime("bookmarks") { pullBookmarks(uid, since) }
        if (wants("notes"))               pullAndRecordTime("notes") { pullNotes(uid, since) }
        if (wants("history"))             pullAndRecordTime("history") { pullHistory(uid, since) }
        if (wants("custom_list_items") || wants("custom_list_info"))
                                          pullAndRecordTime("customlist") { pullLists(uid, since) }
        if (wants("heatmap_items"))       pullAndRecordTime("heatmap") { pullHeatMap(uid, since) }
    }

    private suspend inline fun <reified T> fetchAllRecords(
        tableName: String,
        selection: SelectRequestBuilder.() -> Unit = {},
    ): List<T> {
        val allRecords = mutableListOf<T>()
        val pageSize = 1000L
        var offset = 0L

        //TODO: Only print in debug builds
        /*client
            .postgrest[tableName]
            .select {
                count(Count.EXACT)
                selection()
            }
            .countOrNull()
            ?.let { totalCount -> println("Total records in $tableName: $totalCount") }*/

        while (true) {
            // Calculate the inclusive end index
            val toIndex = offset + pageSize - 1

            // Fetch the current batch
            val batch = client.postgrest[tableName].select {
                range(offset, toIndex)
                selection()
            }.decodeList<T>()

            allRecords.addAll(batch)

            // If we received fewer records than the page size, we've reached the end
            if (batch.size < pageSize) {
                break
            }

            // Move the offset forward for the next iteration
            offset += pageSize
        }

        return allRecords
    }

    private suspend fun pullFavorites(
        uid: String,
        since: Long,
    ) {
        fetchAllRecords<FavoriteItemRow>("favorite_items") {
            filter {
                eq("user_id", uid)
                gt("updated_at", since)
            }
        }
            .also { println("Pulling ${it.size} favorites") }
            .forEach { row ->
                val local = itemDao.getFavoriteByUrl(row.url)
                if (local == null || row.updatedAt > local.updatedAt) {
                    if (row.isDeleted) {
                        if (local != null) itemDao.deleteFavorite(local)
                    } else {
                        itemDao.insertFavorite(row.toDbModel())
                    }
                }
            }
    }

    private suspend fun pullChapters(
        uid: String,
        since: Long,
    ) {
        fetchAllRecords<ChapterWatchedRow>("chapters_watched") {
            filter {
                eq("user_id", uid)
                gt("updated_at", since)
            }
        }
            .also { println("Pulling ${it.size} chapters") }
            .forEach { row ->
                val local = itemDao.getChapterByUrl(row.url)
                if (local == null || row.updatedAt > local.updatedAt) {
                    if (row.isDeleted) {
                        if (local != null) itemDao.deleteChapter(local)
                    } else {
                        itemDao.insertChapterWatched(row.toChapterWatched())
                    }
                }
            }
    }

    private suspend fun pullBookmarks(
        uid: String,
        since: Long,
    ) {
        fetchAllRecords<BookmarkedChapterRow>("bookmarked_chapters") {
            filter {
                eq("user_id", uid)
                gt("updated_at", since)
            }
        }
            .also { println("Pulling ${it.size} bookmarks") }
            .forEach { row ->
                val local = bookmarkDao.getBookmarkByChapterUrl(row.chapterUrl)
                if (local == null || row.updatedAt > local.updatedAt) {
                    if (row.isDeleted) {
                        if (local != null) bookmarkDao.deleteBookmark(local)
                    } else {
                        bookmarkDao.insertBookmark(row.toBookmarkedChapter())
                    }
                }
            }
    }

    private suspend fun pullNotes(
        uid: String,
        since: Long,
    ) {
        fetchAllRecords<NoteItemRow>("notes") {
            filter {
                eq("user_id", uid)
                gt("updated_at", since)
            }
        }
            .also { println("Pulling ${it.size} notes") }
            .forEach { row ->
                val local = notesDao.getNoteByUrl(row.itemUrl)
                if (local == null || row.updatedAt > local.updatedAt) {
                    if (row.isDeleted) {
                        if (local != null) notesDao.deleteNote(local.itemUrl)
                    } else {
                        notesDao.upsertNote(row.toNoteItem())
                    }
                }
            }
    }

    private suspend fun pullHistory(
        uid: String,
        since: Long,
    ) {
        fetchAllRecords<HistoryItemRow>("history") {
            filter {
                eq("user_id", uid)
                gt("updated_at", since)
            }
        }
            .also { println("Pulling ${it.size} history") }
            .forEach { row ->
                val local = historyDao.getHistoryByKey(row.searchText)
                if (local == null || row.updatedAt > local.updatedAt) {
                    if (row.isDeleted) {
                        if (local != null) historyDao.deleteHistory(local)
                    } else {
                        historyDao.insertHistory(row.toHistoryItem())
                    }
                }
            }
    }

    private suspend fun pullLists(
        uid: String,
        since: Long,
    ) {
        fetchAllRecords<CustomListItemRow>("custom_list_items") {
            filter {
                eq("user_id", uid)
                gt("updated_at", since)
            }
        }
            .also { println("Pulling ${it.size} custom list items") }
            .forEach { row ->
                val local = listDao.getCustomListItemByUuid(row.uuid)
                if (local == null || row.updatedAt > local.updatedAt) {
                    if (row.isDeleted) {
                        if (local != null) listDao.removeList(local)
                    } else {
                        val item = row.toCustomListItem()
                        if (local == null) {
                            listDao.createList(item)
                        } else {
                            listDao.updateCustomListItem(item)
                        }
                    }
                }
            }

        fetchAllRecords<CustomListInfoRow>("custom_list_info") {
            filter {
                eq("user_id", uid)
                gt("updated_at", since)
            }
        }
            .also { println("Pulling ${it.size} custom list info") }
            .forEach { row ->
                val local = listDao.getCustomListInfoByUniqueId(row.uniqueId)
                if (local == null || row.updatedAt > local.updatedAt) {
                    if (row.isDeleted) {
                        if (local != null) listDao.removeItem(local)
                    } else {
                        val info = row.toCustomListInfo()
                        if (local == null) {
                            listDao.addItem(info)
                        } else {
                            listDao.updateCustomListInfo(info)
                        }
                    }
                }
            }
    }

    private suspend fun pullHeatMap(
        uid: String,
        since: Long,
    ) {
        fetchAllRecords<HeatMapItemRow>("heatmap_items") {
            filter {
                eq("user_id", uid)
                gt("updated_at", since)
            }
        }
            .also { println("Pulling ${it.size} heatmap items") }
            .forEach { row ->
                val localDate = LocalDate.parse(row.time)
                val local = heatMapDao.getHeatMapItemByTime(localDate)
                if (local == null || row.updatedAt > local.updatedAt) {
                    if (row.isDeleted) {
                        if (local != null) heatMapDao.deleteHeatMap(local)
                    } else {
                        heatMapDao.insertHeatMap(row.toHeatMapItem())
                    }
                }
            }
    }

    private fun CoroutineScope.pushAndRecordTime(dbId: String, block: suspend () -> Unit) =
        handleAndRecordTime(dbId, "Pushing", block)

    private fun CoroutineScope.pullAndRecordTime(dbId: String, block: suspend () -> Unit) =
        handleAndRecordTime(dbId, "Pulling", block)

    private fun CoroutineScope.handleAndRecordTime(
        dbId: String,
        direction: String,
        block: suspend () -> Unit,
    ) {
        launch(Dispatchers.IO) {
            val duration = measureTime {
                runCatching { block() }
            }

            println("$direction $dbId took $duration")
        }
    }

    override suspend fun fullSync() {
        pushLocalChanges()
        pullRemoteChanges(since = 0L)
    }

    override fun subscribeRealtime(scope: CoroutineScope, onEvent: suspend (Set<String>) -> Unit): Job = scope.launch {
        val uid = userId
        val channel = client.channel("otakuworld-sync-$uid")

        // Buffered: preserves table names so the consumer knows exactly which tables changed.
        val trigger = Channel<String>(Channel.BUFFERED)

        val tables = listOf(
            "favorite_items", "chapters_watched", "bookmarked_chapters",
            "notes", "history", "custom_list_items", "custom_list_info", "heatmap_items",
        )

        tables.forEach { table ->
            channel.postgresChangeFlow<PostgresAction>("public") {
                this.table = table
                filter("user_id", FilterOperator.EQ, uid)
            }.onEach { trigger.trySend(table) }
             .launchIn(this)
        }

        // Single-consumer loop — drains all queued table names into a Set, then syncs only those tables.
        launch {
            for (first in trigger) {
                val changed = mutableSetOf(first)
                var next = trigger.tryReceive()
                while (next.isSuccess) {
                    next.getOrNull()?.let { changed.add(it) }
                    next = trigger.tryReceive()
                }
                onEvent(changed)
            }
        }

        try {
            channel.subscribe(blockUntilSubscribed = true)
            awaitCancellation()
        } finally {
            trigger.close()
            channel.unsubscribe()
            runCatching { client.realtime.removeChannel(channel) }
        }
    }
}
