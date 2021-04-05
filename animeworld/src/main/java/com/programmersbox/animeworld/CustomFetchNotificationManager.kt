package com.programmersbox.animeworld

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.DownloadNotification.ActionType.*
import com.tonyodev.fetch2.util.DEFAULT_NOTIFICATION_TIMEOUT_AFTER
import com.tonyodev.fetch2.util.DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET
import com.tonyodev.fetch2.util.onDownloadNotificationActionTriggered
import java.text.DecimalFormat

/**
 * The default notification manager class for Fetch. This manager supports both single
 * download notifications and grouped download notifications. Extend this class to provide your own
 * custom implementation.
 *
 * Note: An instance of this class should only be associated with one Fetch namespace.
 * It is best to provide each fetch instance with a new instance of this class.
 *
 * */
class CustomFetchNotificationManager(context: Context) : FetchNotificationManager {

    companion object {
        private const val packageName = "com.programmersbox.animeworld."
    }

    private val context: Context = context.applicationContext
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val downloadNotificationsMap = mutableMapOf<Int, DownloadNotification>()
    private val downloadNotificationsBuilderMap = mutableMapOf<Int, NotificationCompat.Builder>()
    private val downloadNotificationExcludeSet = mutableSetOf<Int>()

    override val notificationManagerAction: String = "DEFAULT_FETCH2_NOTIFICATION_MANAGER_ACTION_" + System.currentTimeMillis()

