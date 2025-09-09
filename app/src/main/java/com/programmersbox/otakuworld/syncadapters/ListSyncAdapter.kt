package com.programmersbox.otakuworld.syncadapters

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle

class ListSyncAdapter(
    context: Context,
) : AbstractThreadedSyncAdapter(context, true, false) {
    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?,
    ) {
        println(account)
        println(authority)
        //TODO: Might need to make multiple of these. Also have to figure out the names for the sync items
        runCatching {
            val keySet = extras?.keySet()
            if (keySet?.isNotEmpty() == true) {
                keySet.forEach {
                    runCatching {
                        println(it + " | " + extras.get(it))
                    }
                }
            }
        }
    }
}