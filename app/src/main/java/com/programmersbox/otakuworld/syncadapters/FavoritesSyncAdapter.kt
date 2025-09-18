package com.programmersbox.otakuworld.syncadapters

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import androidx.core.os.bundleOf
import com.programmersbox.otakuworld.DataStoreHandling
import com.programmersbox.otakuworld.MultiprocessDataStoreHandler
import com.programmersbox.otakuworld.providers.OtakuFavoritesContentProviderHelper
import com.programmersbox.otakuworld.repository.ServerHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat

class FavoritesSyncAdapter(
    context: Context,
    private val serverHandler: ServerHandler,
    private val dataStoreHandling: DataStoreHandling,
    private val multiprocessDataStoreHandler: MultiprocessDataStoreHandler,
) : BaseSyncAdapter(context, true, false) {
    private val dispatchers = Dispatchers.IO.limitedParallelism(5)
    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?,
    ) {
        super.onPerformSync(account, extras, authority, provider, syncResult)
        authority ?: return
        syncResult ?: return

        val app = getApp(authority) ?: return

        println("[FavoritesSyncAdapter] Syncing favorites for $app")

        val helper = OtakuFavoritesContentProviderHelper(authority)

        // 1) Fetch local favorites
        val localFavorites = runCatching { helper.getAllFavoritesAsList(context) }
            .onSuccess { println("[FavoritesSyncAdapter] Local favorites: ${it.size}") }
            .onFailure { it.printStackTrace() }
            .getOrElse {
                syncResult.stats?.numIoExceptions = (syncResult.stats?.numIoExceptions ?: 0) + 1
                emptyList()
            }

        // 2) Fetch remote favorites from GET endpoint
        val remoteFavorites = runCatching { runBlockingIo { serverHandler.getFavorites(app) } }
            .onSuccess { println("[FavoritesSyncAdapter] Remote favorites: ${it.favorites.size}") }
            .onFailure {
                it.printStackTrace()
                // If remote fetch fails, bail out gracefully
                syncResult.databaseError = true
            }
            .getOrNull() ?: return

        // Index by URL for quick diff
        var localByUrl = localFavorites.associateBy { it.url }
        var remoteByUrl = remoteFavorites.favorites.associateBy { it.url }

        // 3) Decide delete strategy FIRST to avoid re-adding items that should be deleted
        val lastSyncTime = runCatching { runBlockingIo { multiprocessDataStoreHandler.get().lastFavoritesSync } }
            .getOrDefault(0L)
        val remoteUpdated = remoteFavorites.lastTimeUpdated

        println("[FavoritesSyncAdapter] lastSyncTime=$lastSyncTime, remoteUpdated=$remoteUpdated")
        val dateFormat = SimpleDateFormat.getInstance()
        println("[FavoritesSyncAdapter] lastSyncTime=${dateFormat.format(lastSyncTime)}, remoteUpdated=${dateFormat.format(remoteUpdated)}")

        val serverIsSourceFlag = extras?.getBoolean("server_is_source_of_truth", false) == true
        val localIsSourceFlag = extras?.getBoolean("local_is_source_of_truth", false) == true
        val hasOverride = serverIsSourceFlag || localIsSourceFlag

        var doDeleteLocalMissingOnServer = false
        var doDeleteRemoteMissingOnLocal = false

        if (hasOverride) {
            doDeleteLocalMissingOnServer = serverIsSourceFlag
            doDeleteRemoteMissingOnLocal = localIsSourceFlag
            println("[FavoritesSyncAdapter] Overrides provided: serverIsSource=$serverIsSourceFlag, localIsSource=$localIsSourceFlag")
        } else {
            if (remoteUpdated == 0L || lastSyncTime == 0L || localFavorites.isEmpty() || remoteFavorites.favorites.isEmpty()) {
                // First-time sync on one side; skip deletes
                println("[FavoritesSyncAdapter] Skipping deletes (remoteUpdated=$remoteUpdated, lastSyncTime=$lastSyncTime)")
            } else if (remoteUpdated > lastSyncTime) {
                // Server has newer state
                doDeleteLocalMissingOnServer = true
                println("[FavoritesSyncAdapter] Server newer (remoteUpdated=$remoteUpdated > lastSyncTime=$lastSyncTime): will delete local-only items")
            } else if (remoteUpdated < lastSyncTime) {
                // Local has newer state
                doDeleteRemoteMissingOnLocal = true
                println("[FavoritesSyncAdapter] Local newer (remoteUpdated=$remoteUpdated < lastSyncTime=$lastSyncTime): will delete remote-only items")
            } else {
                // Equal and > 0 -> symmetric delete to converge
                doDeleteLocalMissingOnServer = true
                doDeleteRemoteMissingOnLocal = true
                println("[FavoritesSyncAdapter] Timestamps equal ($remoteUpdated). Performing symmetric deletes")
            }
        }

        var hasDeleted = false

        if (doDeleteLocalMissingOnServer) {
            hasDeleted = true
            val toDeleteLocally = localByUrl.keys.minus(remoteByUrl.keys)
            println("[FavoritesSyncAdapter] Deleting locally (missing on server): ${toDeleteLocally.size}")
            toDeleteLocally.forEach { url ->
                runCatching { helper.deleteFavorite(context, url) }
                    .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
            }
        }

        if (doDeleteRemoteMissingOnLocal) {
            hasDeleted = true
            val toDeleteRemotely = remoteByUrl.keys.minus(localByUrl.keys).mapNotNull { remoteByUrl[it] }
            println("[FavoritesSyncAdapter] Deleting remotely (missing locally): ${toDeleteRemotely.size}")
            runBlockingIo {
                toDeleteRemotely
                    .map { model ->
                        async(dispatchers, start = CoroutineStart.LAZY) {
                            runCatching { serverHandler.deleteFavorite(app, model) }
                                .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
                        }
                    }.awaitAll()
            }
        }

        // Re-fetch states after deletions to ensure diffs reflect the latest state
        val refreshedLocal = runCatching {
            if (hasDeleted) helper.getAllFavoritesAsList(context) else localFavorites
        }
            .getOrElse { emptyList() }
        val refreshedRemote = runCatching {
            if (hasDeleted) runBlockingIo { serverHandler.getFavorites(app) } else remoteFavorites
        }
            .getOrElse { remoteFavorites } // fall back to previous if failed

        localByUrl = refreshedLocal.associateBy { it.url }
        remoteByUrl = refreshedRemote.favorites.associateBy { it.url }

        // 4) Pull: Add remote-only items to local provider (after deletes)
        val toInsertLocally = remoteByUrl.keys.minus(localByUrl.keys).mapNotNull { remoteByUrl[it] }
        println("[FavoritesSyncAdapter] To insert locally (post-delete): ${toInsertLocally.size}")
        toInsertLocally
            .chunked(10)
            .forEach { items ->
                runCatching { helper.insertFavorites(context, items) }
                    .onSuccess {
                        println("[FavoritesSyncAdapter] Inserted locally: $it")
                        syncResult.stats?.numInserts = (syncResult.stats?.numInserts ?: 0) + it
                    }
                    .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
            }

        // 5) Push: Add local-only items to remote (server upsert) (after deletes)
        val toPushRemotely = localByUrl.keys.minus(remoteByUrl.keys).mapNotNull { localByUrl[it] }
        println("[FavoritesSyncAdapter] To push remotely (post-delete): ${toPushRemotely.size}")
        runBlockingIo {
            runCatching { serverHandler.upsertFavorites(app, toPushRemotely) }
                .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
        }

        // 6) Resolve conflicts: If present in both but different, prefer the item with higher numChapters
        val intersecting = localByUrl.keys.intersect(remoteByUrl.keys)
        println("[FavoritesSyncAdapter] Intersecting (post-delete): ${intersecting.size}")
        val toUpdateLocal = intersecting.mapNotNull { url ->
            val local = localByUrl[url]
            val remote = remoteByUrl[url]
            if (local != null && remote != null && local != remote) {
                listOf(local, remote).maxByOrNull { it.numChapters }
            } else null
        }
        toUpdateLocal.forEach { model ->
            runCatching { helper.updateFavorite(context, model) }
                .onSuccess { syncResult.stats?.numUpdates = (syncResult.stats?.numUpdates ?: 0) + 1 }
                .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
        }

        // Update last successful sync time
        runCatching {
            runBlockingIo { multiprocessDataStoreHandler.updateData { it.copy(lastFavoritesSync = System.currentTimeMillis()) } }
        }.onFailure { it.printStackTrace() }

        println("[FavoritesSyncAdapter] Sync complete")
    }

    private fun <T> runBlockingIo(block: suspend CoroutineScope.() -> T): T {
        return runBlocking(dispatchers, block = block)
    }


    companion object {
        fun syncToLocal(
            authority: String,
            account: Account,
        ) {
            ContentResolver.requestSync(
                account,
                authority,
                bundleOf(
                    "local_is_source_of_truth" to true
                )
            )
        }

        fun syncToRemote(
            authority: String,
            account: Account,
        ) {
            ContentResolver.requestSync(
                account,
                authority,
                bundleOf(
                    "server_is_source_of_truth" to true
                )
            )
        }

        fun sync(
            authority: String,
            account: Account,
        ) {
            ContentResolver.requestSync(
                account,
                authority,
                bundleOf()
            )
        }
    }
}