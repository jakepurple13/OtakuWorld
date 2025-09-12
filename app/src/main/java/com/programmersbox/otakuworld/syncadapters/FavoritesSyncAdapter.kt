package com.programmersbox.otakuworld.syncadapters

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.programmersbox.otakuworld.OtakuFavoritesContentProviderHelper
import com.programmersbox.otakuworld.repository.ServerHandler
import kotlinx.coroutines.runBlocking

class FavoritesSyncAdapter(
    context: Context,
    private val serverHandler: ServerHandler,
) : AbstractThreadedSyncAdapter(context, true, false) {
    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?,
    ) {
        authority ?: return
        syncResult ?: return
        val helper = OtakuFavoritesContentProviderHelper(authority)

        // 1) Fetch local favorites
        val localFavorites = runCatching { helper.getAllFavoritesAsList(context) }
            .getOrElse {
                syncResult.stats?.numIoExceptions = (syncResult.stats?.numIoExceptions ?: 0) + 1
                emptyList()
            }

        // 2) Fetch remote favorites from GET endpoint
        val remoteFavorites = runCatching { runBlocking { serverHandler.getFavorites() } }
            .getOrElse {
                // If remote fetch fails, bail out gracefully
                syncResult.databaseError = true
                emptyList()
            }

        // Index by URL for quick diff
        val localByUrl = localFavorites.associateBy { it.url }
        val remoteByUrl = remoteFavorites.associateBy { it.url }

        // 3) Pull: Add remote-only items to local provider
        val toInsertLocally = remoteByUrl.keys.minus(localByUrl.keys).mapNotNull { remoteByUrl[it] }
        toInsertLocally.forEach { model ->
            runCatching { helper.insertFavorite(context, model) }
                .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
        }

        // 4) Push: Add local-only items to remote (server upsert)
        val toPushRemotely = localByUrl.keys.minus(remoteByUrl.keys).mapNotNull { localByUrl[it] }
        toPushRemotely.forEach { model ->
            runCatching { runBlocking { serverHandler.upsertFavorite(model) } }
                .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
        }

        // 5) Resolve conflicts: If present in both but different, prefer server and update local
        val intersecting = localByUrl.keys.intersect(remoteByUrl.keys)
        val toUpdateLocal = intersecting.mapNotNull { url ->
            val local = localByUrl[url]
            val remote = remoteByUrl[url]
            if (local != null && remote != null && local != remote) {
                listOf(local, remote).maxByOrNull { it.numChapters }
            } else null
        }
        toUpdateLocal.forEach { model ->
            runCatching { helper.updateFavorite(context, model) }
                .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
        }

        // 6) Optional: If extras specify that server is source of truth, delete local items missing on server
        //TODO: Make this a datastore value that user decides...
        // MAYBE there can also be a sync with server and sync with local?
        val serverIsSource = extras?.getBoolean("server_is_source_of_truth", false) == true
        if (serverIsSource) {
            val toDeleteLocally = localByUrl.keys.minus(remoteByUrl.keys)
            toDeleteLocally.forEach { url ->
                runCatching { helper.deleteFavorite(context, url) }
                    .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
            }
        }

        // Optional logging of extras for debugging
        runCatching {
            val keySet = extras?.keySet()
            if (keySet?.isNotEmpty() == true) {
                keySet.forEach { key ->
                    runCatching { println("[FavoritesSyncAdapter] Extra: " + key + " = " + extras.get(key)) }
                }
            }
        }
    }
}