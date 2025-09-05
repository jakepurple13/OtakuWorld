package com.programmersbox.otakuworld.syncadapters

import android.accounts.Account
import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.os.Bundle
import android.os.IBinder

class FavoritesSyncAdapter(
    context: Context,
) : AbstractThreadedSyncAdapter(context, true, false) {
    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?,
    ) {
        //TODO: Might need to make multiple of these. Also have to figure out the names for the sync items
    }
}

class FavoritesSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        synchronized(syncAdapterLock) {
            syncAdapter = syncAdapter ?: FavoritesSyncAdapter(applicationContext)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return syncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        private var syncAdapter: FavoritesSyncAdapter? = null
        private val syncAdapterLock = Any()
    }
}