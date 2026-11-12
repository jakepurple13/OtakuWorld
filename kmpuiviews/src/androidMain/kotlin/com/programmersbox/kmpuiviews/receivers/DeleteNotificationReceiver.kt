package com.programmersbox.kmpuiviews.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.utils.printLogs
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeleteNotificationReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context?, intent: Intent?) {
        val dao: ItemDao by inject()
        val url = intent?.getStringExtra("url")
        val id = intent?.getIntExtra("id", -1)
        printLogs { url }
        goAsync {
            clearUpdateNotification(context, dao, url, id)
        }
    }
}
