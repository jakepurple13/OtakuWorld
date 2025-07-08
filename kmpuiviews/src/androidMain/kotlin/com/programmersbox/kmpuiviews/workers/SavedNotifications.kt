package com.programmersbox.kmpuiviews.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.util.fastMap
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.mark_read

object SavedNotifications {

    suspend fun viewNotificationFromDb(
        context: Context,
        n: NotificationItem,
        notificationLogo: NotificationLogo,
        info: PlatformGenericInfo,
        sourceRepository: SourceRepository,
        itemDao: ItemDao,
        update: UpdateNotification,
    ) {
        val client = HttpClient()
        val icon = notificationLogo.notificationId
        itemDao.updateNotification(
            url = n.url,
            isShowing = true
        )
        (n.id to NotificationDslBuilder.builder(
            context,
            NotificationChannels.Otaku.id,
            icon
        ) {
            title = n.notiTitle
            subText = n.source
            client.getBitmapFromURL(n.imageUrl)?.let {
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
                actionTitle = getString(Res.string.mark_read)
                actionIcon = notificationLogo.notificationId
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
            pendingIntent { context ->
                runBlocking {
                    val itemModel = sourceRepository.toSourceByApiServiceName(n.source)
                        ?.apiService
                        ?.getSourceByUrlFlow(n.url)
                        ?.firstOrNull()

                    /*val d = Screen.DetailsScreen.Details(
                        title = itemModel?.title.orEmpty(),
                        imageUrl = itemModel?.imageUrl.orEmpty(),
                        source = itemModel?.source?.serviceName.orEmpty(),
                        url = itemModel?.url.orEmpty(),
                        description = itemModel?.description.orEmpty(),
                    )

                    val c = d.generateRouteWithArgs(
                        mapOf<String, NavType<Any?>>(
                            "title" to NavType.StringType,
                            "imageUrl" to NavType.StringType,
                            "source" to NavType.StringType,
                            "url" to NavType.StringType,
                            "description" to NavType.StringType
                        )
                    )

                    NavDeepLinkBuilder(context)
                        .addDestination(c)
                        .createPendingIntent()*/

                    info.deepLinkDetails(context, itemModel)
                }
            }
        })
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
        val client = HttpClient()
        val icon = logo.notificationId
        dao.getAllNotifications()
            .fastMap { n ->
                printLogs { n }
                dao.updateNotification(
                    url = n.url,
                    isShowing = true
                )
                n.id to NotificationDslBuilder.builder(
                    context,
                    NotificationChannels.Otaku.id,
                    icon
                ) {
                    title = n.notiTitle
                    subText = n.source
                    client.getBitmapFromURL(n.imageUrl)?.let {
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
                        actionTitle = getString(Res.string.mark_read)
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
                    pendingIntent { context ->
                        runBlocking {
                            val itemModel = sourceRepository.toSourceByApiServiceName(n.source)//UpdateWorker.sourceFromString(n.source)
                                ?.apiService
                                ?.getSourceByUrlFlow(n.url)
                                ?.firstOrNull()

                            info.deepLinkDetails(context, itemModel)
                        }
                    }
                }
            }
            .let { update.onEnd(it) }
    }
}