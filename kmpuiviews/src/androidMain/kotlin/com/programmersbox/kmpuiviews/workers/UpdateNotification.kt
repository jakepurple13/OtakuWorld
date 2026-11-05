package com.programmersbox.kmpuiviews.workers

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.util.fastForEach
import androidx.core.content.getSystemService
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.domain.MediaUpdateChecker
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckProgress
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.domain.UpdateModel
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdatedItem
import com.programmersbox.kmpuiviews.domain.updatecheck.chapterRange
import com.programmersbox.kmpuiviews.domain.updatecheck.inboxLines
import com.programmersbox.kmpuiviews.domain.updatecheck.updateTitle
import com.programmersbox.kmpuiviews.receivers.DownloadNewChaptersReceiver
import com.programmersbox.kmpuiviews.domain.updatecheck.finishedText
import com.programmersbox.kmpuiviews.domain.updatecheck.finishedTitle
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.receivers.DeleteNotificationReceiver
import com.programmersbox.kmpuiviews.receivers.SwipeAwayReceiver
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.DeepLinks
import com.programmersbox.kmpuiviews.utils.GroupBehavior
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationGroups
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.SemanticActions
import io.ktor.client.HttpClient
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.checking
import otakuworld.kmpuiviews.generated.resources.finishedChecking
import otakuworld.kmpuiviews.generated.resources.uc_download_action
import otakuworld.kmpuiviews.generated.resources.mark_read
import otakuworld.kmpuiviews.generated.resources.updateAmount

class UpdateNotification(
    private val context: Context,
    private val mediaUpdateChecker: MediaUpdateChecker,
    private val icon: NotificationLogo,
    private val appConfig: AppConfig,
    private val info: PlatformGenericInfo,
    private val deepLinks: DeepLinks,
) {
    private val client = HttpClient()

    private val notificationManager by lazy { context.getSystemService<NotificationManager>() }

    suspend fun updateManga(dao: ItemDao, triple: List<Pair<KmpInfoModel?, DbModel>>) {
        triple.fastForEach {
            val item = it.second
            item.numChapters = it.first?.chapters?.size ?: item.numChapters
            dao.insertFavorite(item)
        }
    }

    private suspend fun getTitle(updateModel: UpdateModel) = updateTitle(
        updateModel.item.dbModel.title,
        updateModel.item.newChapters.size,
        info.updateUnit,
    )

    suspend fun mapDbModel(list: List<UpdatedItem>): List<Pair<Int, Notification>> {
        return mediaUpdateChecker.mapDbModel(
            list = list,
            // No progress here: the mapping phase is short, and a plain notify on the foreground id outlives the worker.
            notificationUpdate = { _, _, _ -> },
            unit = info.updateUnit,
        )
            .map { updateModel ->
                val notificationId = updateModel.notificationId
                val pair: Pair<KmpInfoModel?, DbModel> = updateModel.item.info to updateModel.item.dbModel
                val newChapters = updateModel.item.newChapters
                val notificationTitle = getTitle(updateModel)
                notificationId to NotificationDslBuilder.builder(
                    context,
                    NotificationChannels.Otaku.id,
                    icon.notificationId
                ) {
                    title = notificationTitle
                    message = chapterRange(newChapters)
                    subText = pair.first?.source?.displayName ?: pair.second.source
                    client.getBitmapFromURL(pair.second.imageUrl, pair.first?.extras.orEmpty())?.let {
                        largeIconBitmap = it
                    }
                    inboxStyle {
                        contentTitle = notificationTitle
                        inboxLines(newChapters).forEach { addLine(it) }
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
                    if (info.supportsBatchDownload && newChapters.isNotEmpty()) addAction {
                        actionTitle = getPluralString(Res.plurals.uc_download_action, newChapters.size, newChapters.size)
                        actionIcon = icon.notificationId
                        pendingActionIntent {
                            val intent = Intent(context, DownloadNewChaptersReceiver::class.java)
                            intent.action = "DOWNLOAD_NEW_CHAPTERS_ACTION"
                            intent.putExtra("url", pair.second.url)
                            intent.putExtra("source", pair.second.source)
                            intent.putExtra("count", newChapters.size)
                            intent.putExtra("id", notificationId)
                            PendingIntent.getBroadcast(context, notificationId + 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                        }
                    }
                    deleteIntent { context ->
                        val intent = Intent(context, SwipeAwayReceiver::class.java)
                        intent.action = "NOTIFICATION_DELETED_ACTION"
                        intent.putExtra("url", pair.second.url)
                        intent.putExtra("id", notificationId)
                        // Per-notification request code: with a shared one, every swipe would carry the first title's url.
                        PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    }
                    pendingIntent { context ->
                        info.deepLinkDetails(context, pair.second.toItemModel(pair.first!!.source))
                    }
                }
            }
    }

    /** Update notifications already showing that [list] doesn't replace. Skips the summary and non-update notifications. */
    private fun otherUpdateNotificationCount(list: List<Pair<Int, Notification>>): Int = notificationManager
        ?.activeNotifications
        ?.count {
            it.notification.group == NotificationGroups.Otaku.id &&
                it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
                list.none { l -> l.first == it.id }
        }
        ?: 0

    suspend fun onEnd(
        list: List<Pair<Int, Notification>>,
        notificationId: Int = 42,
    ) {
        val currentNotificationSize = otherUpdateNotificationCount(list)

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
        val currentNotificationSize = otherUpdateNotificationCount(list)

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

    suspend fun sendRunningNotificationLongTerm(
        max: Int,
        progress: Int,
        contextText: CharSequence = "",
        block: NotificationDslBuilder.() -> Unit = {},
    ): Notification {
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

            block()
        }
        logFirebaseMessage("Checking for $contextText")
        return notification
    }

    suspend fun buildProgressNotification(progress: UpdateCheckProgress, cancelIntent: PendingIntent): Notification =
        buildCheckProgressNotification(context, icon, progress, cancelIntent)

    /** Posts nothing when [report] is null (nothing ran). */
    suspend fun sendFinishedNotification(report: UpdateCheckReport?) {
        if (report == null) return
        val title = report.finishedTitle()
        val text = report.finishedText()
        val notification = NotificationDslBuilder.builder(
            context,
            NotificationChannels.UpdateCheck.id,
            icon.notificationId
        ) {
            onlyAlertOnce = true
            this.title = title
            message = text
            subText = getString(Res.string.finishedChecking)
            if (report.hasProblems) {
                autoCancel = true
                pendingIntent { context -> info.deepLinkUpdateCheckErrors(context) }
            } else {
                timeoutAfter = 4000L
            }
        }
        notificationManager?.notify(UPDATE_CHECK_FINISHED_ID, notification)
    }

    fun cancelProgressNotification() {
        notificationManager?.cancel(UPDATE_CHECK_PROGRESS_ID)
    }

    /** A new check replaces the last report, so its finished notification is stale. */
    fun cancelFinishedNotification() {
        notificationManager?.cancel(UPDATE_CHECK_FINISHED_ID)
    }
}
