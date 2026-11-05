package com.programmersbox.kmpuiviews.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.utils.printLogs
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeleteNotificationReceiver : BroadcastReceiver(), KoinComponent {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        val dao: ItemDao by inject()
        val url = intent?.getStringExtra("url")
        val id = intent?.getIntExtra("id", -1)
        printLogs { url }
        GlobalScope.launch {
            clearUpdateNotification(context, dao, url, id)
        }
    }
}
