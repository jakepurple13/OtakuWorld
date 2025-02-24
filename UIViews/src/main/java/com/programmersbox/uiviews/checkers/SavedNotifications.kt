package com.programmersbox.uiviews.checkers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.util.fastMap
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.helpfulutils.NotificationDslBuilder
import com.programmersbox.helpfulutils.SemanticActions
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.receivers.DeleteNotificationReceiver
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.logFirebaseMessage
import com.programmersbox.uiviews.utils.recordFirebaseException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL


object SavedNotifications {

    fun viewNotificationFromDb(
        context: Context,
        n: NotificationItem,
        notificationLogo: NotificationLogo,
        info: GenericInfo,
        sourceRepository: SourceRepository,
    ) {
        val icon = notificationLogo.notificationId
        val update = UpdateNotification(context)
        (n.id to NotificationDslBuilder.builder(
            context,
            "otakuChannel",
            icon
        ) {
            title = n.notiTitle
            subText = n.source
            getBitmapFromURL(n.imageUrl)?.let {
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
            groupId = "otakuGroup"
            addAction {
                actionTitle = context.getString(R.string.mark_read)
                actionIcon = notificationLogo.notificationId
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) semanticAction = SemanticActions.MARK_AS_READ
                pendingActionIntent {
                    val intent = Intent(context, DeleteNotificationReceiver::class.java)
                    intent.action = "NOTIFICATION_DELETED_ACTION"
                    intent.putExtra("url", n.url)
                    intent.putExtra("id", n.id)
                    PendingIntent.getBroadcast(context, n.id, intent, PendingIntent.FLAG_IMMUTABLE)
                }
            }
            /*deleteIntent { context ->
                val intent1 = Intent(context, DeleteNotificationReceiver::class.java)
                intent1.action = "NOTIFICATION_DELETED_ACTION"
                intent1.putExtra("url", n.url)
                PendingIntent.getBroadcast(context, n.id, intent1, 0)
            }*/
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
            .let { update.onEnd(listOf(it), info = info) }
    }

    fun viewNotificationsFromDb(
        context: Context,
        logo: NotificationLogo,
        info: GenericInfo,
        sourceRepository: SourceRepository,
    ) {
        val dao by lazy { ItemDatabase.getInstance(context).itemDao() }
        val icon = logo.notificationId
        val update = UpdateNotification(context)
        GlobalScope.launch {
            dao.getAllNotifications()
                .fastMap { n ->
                    println(n)
                    n.id to NotificationDslBuilder.builder(
                        context,
                        "otakuChannel",
                        icon
                    ) {
                        title = n.notiTitle
                        subText = n.source
                        getBitmapFromURL(n.imageUrl)?.let {
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
                        groupId = "otakuGroup"
                        addAction {
                            actionTitle = context.getString(R.string.mark_read)
                            actionIcon = logo.notificationId
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) semanticAction = SemanticActions.MARK_AS_READ
                            pendingActionIntent {
                                val intent = Intent(context, DeleteNotificationReceiver::class.java)
                                intent.action = "NOTIFICATION_DELETED_ACTION"
                                intent.putExtra("url", n.url)
                                intent.putExtra("id", n.id)
                                PendingIntent.getBroadcast(context, n.id, intent, PendingIntent.FLAG_IMMUTABLE)
                            }
                        }
                        /*deleteIntent { context ->
                            val intent1 = Intent(context, DeleteNotificationReceiver::class.java)
                            intent1.action = "NOTIFICATION_DELETED_ACTION"
                            intent1.putExtra("url", n.url)
                            PendingIntent.getBroadcast(context, n.id, intent1, 0)
                        }*/
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
                .let { update.onEnd(it, info = info) }
        }
    }
}

fun getBitmapFromURL(strURL: String?, headers: Map<String, Any> = emptyMap()): Bitmap? = runCatching {
    val url = URL(strURL)
    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
    headers.forEach { connection.setRequestProperty(it.key, it.value.toString()) }
    connection.doInput = true
    connection.connect()
    BitmapFactory.decodeStream(connection.inputStream)
}
    .onFailure {
        logFirebaseMessage("Getting bitmap from $strURL")
        recordFirebaseException(it)
        it.printStackTrace()
    }
    .getOrNull()