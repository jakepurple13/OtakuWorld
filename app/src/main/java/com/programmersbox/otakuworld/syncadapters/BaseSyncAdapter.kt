package com.programmersbox.otakuworld.syncadapters

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import androidx.annotation.CallSuper
import com.programmersbox.otakuworld.App

abstract class BaseSyncAdapter(
    context: Context,
    autoInitialize: Boolean,
    allowParallelSyncs: Boolean,
) : AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {
    fun getApp(authority: String): App? = when {
        authority.contains("manga") -> App.MangaWorld
        authority.contains("anime") -> App.AnimeWorld
        authority.contains("novel") -> App.NovelWorld
        else -> null
    }

    @CallSuper
    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?,
    ) {
        println("Performing sync for $authority")
    }
}