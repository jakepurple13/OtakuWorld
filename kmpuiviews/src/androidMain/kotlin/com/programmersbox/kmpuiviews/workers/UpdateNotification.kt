package com.programmersbox.kmpuiviews.workers

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.core.content.getSystemService
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.domain.MediaUpdateChecker
import com.programmersbox.kmpuiviews.receivers.DeleteNotificationReceiver
import com.programmersbox.kmpuiviews.receivers.SwipeAwayReceiver
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationGroups
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.GroupBehavior
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.SemanticActions
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.checking
import otakuworld.kmpuiviews.generated.resources.finishedChecking
import otakuworld.kmpuiviews.generated.resources.hadAnUpdate
import otakuworld.kmpuiviews.generated.resources.mark_read
import otakuworld.kmpuiviews.generated.resources.updateAmount

class UpdateNotification(
    private val context: Context,
    private val mediaUpdateChecker: MediaUpdateChecker,
    private val icon: NotificationLogo,
    private val appConfig: AppConfig,
    private val kmpFirebaseConnection: KmpFirebaseConnection,
    private val info: PlatformGenericInfo,
) {

    private val notificationManager by lazy { context.getSystemService<NotificationManager>() }

    suspend fun updateManga(dao: ItemDao, triple: List<Pair<KmpInfoModel?, DbModel>>) {
        triple.fastForEach {
            val item = it.second
            item.numChapters = it.first?.chapters?.size ?: item.numChapters
            dao.insertFavorite(item)
            kmpFirebaseConnection
                .updateShowFlow(item)
                .catch { error ->
                    logFirebaseMessage("Something went wrong: ${error.message}")
                    recordFirebaseException(error)
                }
                .collect()
        }
    }

    suspend fun mapDbModel(list: List<Pair<KmpInfoModel?, DbModel>>): List<Pair<Int, Notification>> {
        return mediaUpdateChecker.mapDbModel(
            list = list,
            notificationUpdate = { max, progress, source -> sendRunningNotification(max, progress, source) }
        )
            .map { updateModel ->
                val notificationId = updateModel.notificationId
                val pair = updateModel.infoModel to updateModel.dbModel
                notificationId to NotificationDslBuilder.builder(
                    context,
                    NotificationChannels.Otaku.id,
                    icon.notificationId
                ) {
                    title = pair.second.title
                    subText = pair.second.source
                    getBitmapFromURL(pair.second.imageUrl, pair.first?.extras.orEmpty())?.let {
                        largeIconBitmap = it
                        pictureStyle {
                            bigPicture = it
                            largeIcon = it
                            contentTitle = pair.first?.chapters?.firstOrNull()?.name ?: ""
                            summaryText = getString(
                                Res.string.hadAnUpdate,
                                pair.second.title,
                                pair.first?.chapters?.firstOrNull()?.name ?: ""
                            )
                        }
                    } ?: bigTextStyle {
                        contentTitle = pair.first?.chapters?.firstOrNull()?.name ?: ""
                        bigText = getString(
                            Res.string.hadAnUpdate,
                            pair.second.title,
                            pair.first?.chapters?.firstOrNull()?.name.orEmpty()
                        )
                    }
                    showWhen = true
                    groupId = NotificationGroups.Otaku.id
                    addAction {
                        actionTitle = getString(Res.string.mark_read)
                        actionIcon = icon.notificationId
                        semanticAction = SemanticActions.MARK_AS_READ
                        pendingActionIntent {
                            val intent = Intent(context, DeleteNotificationReceiver::class.java)
                            intent.action = "NOTIFICATION_DELETED_ACTION"
                            intent.putExtra("url", pair.second.url)
                            intent.putExtra("id", notificationId)
                            PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)
                        }
                    }
                    deleteIntent { context ->
                        val intent = Intent(context, SwipeAwayReceiver::class.java)
                        intent.action = "NOTIFICATION_DELETED_ACTION"
                        intent.putExtra("url", pair.second.url)
                        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                    }
                    pendingIntent { context ->
                        info.deepLinkDetails(context, pair.second.toItemModel(pair.first!!.source))
                    }
                }
            }
    }

    suspend fun onEnd(
        list: List<Pair<Int, Notification>>,
        notificationId: Int = 42,
    ) {
        val currentNotificationSize = notificationManager
            ?.activeNotifications
            ?.filterNot { list.fastAny { l -> l.first == it.id } }
            .orEmpty()
            .size - 1

        list.fastForEach { pair -> notificationManager?.notify(pair.first, pair.second) }
        if (list.isNotEmpty()) {
            notificationManager?.notify(
                notificationId,
                NotificationDslBuilder.builder(
                    context,
                    NotificationChannels.Otaku.id,
                    icon.notificationId
                ) {
                    title = appConfig.appName
                    val size = list.size + currentNotificationSize
                    subText = getPluralString(Res.plurals.updateAmount, size, size)
                    showWhen = true
                    groupSummary = true
                    groupAlertBehavior = GroupBehavior.ALL
                    groupId = NotificationGroups.Otaku.id
                    pendingIntent { context -> info.deepLinkSettings(context) }
                }
            )
        }
    }

    suspend fun onEnd(
        list: List<Pair<Int, Notification>>,
        notificationId: Int = 42,
        info: PlatformGenericInfo,
        onNotify: suspend (Int, Notification) -> Unit,
    ) {
        val currentNotificationSize = notificationManager
            ?.activeNotifications
            ?.filterNot { list.fastAny { l -> l.first == it.id } }
            .orEmpty()
            .size - 1

        list.fastForEach { pair -> notificationManager?.notify(pair.first, pair.second) }
        if (list.isNotEmpty()) {
            onNotify(
                notificationId,
                NotificationDslBuilder.builder(
                    context,
                    NotificationChannels.Otaku.id,
                    icon.notificationId
                ) {
                    title = appConfig.appName
                    val size = list.size + currentNotificationSize
                    subText = getPluralString(Res.plurals.updateAmount, size, size)
                    showWhen = true
                    groupSummary = true
                    groupAlertBehavior = GroupBehavior.ALL
                    groupId = NotificationGroups.Otaku.id
                    pendingIntent { context -> info.deepLinkSettings(context) }
                }
            )
        }
    }

    suspend fun sendRunningNotification(max: Int, progress: Int, contextText: CharSequence = "") {
        val notification = NotificationDslBuilder.builder(
            context,
            NotificationChannels.UpdateCheck.id,
            icon.notificationId
        ) {
            onlyAlertOnce = true
            ongoing = true
            progress {
                this.max = max
                this.progress = progress
                indeterminate = progress == 0
            }
            showWhen = true
            message = contextText
            subText = getString(Res.string.checking)
        }
        notificationManager?.notify(13, notification)
        logFirebaseMessage("Checking for $contextText")
    }

    suspend fun sendFinishedNotification() {
        val notification = NotificationDslBuilder.builder(
            context,
            NotificationChannels.UpdateCheck.id,
            icon.notificationId
        ) {
            onlyAlertOnce = true
            subText = getString(Res.string.finishedChecking)
            timeoutAfter = 750L
        }
        notificationManager?.notify(13, notification)
    }
}