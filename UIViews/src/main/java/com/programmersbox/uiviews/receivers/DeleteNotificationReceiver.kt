package com.programmersbox.uiviews.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.uiviews.utils.recordFirebaseException
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
        println(url)
        GlobalScope.launch {
            runCatching {
                url?.let { dao.getNotificationItem(it) }
                    .also { println(it) }
                    ?.let { dao.deleteNotification(it) }
            }.onFailure { recordFirebaseException(it) }
            id?.let { if (it != -1) context?.notificationManager?.cancel(it) }
            val g = context?.notificationManager?.activeNotifications?.map { it.notification }?.filter { it.group == "otakuGroup" }.orEmpty()
            if (g.size == 1) context?.notificationManager?.cancel(42)
        }
    }
}