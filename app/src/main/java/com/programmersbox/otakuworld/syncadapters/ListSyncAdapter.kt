package com.programmersbox.otakuworld.syncadapters

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.programmersbox.otakuworld.CustomList
import com.programmersbox.otakuworld.CustomListInfo
import com.programmersbox.otakuworld.MultiprocessDataStoreHandler
import com.programmersbox.otakuworld.providers.App
import com.programmersbox.otakuworld.providers.OtakuCustomListContentProviderHelper
import com.programmersbox.otakuworld.repository.ServerHandler
import kotlinx.coroutines.runBlocking

class ListSyncAdapter(
    context: Context,
    private val serverHandler: ServerHandler,
    private val multiprocessDataStoreHandler: MultiprocessDataStoreHandler,
) : BaseSyncAdapter(context, true, false) {

    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?,
    ) {
        super.onPerformSync(account, extras, authority, provider, syncResult)
        if (authority.isNullOrBlank()) return

        val app = getApp(authority) ?: return

        val helper = OtakuCustomListContentProviderHelper(authority)

        runCatching {
            runBlocking {
                try {
                    // 1) Load local and remote data
                    val localLists = helper.getAllCustomLists(context) ?: emptyList()
                    val remoteLists = runCatching { serverHandler.getLists(app) }
                        .getOrNull() ?: return@runBlocking

                    // Index by uuid for quick lookups
                    val localById = localLists.associateBy { it.item.uuid }
                    val remoteById = remoteLists.associateBy { it.item.uuid }

                    // 2) Push local lists missing on server or newer than server
                    for ((uuid, local) in localById) {
                        val remote = remoteById[uuid]
                        if (remote == null) {
                            // Not on server → push local
                            runCatching { serverHandler.upsertList(app, local) }
                                .onFailure { syncResult?.stats?.numIoExceptions = syncResult.stats?.numIoExceptions?.plus(1) ?: 1 }
                        } else {
                            // Conflict resolution by updated time
                            if (local.item.time > remote.item.time) {
                                // Local is newer → push to server
                                runCatching { serverHandler.upsertList(app, local) }
                                    .onFailure { syncResult?.stats?.numIoExceptions = syncResult.stats?.numIoExceptions?.plus(1) ?: 1 }
                            }
                        }
                    }

                    // 3) Pull remote lists missing locally or newer than local
                    for ((uuid, remote) in remoteById) {
                        val local = localById[uuid]
                        if (local == null) {
                            // Not locally → create the full list and entries locally
                            upsertLocalList(helper, remote)
                            syncResult?.stats?.numInserts = syncResult.stats?.numInserts?.plus(1) ?: 1
                        } else {
                            if (remote.item.time > local.item.time) {
                                // Remote newer → replace locally
                                upsertLocalList(helper, remote)
                                syncResult?.stats?.numUpdates = syncResult.stats?.numUpdates?.plus(1) ?: 1
                            } else if (remote.item.time == local.item.time) {
                                // Same header time, ensure entries consistency (handle possible item-level drifts)
                                syncEntries(helper, uuid, local, remote)
                            }
                        }
                    }

                } catch (t: Throwable) {
                    t.printStackTrace()
                    // Count as a soft error so sync framework can retry if needed
                    syncResult?.stats?.numIoExceptions = syncResult.stats?.numIoExceptions?.plus(1) ?: 1
                }
            }
        }

        runCatching {
            runBlockingIo {
                multiprocessDataStoreHandler.updateData {
                    when (app) {
                        App.MangaWorld -> it.copy(lastListsSyncManga = System.currentTimeMillis())
                        App.AnimeWorld -> it.copy(lastListsSyncAnime = System.currentTimeMillis())
                        App.NovelWorld -> it.copy(lastListsSyncNovel = System.currentTimeMillis())
                    }
                }
            }
        }.onFailure { it.printStackTrace() }

        println("[ListSyncAdapter] Sync complete")
    }

    private fun upsertLocalList(helper: OtakuCustomListContentProviderHelper, remote: CustomList) {
        val ctx = context
        // Ensure list header exists/updated
        val insertedOrUpdated = if (helper.getListByUuid(ctx, remote.item.uuid)?.use { it.count > 0 } == true) {
            helper.updateList(ctx, remote.item)
        } else {
            helper.insertList(ctx, remote.item)
            1
        }
        // Sync entries: bring local entries to match remote exactly
        syncEntries(helper, remote.item.uuid, null, remote)
    }

    private fun syncEntries(
        helper: OtakuCustomListContentProviderHelper,
        uuid: String,
        local: CustomList?,
        remote: CustomList,
    ) {
        val ctx = context
        val localEntries = local?.list ?: helper.getItemsForList(ctx, uuid)?.let { helper.cursorToCustomListInfos(it) } ?: emptyList()
        val remoteEntries = remote.list

        val localById = localEntries.associateBy { it.uniqueId }
        val remoteById = remoteEntries.associateBy { it.uniqueId }

        // Insert or update entries from remote
        for ((id, r) in remoteById) {
            val l = localById[id]
            if (l == null) {
                helper.insertListItem(ctx, r)
            } else if (!entriesEqual(l, r)) {
                helper.updateListItem(ctx, r)
            }
        }

        // Delete entries that exist locally but not on remote
        for ((id, _) in localById) {
            if (!remoteById.containsKey(id)) {
                helper.deleteListItem(ctx, id)
            }
        }
    }

    private fun entriesEqual(a: CustomListInfo, b: CustomListInfo): Boolean {
        return a.uniqueId == b.uniqueId &&
                a.uuid == b.uuid &&
                a.title == b.title &&
                a.description == b.description &&
                a.url == b.url &&
                a.imageUrl == b.imageUrl &&
                a.source == b.source
    }
}