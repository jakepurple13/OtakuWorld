package com.programmersbox.kmpuiviews.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.updatecheck.newChapters
import com.programmersbox.kmpuiviews.recordFirebaseException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Downloads the chapters a per-title update notification announced, then clears it like Mark read. */
class DownloadNewChaptersReceiver : BroadcastReceiver(), KoinComponent {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        val dao: ItemDao by inject()
        val sourceRepository: SourceRepository by inject()
        val genericInfo: KmpGenericInfo by inject()
        val notificationManager = context?.getSystemService<NotificationManager>()
        val url = intent?.getStringExtra("url")
        val source = intent?.getStringExtra("source")
        val count = intent?.getIntExtra("count", 0) ?: 0
        val id = intent?.getIntExtra("id", -1) ?: -1
        val pending = goAsync()
        GlobalScope.launch {
            try {
                runCatching {
                    val favorite = url?.let { dao.getDbModelSync(it) } ?: return@runCatching
                    val api = source?.let { sourceRepository.toSourceByApiServiceName(it) }?.apiService
                        ?: return@runCatching
                    val info = favorite.toItemModel(api).toInfoModel().firstOrNull()?.getOrNull()
                        ?: return@runCatching
                    val chapters = newChapters(info, info.chapters.size - count)
                    if (chapters.isNotEmpty()) genericInfo.batchDownloadChapters(chapters, info)
                    dao.getNotificationItem(favorite.url)?.let { dao.deleteNotification(it) }
                    if (id != -1) notificationManager?.cancel(id)
                    val g = notificationManager
                        ?.activeNotifications
                        ?.map { it.notification }
                        ?.filter { it.group == "otakuGroup" }
                        .orEmpty()
                    if (g.size == 1) notificationManager?.cancel(42)
                }.onFailure { recordFirebaseException(it) }
            } finally {
                pending.finish()
            }
        }
    }
}
