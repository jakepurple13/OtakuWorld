package com.programmersbox.kmpuiviews.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.recordFirebaseException
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
        val notificationManager by lazy {
            context?.getSystemService<NotificationManager>()
        }
        val url = intent?.getStringExtra("url")
        val id = intent?.getIntExtra("id", -1)
        printLogs { url }
        GlobalScope.launch {
            runCatching {
                url?.let { dao.getNotificationItem(it) }
                    .also { printLogs { it } }
                    ?.let { dao.deleteNotification(it) }
            }.onFailure { recordFirebaseException(it) }
            id?.let { if (it != -1) notificationManager?.cancel(it) }
            val g = notificationManager
                ?.activeNotifications
                ?.map { it.notification }
                ?.filter { it.group == "otakuGroup" }
                .orEmpty()
            if (g.size == 1) notificationManager?.cancel(42)
        }
    }
}