package com.programmersbox.uiviews.checkers

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.helpfulutils.GroupBehavior
import com.programmersbox.helpfulutils.NotificationDslBuilder
import com.programmersbox.helpfulutils.SemanticActions
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.domain.MediaUpdateChecker
import com.programmersbox.kmpuiviews.receivers.DeleteNotificationReceiver
import com.programmersbox.kmpuiviews.receivers.SwipeAwayReceiver
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.NotificationChannels
import com.programmersbox.uiviews.utils.NotificationGroups
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.logFirebaseMessage
import com.programmersbox.uiviews.utils.recordFirebaseException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

class UpdateNotification(
    private val context: Context,
    private val mediaUpdateChecker: MediaUpdateChecker,
    private val icon: NotificationLogo,
) {

    suspend fun updateManga(dao: ItemDao, triple: List<Pair<KmpInfoModel?, DbModel>>) {
        triple.fastForEach {
            val item = it.second
            item.numChapters = it.first?.chapters?.size ?: item.numChapters
            dao.insertFavorite(item)
            FirebaseDb.updateShowFlow(item).catch {
                recordFirebaseException(it)
                println("Something went wrong: ${it.message}")
            }.collect()
        }
    }

    suspend fun mapDbModel(list: List<Pair<KmpInfoModel?, DbModel>>, info: GenericInfo) =
        mediaUpdateChecker.mapDbModel(
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
                            summaryText = context.getString(
                                R.string.hadAnUpdate,
                                pair.second.title,
                                pair.first?.chapters?.firstOrNull()?.name ?: ""
                            )
                        }
                    } ?: bigTextStyle {
                        contentTitle = pair.first?.chapters?.firstOrNull()?.name ?: ""
                        bigText = context.getString(
                            R.string.hadAnUpdate,
                            pair.second.title,
                            pair.first?.chapters?.firstOrNull()?.name.orEmpty()
                        )
                    }
                    showWhen = true
                    groupId = NotificationGroups.Otaku.id
                    addAction {
                        actionTitle = context.getString(R.string.mark_read)
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

    fun onEnd(
        list: List<Pair<Int, Notification>>,
        notificationId: Int = 42,
        info: GenericInfo,
    ) {
        val n = context.notificationManager
        val currentNotificationSize = n.activeNotifications.filterNot { list.fastAny { l -> l.first == it.id } }.size - 1
        list.fastForEach { pair -> n.notify(pair.first, pair.second) }
        if (list.isNotEmpty()) n.notify(
            notificationId,
            NotificationDslBuilder.builder(
                context,
                NotificationChannels.Otaku.id,
                icon.notificationId
            ) {
                title = context.getText(R.string.app_name)
                val size = list.size + currentNotificationSize
                subText = context.resources.getQuantityString(R.plurals.updateAmount, size, size)
                showWhen = true
                groupSummary = true
                groupAlertBehavior = GroupBehavior.ALL
                groupId = NotificationGroups.Otaku.id
                pendingIntent { context -> info.deepLinkSettings(context) }
            }
        )
    }

    suspend fun onEnd(
        list: List<Pair<Int, Notification>>,
        notificationId: Int = 42,
        info: GenericInfo,
        onNotify: suspend (Int, Notification) -> Unit,
    ) {
        val n = context.notificationManager
        val currentNotificationSize = n.activeNotifications.filterNot { list.fastAny { l -> l.first == it.id } }.size - 1
        list.fastForEach { pair -> n.notify(pair.first, pair.second) }
        if (list.isNotEmpty()) onNotify(
            notificationId,
            NotificationDslBuilder.builder(
                context,
                NotificationChannels.Otaku.id,
                icon.notificationId
            ) {
                title = context.getText(R.string.app_name)
                val size = list.size + currentNotificationSize
                subText = context.resources.getQuantityString(R.plurals.updateAmount, size, size)
                showWhen = true
                groupSummary = true
                groupAlertBehavior = GroupBehavior.ALL
                groupId = NotificationGroups.Otaku.id
                pendingIntent { context -> info.deepLinkSettings(context) }
            }
        )
    }

    fun sendRunningNotification(max: Int, progress: Int, contextText: CharSequence = "") {
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
            subText = context.getString(R.string.checking)
        }
        context.notificationManager.notify(13, notification)
        logFirebaseMessage("Checking for $contextText")
    }

    fun sendFinishedNotification() {
        val notification = NotificationDslBuilder.builder(
            context,
            NotificationChannels.UpdateCheck.id,
            icon.notificationId
        ) {
            onlyAlertOnce = true
            subText = context.getString(R.string.finishedChecking)
            timeoutAfter = 750L
        }
        context.notificationManager.notify(13, notification)
    }
}