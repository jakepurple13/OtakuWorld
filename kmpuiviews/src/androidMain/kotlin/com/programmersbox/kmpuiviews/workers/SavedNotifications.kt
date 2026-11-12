package com.programmersbox.kmpuiviews.workers

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.receivers.DeleteNotificationReceiver
import com.programmersbox.kmpuiviews.receivers.SwipeAwayReceiver
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationGroups
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.SemanticActions
import com.programmersbox.kmpuiviews.utils.printLogs
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.mark_read

object SavedNotifications {

    // One client for every restore; a new client per call was never closed.
    private val client by lazy { HttpClient() }

    // Keeps a boot restore well inside the broadcast deadline even when image hosts are slow.
    private const val BITMAP_TIMEOUT_MS = 5_000L

    suspend fun viewNotificationFromDb(
        context: Context,
        n: NotificationItem,
        notificationLogo: NotificationLogo,
        info: PlatformGenericInfo,
        sourceRepository: SourceRepository,
        itemDao: ItemDao,
        update: UpdateNotification,
    ) {
        itemDao.updateNotification(
            url = n.url,
            isShowing = true
        )
        (n.id to buildNotification(context, n, notificationLogo, info, sourceRepository))
            .let { update.onEnd(listOf(it)) }
    }

    suspend fun viewNotificationsFromDb(
        context: Context,
        logo: NotificationLogo,
        info: PlatformGenericInfo,
        sourceRepository: SourceRepository,
        dao: ItemDao,
        update: UpdateNotification,
    ) {
        coroutineScope {
            dao.getAllNotifications()
                .filter { it.isShowing }
                .map { n ->
                    async {
                        printLogs { n }
                        dao.updateNotification(
                            url = n.url,
                            isShowing = true
                        )
                        n.id to buildNotification(context, n, logo, info, sourceRepository)
                    }
                }
                .awaitAll()
        }
            .let { update.onEnd(it) }
    }

    private suspend fun buildNotification(
        context: Context,
        n: NotificationItem,
        logo: NotificationLogo,
        info: PlatformGenericInfo,
        sourceRepository: SourceRepository,
    ): Notification {
        val bitmap = withTimeoutOrNull(BITMAP_TIMEOUT_MS) { client.getBitmapFromURL(n.imageUrl) }
        val markRead = getString(Res.string.mark_read)
        return NotificationDslBuilder.builder(
            context,
            NotificationChannels.Otaku.id,
            logo.notificationId
        ) {
            title = n.notiTitle
            subText = sourceRepository.displayNameFor(n.source)
            bitmap?.let {
                largeIconBitmap = it
                pictureStyle {
                    bigPicture = it
                    largeIcon = it
                    contentTitle = n.contentTitle
                    summaryText = n.summaryText
                }
            } ?: bigTextStyle {
                contentTitle = n.contentTitle
                bigText = n.summaryText
            }
            showWhen = true
            groupId = NotificationGroups.Otaku.id
            addAction {
                actionTitle = markRead
                actionIcon = logo.notificationId
                semanticAction = SemanticActions.MARK_AS_READ
                pendingActionIntent {
                    val intent = Intent(context, DeleteNotificationReceiver::class.java)
                    intent.action = "NOTIFICATION_DELETED_ACTION"
                    intent.putExtra("url", n.url)
                    intent.putExtra("id", n.id)
                    PendingIntent.getBroadcast(context, n.id, intent, PendingIntent.FLAG_IMMUTABLE)
                }
            }
            deleteIntent { context ->
                val intent1 = Intent(context, SwipeAwayReceiver::class.java)
                intent1.action = "NOTIFICATION_DELETED_ACTION"
                intent1.putExtra("url", n.url)
                PendingIntent.getBroadcast(context, n.id, intent1, PendingIntent.FLAG_IMMUTABLE)
            }
            // Built from the stored row: sources may not be loaded yet (e.g. at boot), and a network lookup here
            // would block the receiver.
            pendingIntent { context ->
                info.deepLink(
                    context,
                    info.deepLinkDetailsUri(url = n.url, imageUrl = n.imageUrl.orEmpty(), source = n.source)
                )
            }
        }
    }
}
