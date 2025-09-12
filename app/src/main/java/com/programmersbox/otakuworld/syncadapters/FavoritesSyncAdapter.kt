package com.programmersbox.otakuworld.syncadapters

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import androidx.core.os.bundleOf
import com.programmersbox.otakuworld.DataStoreHandling
import com.programmersbox.otakuworld.OtakuFavoritesContentProviderHelper
import com.programmersbox.otakuworld.repository.ServerHandler
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

        println("Syncing favorites for $app")

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
        val remoteFavorites = runCatching { runBlocking { serverHandler.getFavorites(app) } }
            .onSuccess { println("[FavoritesSyncAdapter] Remote favorites: ${it.favorites.size}") }
            .onFailure {
                it.printStackTrace()
                // If remote fetch fails, bail out gracefully
                syncResult.databaseError = true
            }
            .getOrThrow()

        // Index by URL for quick diff
        val localByUrl = localFavorites.associateBy { it.url }
        val remoteByUrl = remoteFavorites.favorites.associateBy { it.url }

        // 3) Pull: Add remote-only items to local provider
        val toInsertLocally = remoteByUrl.keys.minus(localByUrl.keys).mapNotNull { remoteByUrl[it] }
        println("[FavoritesSyncAdapter] To insert locally: ${toInsertLocally.size}")
        toInsertLocally.forEach { model ->
            runCatching { helper.insertFavorite(context, model) }
                .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
        }

        // 4) Push: Add local-only items to remote (server upsert)
        val toPushRemotely = localByUrl.keys.minus(remoteByUrl.keys).mapNotNull { localByUrl[it] }
        println("[FavoritesSyncAdapter] To push remotely: ${toPushRemotely.size}")
        runBlocking {
            toPushRemotely
                .map { model ->
                    async(dispatchers, start = CoroutineStart.LAZY) {
                        runCatching { serverHandler.upsertFavorite(app, model) }
                            .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
                    }
                }.awaitAll()
        }

        // 5) Resolve conflicts: If present in both but different, prefer server and update local
        val intersecting = localByUrl.keys.intersect(remoteByUrl.keys)
        println("[FavoritesSyncAdapter] Intersecting: ${intersecting.size}")
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

        // 6) Deletes strategy using FavoritesData.lastTimeUpdated and DataStoreHandling.lastTimeFavoritesSynced
        // We use remote's lastTimeUpdated to infer who has the newer source of truth:
        // - If flags provided in extras, they override and force direction.
        // - If no flags:
        //   - If either timestamp is 0, skip destructive deletes to avoid data loss on first syncs.
        //   - If remoteUpdated > lastSyncTime -> server newer -> delete local-only items.
        //   - If remoteUpdated < lastSyncTime -> local newer -> delete remote-only items.
        //   - If equal and > 0 -> perform symmetric deletes to converge.
        val lastSyncTime = runCatching { runBlocking { dataStoreHandling.lastTimeFavoritesSynced.get() } }
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
            if (remoteUpdated == 0L || lastSyncTime == 0L) {
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

        if (doDeleteLocalMissingOnServer) {
            val toDeleteLocally = localByUrl.keys.minus(remoteByUrl.keys)
            println("[FavoritesSyncAdapter] Deleting locally (missing on server): ${toDeleteLocally.size}")
            toDeleteLocally.forEach { url ->
                runCatching { helper.deleteFavorite(context, url) }
                    .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
            }
        }

        if (doDeleteRemoteMissingOnLocal) {
            val toDeleteRemotely = remoteByUrl.keys.minus(localByUrl.keys).mapNotNull { remoteByUrl[it] }
            println("[FavoritesSyncAdapter] Deleting remotely (missing locally): ${toDeleteRemotely.size}")
            runBlocking {
                toDeleteRemotely
                    .map { model ->
                        async(dispatchers, start = CoroutineStart.LAZY) {
                            runCatching { serverHandler.deleteFavorite(app, model) }
                                .onFailure { syncResult.stats?.numSkippedEntries = (syncResult.stats?.numSkippedEntries ?: 0) + 1 }
                        }
                    }.awaitAll()
            }
        }

        // Update last successful sync time
        runCatching { runBlocking { dataStoreHandling.lastTimeFavoritesSynced.set(System.currentTimeMillis()) } }
            .onFailure { it.printStackTrace() }

        // Optional logging of extras for debugging
        runCatching {
            val keySet = extras?.keySet()
            if (keySet?.isNotEmpty() == true) {
                keySet.forEach { key ->
                    runCatching { println("[FavoritesSyncAdapter] Extra: " + key + " = " + extras.get(key)) }
                }
            }
        }

        println("[FavoritesSyncAdapter] Sync complete")
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