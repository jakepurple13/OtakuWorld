package com.programmersbox.supabaseintegration.sync

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class SyncEngineImpl(
    private val clientProvider: SupabaseClientProvider,
    private val authManager: AuthManager,
    private val itemDao: ItemDao,
    private val connectivityMonitor: ConnectivityMonitor,
) : SyncEngine {

    private val client get() = clientProvider.getOrCreate() ?: error("Client not initialized")
    private val userId get() = (authManager.authState.value as? AuthState.Authenticated)?.user?.id
        ?: error("Not authenticated")

    override suspend fun pushLocalChanges(): Unit = coroutineScope {
        if (!connectivityMonitor.isOnline.value) return@coroutineScope
        val uid = userId
        launch {
            itemDao.getDirtyFavorites().forEach { model ->
                runCatching {
                    if (model.isDeleted) {
                        client.postgrest["favorite_items"].delete {
                            filter { eq("user_id", uid); eq("url", model.url) }
                        }
                    } else {
                        client.postgrest["favorite_items"].upsert(model.toFavoriteRow(uid))
                    }
                    itemDao.updateFavorite(model.copy(isDirty = false))
                }
            }
        }
        launch {
            itemDao.getDirtyChapters().forEach { model ->
                runCatching {
                    if (model.isDeleted) {
                        client.postgrest["chapters_watched"].delete {
                            filter { eq("user_id", uid); eq("url", model.url) }
                        }
                    } else {
                        client.postgrest["chapters_watched"].upsert(model.toChapterRow(uid))
                    }
                    itemDao.updateChapterWatched(model.copy(isDirty = false))
                }
            }
        }
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
                    itemDao.insertFavorite(row.toDbModel())
                }
            }
        client.postgrest["chapters_watched"]
            .select { filter { eq("user_id", uid); gt("updated_at", since) } }
            .decodeList<ChapterWatchedRow>()
            .forEach { row ->
                val local = itemDao.getChapterByUrl(row.url)
                if (local == null || row.updatedAt > local.updatedAt) {
                    itemDao.insertChapterWatched(row.toChapterWatched())
                }
            }
    }

    override suspend fun fullSync() {
        pushLocalChanges()
        pullRemoteChanges(since = 0L)
    }
}
