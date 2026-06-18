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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

class SyncEngineImpl(
    private val clientProvider: SupabaseClientProvider,
    private val authManager: AuthManager,
    private val itemDao: ItemDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val historyDao: HistoryDao? = null,
    private val bookmarkDao: BookmarkDao? = null,
    private val notesDao: NotesDao? = null,
    private val listDao: ListDao? = null,
    private val heatMapDao: HeatMapDao? = null,
) : SyncEngine {

    private val client get() = clientProvider.getOrCreate() ?: error("Client not initialized")
    private val userId get() = (authManager.authState.value as? AuthState.Authenticated)?.user?.id
        ?: error("Not authenticated")

    override suspend fun pushLocalChanges(): Unit = coroutineScope {
        if (!connectivityMonitor.isOnline.value) return@coroutineScope
        val uid = userId
        launch { pushFavorites(uid) }
        launch { pushChapters(uid) }
        launch { pushBookmarks(uid) }
        launch { pushNotes(uid) }
        launch { pushHistory(uid) }
        launch { pushCustomListItems(uid) }
        launch { pushCustomListInfo(uid) }
        launch { pushHeatMap(uid) }
    }

    private suspend fun pushFavorites(uid: String) {
        val dirty = itemDao.getDirtyFavorites()
        if (dirty.isEmpty()) return
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                if (model.isDeleted) {
                    client.postgrest["favorite_items"].delete {
                        filter { eq("user_id", uid); eq("url", model.url) }
                    }
                } else {
                    client.postgrest["favorite_items"].upsert(model.toFavoriteRow(uid, timestamp))
                }
                itemDao.markFavoriteSynced(model.url, timestamp)
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushChapters(uid: String) {
        val dirty = itemDao.getDirtyChapters()
        if (dirty.isEmpty()) return
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                if (model.isDeleted) {
                    client.postgrest["chapters_watched"].delete {
                        filter { eq("user_id", uid); eq("url", model.url) }
                    }
                } else {
                    client.postgrest["chapters_watched"].upsert(model.toChapterRow(uid, timestamp))
                }
                itemDao.markChapterSynced(model.url, timestamp)
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushBookmarks(uid: String) {
        val dao = bookmarkDao ?: return
        val dirty = dao.getDirtyBookmarks()
        if (dirty.isEmpty()) return
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                if (model.isDeleted) {
                    client.postgrest["bookmarked_chapters"].delete {
                        filter { eq("user_id", uid); eq("chapter_url", model.chapterUrl) }
                    }
                } else {
                    client.postgrest["bookmarked_chapters"].upsert(model.toBookmarkedChapterRow(uid, timestamp))
                }
                dao.markBookmarkSynced(model.chapterUrl, timestamp)
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushNotes(uid: String) {
        val dao = notesDao ?: return
        val dirty = dao.getDirtyNotes()
        if (dirty.isEmpty()) return
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                if (model.isDeleted) {
                    client.postgrest["notes"].delete {
                        filter { eq("user_id", uid); eq("item_url", model.itemUrl) }
                    }
                } else {
                    client.postgrest["notes"].upsert(model.toNoteItemRow(uid, timestamp))
                }
                dao.markNoteSynced(model.itemUrl, timestamp)
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushHistory(uid: String) {
        val dao = historyDao ?: return
        val dirty = dao.getDirtyHistory()
        if (dirty.isEmpty()) return
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                if (model.isDeleted) {
                    client.postgrest["history"].delete {
                        filter { eq("user_id", uid); eq("search_text", model.searchText) }
                    }
                } else {
                    client.postgrest["history"].upsert(model.toHistoryItemRow(uid, timestamp))
                }
                dao.markHistorySynced(model.searchText, timestamp)
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushCustomListItems(uid: String) {
        val dao = listDao ?: return
        val dirty = dao.getDirtyCustomListItems()
        if (dirty.isEmpty()) return
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                if (model.isDeleted) {
                    client.postgrest["custom_list_items"].delete {
                        filter { eq("user_id", uid); eq("uuid", model.uuid) }
                    }
                } else {
                    client.postgrest["custom_list_items"].upsert(model.toCustomListItemRow(uid, timestamp))
                }
                dao.markCustomListItemSynced(model.uuid, timestamp)
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushCustomListInfo(uid: String) {
        val dao = listDao ?: return
        val dirty = dao.getDirtyCustomListInfo()
        if (dirty.isEmpty()) return
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                if (model.isDeleted) {
                    client.postgrest["custom_list_info"].delete {
                        filter { eq("user_id", uid); eq("unique_id", model.uniqueId) }
                    }
                } else {
                    client.postgrest["custom_list_info"].upsert(model.toCustomListInfoRow(uid, timestamp))
                }
                dao.markCustomListInfoSynced(model.uniqueId, timestamp)
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    private suspend fun pushHeatMap(uid: String) {
        val dao = heatMapDao ?: return
        val dirty = dao.getDirtyHeatMapItems()
        if (dirty.isEmpty()) return
        val errors = mutableListOf<Throwable>()
        dirty.forEach { model ->
            runCatching {
                val timestamp = if (model.updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else model.updatedAt
                if (model.isDeleted) {
                    client.postgrest["heatmap_items"].delete {
                        filter { eq("user_id", uid); eq("time", model.time.toString()) }
                    }
                } else {
                    client.postgrest["heatmap_items"].upsert(model.toHeatMapItemRow(uid, timestamp))
                }
                dao.markHeatMapItemSynced(model.time, timestamp)
            }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) throw errors.first()
    }

    override suspend fun pullRemoteChanges(since: Long) {
        if (!connectivityMonitor.isOnline.value) return
        val uid = userId

        client.postgrest["favorite_items"]
            .select { filter { eq("user_id", uid); gt("updated_at", since) } }
            .decodeList<FavoriteItemRow>()
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

        client.postgrest["chapters_watched"]
            .select { filter { eq("user_id", uid); gt("updated_at", since) } }
            .decodeList<ChapterWatchedRow>()
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

        bookmarkDao?.let { dao ->
            client.postgrest["bookmarked_chapters"]
                .select { filter { eq("user_id", uid); gt("updated_at", since) } }
                .decodeList<BookmarkedChapterRow>()
                .forEach { row ->
                    val local = dao.getBookmarkByChapterUrl(row.chapterUrl)
                    if (local == null || row.updatedAt > local.updatedAt) {
                        if (row.isDeleted) {
                            if (local != null) dao.deleteBookmark(local)
                        } else {
                            dao.insertBookmark(row.toBookmarkedChapter())
                        }
                    }
                }
        }

        notesDao?.let { dao ->
            client.postgrest["notes"]
                .select { filter { eq("user_id", uid); gt("updated_at", since) } }
                .decodeList<NoteItemRow>()
                .forEach { row ->
                    val local = dao.getNoteByUrl(row.itemUrl)
                    if (local == null || row.updatedAt > local.updatedAt) {
                        if (row.isDeleted) {
                            if (local != null) dao.deleteNote(local.itemUrl)
                        } else {
                            dao.upsertNote(row.toNoteItem())
                        }
                    }
                }
        }

        historyDao?.let { dao ->
            client.postgrest["history"]
                .select { filter { eq("user_id", uid); gt("updated_at", since) } }
                .decodeList<HistoryItemRow>()
                .forEach { row ->
                    val local = dao.getHistoryByKey(row.searchText)
                    if (local == null || row.updatedAt > local.updatedAt) {
                        if (row.isDeleted) {
                            if (local != null) dao.deleteHistory(local)
                        } else {
                            dao.insertHistory(row.toHistoryItem())
                        }
                    }
                }
        }

        listDao?.let { dao ->
            client.postgrest["custom_list_items"]
                .select { filter { eq("user_id", uid); gt("updated_at", since) } }
                .decodeList<CustomListItemRow>()
                .forEach { row ->
                    val local = dao.getCustomListItemByUuid(row.uuid)
                    if (local == null || row.updatedAt > local.updatedAt) {
                        if (row.isDeleted) {
                            if (local != null) dao.removeList(local)
                        } else {
                            val item = row.toCustomListItem()
                            if (local == null) {
                                dao.createList(item)
                            } else {
                                dao.updateCustomListItem(item)
                            }
                        }
                    }
                }

            client.postgrest["custom_list_info"]
                .select { filter { eq("user_id", uid); gt("updated_at", since) } }
                .decodeList<CustomListInfoRow>()
                .forEach { row ->
                    val local = dao.getCustomListInfoByUniqueId(row.uniqueId)
                    if (local == null || row.updatedAt > local.updatedAt) {
                        if (row.isDeleted) {
                            if (local != null) dao.removeItem(local)
                        } else {
                            val info = row.toCustomListInfo()
                            if (local == null) {
                                dao.addItem(info)
                            } else {
                                dao.updateCustomListInfo(info)
                            }
                        }
                    }
                }
        }

        heatMapDao?.let { dao ->
            client.postgrest["heatmap_items"]
                .select { filter { eq("user_id", uid); gt("updated_at", since) } }
                .decodeList<HeatMapItemRow>()
                .forEach { row ->
                    val localDate = LocalDate.parse(row.time)
                    val local = dao.getHeatMapItemByTime(localDate)
                    if (local == null || row.updatedAt > local.updatedAt) {
                        if (row.isDeleted) {
                            if (local != null) dao.deleteHeatMap(local)
                        } else {
                            dao.insertHeatMap(row.toHeatMapItem())
                        }
                    }
                }
        }
    }

    override suspend fun fullSync() {
        pushLocalChanges()
        pullRemoteChanges(since = 0L)
    }
}
