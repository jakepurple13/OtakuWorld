package com.programmersbox.uiviews.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.uiviews.utils.recordFirebaseException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DeleteNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val dao by lazy { context?.let { ItemDatabase.getInstance(it).itemDao() } }
        val url = intent?.getStringExtra("url")
        val id = intent?.getIntExtra("id", -1)
        println(url)
        GlobalScope.launch {
            runCatching {
                url?.let { dao?.getNotificationItem(it) }
                    .also { println(it) }
                    ?.let { dao?.deleteNotification(it) }
            }.onFailure { recordFirebaseException(it) }
            id?.let { if (it != -1) context?.notificationManager?.cancel(it) }
            val g = context?.notificationManager?.activeNotifications?.map { it.notification }?.filter { it.group == "otakuGroup" }.orEmpty()
            if (g.size == 1) context?.notificationManager?.cancel(42)
        }
    }
}