    override val broadcastReceiver: BroadcastReceiver
        get() = object : BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                onDownloadNotificationActionTriggered(context, intent, this@CustomFetchNotificationManager)
            }

        }

    init {
        initialize()
    }

    private fun initialize() {
        registerBroadcastReceiver()
        createNotificationChannels(context, notificationManager)
    }

    override fun registerBroadcastReceiver() {
        context.registerReceiver(broadcastReceiver, IntentFilter(notificationManagerAction))
    }

    override fun unregisterBroadcastReceiver() {
        context.unregisterReceiver(broadcastReceiver)
    }

    override fun createNotificationChannels(context: Context, notificationManager: NotificationManager) {
        val channelId = context.getString(R.string.fetch_notification_default_channel_id)
        var channel: NotificationChannel? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(channelId)
        } else {
            null
        }
        if (channel == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelName = context.getString(R.string.fetch_notification_default_channel_name)
            channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun getChannelId(notificationId: Int, context: Context): String {
        return context.getString(R.string.fetch_notification_default_channel_id)
    }

    override fun updateGroupSummaryNotification(
        groupId: Int,
        notificationBuilder: NotificationCompat.Builder,
        downloadNotifications: List<DownloadNotification>,
        context: Context
    ): Boolean {
        val style = NotificationCompat.InboxStyle()
        for (downloadNotification in downloadNotifications) {
            val contentTitle = getSubtitleText(context, downloadNotification)
            style.addLine("${downloadNotification.total} $contentTitle")
        }
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.fetch_notification_default_channel_name))
            .setContentText("")
            .setStyle(style)
            .setGroup(groupId.toString())
            .setGroupSummary(true)
        return false
    }

    /*override fun updateNotification(notificationBuilder: NotificationCompat.Builder,
                                    downloadNotification: DownloadNotification,
                                    context: Context) {
        val smallIcon = if (downloadNotification.isDownloading) {
            android.R.drawable.stat_sys_download
        } else {
            android.R.drawable.stat_sys_download_done
        }
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(smallIcon)
                .setContentTitle(downloadNotification.title)
                .setContentText(getSubtitleText(context, downloadNotification))
                .setOngoing(downloadNotification.isOnGoingNotification)
                .setGroup(downloadNotification.groupId.toString())
                .setGroupSummary(false)
        if (downloadNotification.isFailed || downloadNotification.isCompleted) {
            notificationBuilder.setProgress(0, 0, false)
        } else {
            val progressIndeterminate = downloadNotification.progressIndeterminate
            val maxProgress = if (downloadNotification.progressIndeterminate) 0 else 100
            val progress = if (downloadNotification.progress < 0) 0 else downloadNotification.progress
            notificationBuilder.setProgress(maxProgress, progress, progressIndeterminate)
        }
        when {
            downloadNotification.isDownloading -> {
                notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())
                        .addAction(R.drawable.fetch_notification_pause,
                                context.getString(R.string.fetch_notification_download_pause),
                                getActionPendingIntent(downloadNotification, PAUSE))
                        .addAction(R.drawable.fetch_notification_cancel,
                                context.getString(R.string.fetch_notification_download_cancel),
                                getActionPendingIntent(downloadNotification, CANCEL))
            }
            downloadNotification.isPaused -> {
                notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())
                        .addAction(R.drawable.fetch_notification_resume,
                                context.getString(R.string.fetch_notification_download_resume),
                                getActionPendingIntent(downloadNotification, RESUME))
                        .addAction(R.drawable.fetch_notification_cancel,
                                context.getString(R.string.fetch_notification_download_cancel),
                                getActionPendingIntent(downloadNotification, CANCEL))
            }
            downloadNotification.isQueued -> {
                notificationBuilder.setTimeoutAfter(getNotificationTimeOutMillis())
            }
            else -> {
                notificationBuilder.setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET)
            }
        }
    }*/

    override fun updateNotification(
        notificationBuilder: NotificationCompat.Builder,
        downloadNotification: DownloadNotification,
        context: Context
    ) {
        val smallIcon = if (downloadNotification.isDownloading) {
            android.R.drawable.stat_sys_download
        } else {
            android.R.drawable.stat_sys_download_done
        }

        // Creates an explicit intent for an Activity in your app
        val resultIntent = Intent(context, MainActivity::class.java)
        //resultIntent.putExtra(ConstantValues.DOWNLOAD_NOTIFICATION, false)
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your app to the Home screen.
        val stackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity::class.java)
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent = stackBuilder.getPendingIntent(
            0,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        fun getPauseOrResumeAction(downloadNotification: DownloadNotification): NotificationCompat.Action? {
            var text = ""
            var icon = 0
            val pauseIntent: Intent = when {
                downloadNotification.isDownloading -> {
                    text = "Pause"
                    icon = android.R.drawable.ic_media_pause
                    Intent(context, PauseReceiver::class.java).apply {
                        action = "${packageName}PAUSE_DOWNLOAD"
                        putExtra(ConstantValues.NOTIFICATION_ID, downloadNotification.notificationId)
                    }
                }
                downloadNotification.isPaused -> {
                    text = "Resume"
                    icon = android.R.drawable.ic_media_play
                    Intent(context, ResumeReceiver::class.java).apply {
                        action = "${packageName}RESUME_DOWNLOAD"
                        putExtra(ConstantValues.NOTIFICATION_ID, downloadNotification.notificationId)
                    }
                }
                downloadNotification.isFailed -> {
                    text = "Retry"
                    icon = android.R.drawable.ic_delete
                    Intent(context, RetryReceiver::class.java).apply {
                        action = "${packageName}RETRY"
                        putExtra(ConstantValues.NOTIFICATION_ID, downloadNotification.notificationId)
                    }
                }
                else -> {
                    null
                }
            } ?: return null
            val pendingPauseIntent = PendingIntent.getBroadcast(context, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            return NotificationCompat.Action(icon, text, pendingPauseIntent)
        }

        val currentProgress = "%.2f".format(getProgress(downloadNotification.downloaded, downloadNotification.total))
        val speed = getDownloadSpeedString(downloadNotification.downloadedBytesPerSecond)
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(smallIcon)
            .setContentTitle(downloadNotification.title)
            .setContentText(getSubtitleText(context, downloadNotification))
            .setOngoing(downloadNotification.isOnGoingNotification)
            .setContentIntent(resultPendingIntent)
            .setSubText("$speed at $currentProgress%")
            .setGroup(downloadNotification.groupId.toString())
            .setGroupSummary(false)
            .setOnlyAlertOnce(true)
        if (downloadNotification.isPaused)
            notificationBuilder.setTimeoutAfter(5000)
        if (downloadNotification.isFailed) {
            notificationBuilder.setProgress(0, 0, false)
        } else {
            val progressIndeterminate = downloadNotification.progressIndeterminate
            val maxProgress = if (downloadNotification.progressIndeterminate) 0 else 100
            val progress = if (downloadNotification.progress < 0) 0 else downloadNotification.progress
            notificationBuilder.setProgress(maxProgress, progress, progressIndeterminate)
        }
        val actions = getPauseOrResumeAction(downloadNotification)
        if (actions != null)
            notificationBuilder.addAction(actions)
        when {
            downloadNotification.isDownloading -> {
                notificationBuilder.addAction(
                    R.drawable.fetch_notification_pause,
                    context.getString(R.string.fetch_notification_download_pause),
                    getActionPendingIntent(downloadNotification, PAUSE)
                )
                    .addAction(
                        R.drawable.fetch_notification_cancel,
                        context.getString(R.string.fetch_notification_download_cancel),
                        getActionPendingIntent(downloadNotification, CANCEL)
                    )
            }
            downloadNotification.isPaused -> {
                notificationBuilder.addAction(
                    R.drawable.fetch_notification_resume,
                    context.getString(R.string.fetch_notification_download_resume),
                    getActionPendingIntent(downloadNotification, RESUME)
                )
                    .addAction(
                        R.drawable.fetch_notification_cancel,
                        context.getString(R.string.fetch_notification_download_cancel),
                        getActionPendingIntent(downloadNotification, CANCEL)
                    )
            }
        }

        val cancelIntent = Intent(context, CancelReceiver::class.java).apply {
            action = "${packageName}CANCEL_DOWNLOAD"
            putExtra(ConstantValues.NOTIFICATION_ID, downloadNotification.notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(context, 1, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationBuilder.addAction(android.R.drawable.ic_delete, "Cancel", pendingIntent)
    }

    override fun getActionPendingIntent(
        downloadNotification: DownloadNotification,
        actionType: DownloadNotification.ActionType
    ): PendingIntent {
        synchronized(downloadNotificationsMap) {
            val intent = Intent(notificationManagerAction)
            intent.putExtra(EXTRA_NAMESPACE, downloadNotification.namespace)
            intent.putExtra(EXTRA_DOWNLOAD_ID, downloadNotification.notificationId)
            intent.putExtra(EXTRA_NOTIFICATION_ID, downloadNotification.notificationId)
            intent.putExtra(EXTRA_GROUP_ACTION, false)
            intent.putExtra(EXTRA_NOTIFICATION_GROUP_ID, downloadNotification.groupId)
            val action = when (actionType) {
                CANCEL -> ACTION_TYPE_CANCEL
                DELETE -> ACTION_TYPE_DELETE
                RESUME -> ACTION_TYPE_RESUME
                PAUSE -> ACTION_TYPE_PAUSE
                RETRY -> ACTION_TYPE_RETRY
                else -> ACTION_TYPE_INVALID
            }
            intent.putExtra(EXTRA_ACTION_TYPE, action)
            return PendingIntent.getBroadcast(context, downloadNotification.notificationId + action, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun getGroupActionPendingIntent(
        groupId: Int,
        downloadNotifications: List<DownloadNotification>,
        actionType: DownloadNotification.ActionType
    ): PendingIntent {
        synchronized(downloadNotificationsMap) {
            val intent = Intent(notificationManagerAction)
            intent.putExtra(EXTRA_NOTIFICATION_GROUP_ID, groupId)
            intent.putExtra(EXTRA_DOWNLOAD_NOTIFICATIONS, ArrayList(downloadNotifications))
            intent.putExtra(EXTRA_GROUP_ACTION, true)
            val action = when (actionType) {
                CANCEL_ALL -> ACTION_TYPE_CANCEL_ALL
                DELETE_ALL -> ACTION_TYPE_DELETE_ALL
                RESUME_ALL -> ACTION_TYPE_RESUME_ALL
                PAUSE_ALL -> ACTION_TYPE_PAUSE_ALL
                RETRY_ALL -> ACTION_TYPE_RETRY_ALL
                else -> ACTION_TYPE_INVALID
            }
            intent.putExtra(EXTRA_ACTION_TYPE, action)
            return PendingIntent.getBroadcast(context, groupId + action, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun cancelNotification(notificationId: Int) {
        synchronized(downloadNotificationsMap) {
            notificationManager.cancel(notificationId)
            downloadNotificationsBuilderMap.remove(notificationId)
            downloadNotificationExcludeSet.remove(notificationId)
            val downloadNotification = downloadNotificationsMap[notificationId]
            if (downloadNotification != null) {
                downloadNotificationsMap.remove(notificationId)
                notify(downloadNotification.groupId)
            }
        }
    }

    override fun cancelOngoingNotifications() {
        synchronized(downloadNotificationsMap) {
            val iterator = downloadNotificationsMap.values.iterator()
            var downloadNotification: DownloadNotification
            while (iterator.hasNext()) {
                downloadNotification = iterator.next()
                if (!downloadNotification.isFailed && !downloadNotification.isCompleted) {
                    notificationManager.cancel(downloadNotification.notificationId)
                    downloadNotificationsBuilderMap.remove(downloadNotification.notificationId)
                    downloadNotificationExcludeSet.remove(downloadNotification.notificationId)
                    iterator.remove()
                    notify(downloadNotification.groupId)
                }
            }
        }
    }

    override fun notify(groupId: Int) {
        synchronized(downloadNotificationsMap) {
            val groupedDownloadNotifications = downloadNotificationsMap.values.filter { it.groupId == groupId }
            val groupSummaryNotificationBuilder = getNotificationBuilder(groupId, groupId)
            val useGroupNotification = updateGroupSummaryNotification(groupId, groupSummaryNotificationBuilder, groupedDownloadNotifications, context)
            var notificationId: Int
            var notificationBuilder: NotificationCompat.Builder
            for (downloadNotification in groupedDownloadNotifications) {
                if (shouldUpdateNotification(downloadNotification)) {
                    notificationId = downloadNotification.notificationId
                    notificationBuilder = getNotificationBuilder(notificationId, groupId)
                    updateNotification(notificationBuilder, downloadNotification, context)
                    notificationManager.notify(notificationId, notificationBuilder.build())
                    when (downloadNotification.status) {
                        Status.COMPLETED,
                        Status.FAILED -> {
                            downloadNotificationExcludeSet.add(downloadNotification.notificationId)
                        }
                        else -> {
                        }
                    }
                }
            }
            if (useGroupNotification) {
                notificationManager.notify(groupId, groupSummaryNotificationBuilder.build())
            }
        }
    }

    override fun shouldUpdateNotification(downloadNotification: DownloadNotification): Boolean {
        return !downloadNotificationExcludeSet.contains(downloadNotification.notificationId)
    }

    override fun shouldCancelNotification(downloadNotification: DownloadNotification): Boolean {
        return downloadNotification.isPaused
    }

    override fun postDownloadUpdate(download: Download): Boolean {
        return synchronized(downloadNotificationsMap) {
            if (downloadNotificationsMap.size > 50) {
                downloadNotificationsBuilderMap.clear()
                downloadNotificationsMap.clear()
            }
            val downloadNotification = downloadNotificationsMap[download.id] ?: DownloadNotification()
            downloadNotification.status = download.status
            downloadNotification.progress = download.progress
            downloadNotification.notificationId = download.id
            downloadNotification.groupId = download.group
            downloadNotification.etaInMilliSeconds = download.etaInMilliSeconds
            downloadNotification.downloadedBytesPerSecond = download.downloadedBytesPerSecond
            downloadNotification.total = download.total
            downloadNotification.downloaded = download.downloaded
            downloadNotification.namespace = download.namespace
            downloadNotification.title = getDownloadNotificationTitle(download)
            downloadNotificationsMap[download.id] = downloadNotification
            if (downloadNotificationExcludeSet.contains(downloadNotification.notificationId)
                && !downloadNotification.isFailed && !downloadNotification.isCompleted
            ) {
                downloadNotificationExcludeSet.remove(downloadNotification.notificationId)
            }
            if (downloadNotification.isCancelledNotification || shouldCancelNotification(downloadNotification)) {
                cancelNotification(downloadNotification.notificationId)
            } else {
                notify(download.group)
            }
            true
        }
    }

    @SuppressLint("RestrictedApi")
    override fun getNotificationBuilder(notificationId: Int, groupId: Int): NotificationCompat.Builder {
        synchronized(downloadNotificationsMap) {
            val notificationBuilder = downloadNotificationsBuilderMap[notificationId]
                ?: NotificationCompat.Builder(context, getChannelId(notificationId, context))
            downloadNotificationsBuilderMap[notificationId] = notificationBuilder
            notificationBuilder
                .setGroup(notificationId.toString())
                .setStyle(null)
                .setProgress(0, 0, false)
                .setContentTitle(null)
                .setContentText(null)
                .setContentIntent(null)
                .setGroupSummary(false)
                .setTimeoutAfter(DEFAULT_NOTIFICATION_TIMEOUT_AFTER_RESET)
                .setOngoing(false)
                .setGroup(groupId.toString())
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .mActions.clear()
            return notificationBuilder
        }
    }

    override fun getNotificationTimeOutMillis(): Long {
        return DEFAULT_NOTIFICATION_TIMEOUT_AFTER
    }

    override fun getFetchInstanceForNamespace(namespace: String): Fetch {
        return Fetch.getDefaultInstance()
    }

    override fun getDownloadNotificationTitle(download: Download): String {
        return download.fileUri.lastPathSegment ?: Uri.parse(download.url).lastPathSegment ?: download.url
    }

    override fun getSubtitleText(context: Context, downloadNotification: DownloadNotification): String {
        return when {
            downloadNotification.isCompleted -> context.getString(R.string.fetch_notification_download_complete)
            downloadNotification.isFailed -> context.getString(R.string.fetch_notification_download_failed)
            downloadNotification.isPaused -> context.getString(R.string.fetch_notification_download_paused)
            downloadNotification.isQueued -> context.getString(R.string.fetch_notification_download_starting)
            downloadNotification.etaInMilliSeconds < 0 -> context.getString(R.string.fetch_notification_download_downloading)
            else -> getEtaText(context, downloadNotification.etaInMilliSeconds)
        }
    }

    private fun getEtaText(context: Context, etaInMilliSeconds: Long): String {
        var seconds = (etaInMilliSeconds / 1000)
        val hours = (seconds / 3600)
        seconds -= (hours * 3600)
        val minutes = (seconds / 60)
        seconds -= (minutes * 60)
        return when {
            hours > 0 -> context.getString(R.string.fetch_notification_download_eta_hrs, hours, minutes, seconds)
            minutes > 0 -> context.getString(R.string.fetch_notification_download_eta_min, minutes, seconds)
            else -> context.getString(R.string.fetch_notification_download_eta_sec, seconds)
        }
    }

    private fun getProgress(downloaded: Long, total: Long): Double {
        return when {
            total < 1 -> -1.0
            downloaded < 1 -> 0.0
            downloaded >= total -> 100.0
            else -> (downloaded.toDouble() / total.toDouble() * 100)
        }
    }

    private fun getDownloadSpeedString(downloadedBytesPerSecond: Long): String {
        if (downloadedBytesPerSecond < 0) {
            return ""
        }
        val kb = downloadedBytesPerSecond.toDouble() / 1000.toDouble()
        val mb = kb / 1000.toDouble()
        val gb = mb / 1000
        val tb = gb / 1000
        val decimalFormat = DecimalFormat(".##")
        return when {
            tb >= 1 -> "${decimalFormat.format(tb)} tb/s"
            gb >= 1 -> "${decimalFormat.format(gb)} gb/s"
            mb >= 1 -> "${decimalFormat.format(mb)} mb/s"
            kb >= 1 -> "${decimalFormat.format(kb)} kb/s"
            else -> "$downloadedBytesPerSecond b/s"
        }
    }

}

object ConstantValues {
    const val NOTIFICATION_ID = "notification_id"
}

class PauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.getIntExtra(ConstantValues.NOTIFICATION_ID, 0)?.let { Fetch.getDefaultInstance().pause(it) }
    }
}

class ResumeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.getIntExtra(ConstantValues.NOTIFICATION_ID, 0)?.let { Fetch.getDefaultInstance().resume(it) }
    }
}

class RetryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.getIntExtra(ConstantValues.NOTIFICATION_ID, 0)?.let { Fetch.getDefaultInstance().retry(it) }
    }
}

class CancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.getIntExtra(ConstantValues.NOTIFICATION_ID, 0)?.let { Fetch.getDefaultInstance().cancel(it).deleteAllWithStatus(Status.CANCELLED) }
    }
}