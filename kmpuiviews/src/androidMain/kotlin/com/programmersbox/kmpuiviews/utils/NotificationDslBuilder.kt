package com.programmersbox.kmpuiviews.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import kotlin.properties.Delegates

class NotificationException(message: String?) : Exception(message)

@SuppressLint("InlinedApi")
enum class NotificationChannelImportance(@RequiresApi(Build.VERSION_CODES.N) internal val importanceSdk: Int, internal val importance: Int) {
    /**
     * A notification with no importance: does not show in the shade.
     */
    NONE(NotificationManager.IMPORTANCE_NONE, NotificationManagerCompat.IMPORTANCE_NONE),

    /**
     * Min notification importance: only shows in the shade, below the fold.  This should
     * not be used with {@link Service#startForeground(int, Notification) Service.startForeground}
     * since a foreground service is supposed to be something the user cares about so it does
     * not make semantic sense to mark its notification as minimum importance.  If you do this
     * as of Android version {@link android.os.Build.VERSION_CODES#O}, the system will show
     * a higher-priority notification about your app running in the background.
     */
    MIN(NotificationManager.IMPORTANCE_MIN, NotificationManagerCompat.IMPORTANCE_MIN),

    /**
     * Low notification importance: Shows in the shade, and potentially in the status bar
     * (see {@link #shouldHideSilentStatusBarIcons()}), but is not audibly intrusive.
     */
    LOW(NotificationManager.IMPORTANCE_LOW, NotificationManagerCompat.IMPORTANCE_LOW),

    /**
     * Default notification importance: shows everywhere, makes noise, but does not visually
     * intrude.
     */
    DEFAULT(NotificationManager.IMPORTANCE_DEFAULT, NotificationManagerCompat.IMPORTANCE_DEFAULT),

    /**
     * Higher notification importance: shows everywhere, makes noise and peeks. May use full screen
     * intents.
     */
    HIGH(NotificationManager.IMPORTANCE_HIGH, NotificationManagerCompat.IMPORTANCE_HIGH),

    /**
     * Unused.
     */
    MAX(NotificationManager.IMPORTANCE_MAX, NotificationManagerCompat.IMPORTANCE_MAX)
}

/**
 * Creates a [NotificationChannel]
 */
@JvmOverloads
@RequiresApi(Build.VERSION_CODES.O)
fun Context.createNotificationChannel(
    id: String,
    name: CharSequence = id,
    importance: NotificationChannelImportance = NotificationChannelImportance.DEFAULT,
    block: NotificationChannel.() -> Unit = {},
) = getSystemService<NotificationManager>()?.createNotificationChannel(NotificationChannel(id, name, importance.importance).apply(block))

/**
 * Creates a [NotificationChannelGroup]
 */
@JvmOverloads
@RequiresApi(Build.VERSION_CODES.O)
fun Context.createNotificationGroup(id: String, name: CharSequence = id, block: NotificationChannelGroup.() -> Unit = {}) =
    getSystemService<NotificationManager>()?.createNotificationChannelGroup(NotificationChannelGroup(id, name).apply(block))

/**
 * sendNotification - sends a notification
 * @param smallIconId the icon id for the notification
 * @param title the title
 * @param message the message
 * @param channelId the channel id
 * @param gotoActivity the activity that will launch when notification is pressed
 * @param notificationId the id of the notification
 */
@JvmOverloads
fun Context.sendNotification(
    @DrawableRes smallIconId: Int,
    title: String?,
    message: String?,
    notificationId: Int,
    channelId: String,
    groupId: String = channelId,
    gotoActivity: Class<*>? = null,
) {
    // mNotificationId is a unique integer your app uses to identify the
    // notification. For example, to cancel the notification, you can pass its ID
    // number to NotificationManager.cancel().
    getSystemService<NotificationManager>()?.notify(
        notificationId,
        NotificationDslBuilder.builder(this, channelId, smallIconId) {
            this.title = title
            this.message = message
            this.groupId = groupId
            pendingIntent(gotoActivity)
        }
    )
}

fun Context.sendNotification(notificationId: Int, channelId: String, @DrawableRes smallIconId: Int, block: NotificationDslBuilder.() -> Unit) =
    getSystemService<NotificationManager>()?.notify(notificationId, NotificationDslBuilder.builder(this, channelId, smallIconId, block))

fun NotificationManager.notify(
    context: Context,
    notificationId: Int,
    channelId: String,
    @DrawableRes smallIconId: Int,
    block: NotificationDslBuilder.() -> Unit,
) = notify(notificationId, NotificationDslBuilder.builder(context, channelId, smallIconId, block))

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun NotificationManagerCompat.notify(
    context: Context,
    notificationId: Int,
    channelId: String,
    @DrawableRes smallIconId: Int,
    block: NotificationDslBuilder.() -> Unit,
) = notify(notificationId, NotificationDslBuilder.builder(context, channelId, smallIconId, block))

@DslMarker
private annotation class NotificationUtilsMarker

@DslMarker
private annotation class NotificationActionMarker

@DslMarker
private annotation class NotificationStyleMarker

@DslMarker
private annotation class NotificationBubbleMarker

@DslMarker
private annotation class NotificationProgressMarker

@DslMarker
private annotation class NotificationPersonBuilder

@DslMarker
private annotation class RemoteMarker

class NotificationDslBuilder(
    val context: Context,
    /**
     * @see Notification.Builder.setChannelId
     * @see NotificationCompat.Builder.setChannelId
     */
    @NotificationUtilsMarker
    var channelId: String,
    /**
     * @see Notification.Builder.setSmallIcon
     * @see NotificationCompat.Builder.setSmallIcon
     */
    @DrawableRes
    @NotificationUtilsMarker
    var smallIconId: Int,
) {

    /**
     * @see Notification.Builder.setGroup
     * @see NotificationCompat.Builder.setGroup
     */
    @NotificationUtilsMarker
    var groupId: String = ""

    /**
     * @see Notification.Builder.setContentTitle
     * @see NotificationCompat.Builder.setContentTitle
     */
    @NotificationUtilsMarker
    var title: CharSequence? = null

    /**
     * @see Notification.Builder.setContentText
     * @see NotificationCompat.Builder.setContentText
     */
    @NotificationUtilsMarker
    var message: CharSequence? = null

    /**
     * @see Notification.Builder.setLargeIcon
     * @see NotificationCompat.Builder.setLargeIcon
     */
    @NotificationUtilsMarker
    var largeIconBitmap: Bitmap? = null

    /**
     * @see Notification.Builder.setLargeIcon
     * @see NotificationCompat.Builder.setLargeIcon
     */
    @NotificationUtilsMarker
    var largeIconIcon: Icon? = null

    private var privatePendingIntent: PendingIntent? = null

    @NotificationUtilsMarker
    fun pendingIntent(gotoActivity: Class<*>?, requestCode: Int = 0, block: TaskStackBuilder.() -> Unit = {}) {
        privatePendingIntent = gotoActivity?.let {
            TaskStackBuilder.create(context)
                .addParentStack(gotoActivity)
                .addNextIntent(Intent(context, it))
                .apply(block)
                .getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    /**
     * @see Notification.Builder.setContentIntent
     * @see NotificationCompat.Builder.setContentIntent
     */
    @NotificationUtilsMarker
    fun pendingIntent(pendingIntent: PendingIntent?) = run { privatePendingIntent = pendingIntent }

    /**
     * @see Notification.Builder.setContentIntent
     * @see NotificationCompat.Builder.setContentIntent
     */
    @NotificationUtilsMarker
    fun pendingIntent(block: (context: Context) -> PendingIntent?) = run { privatePendingIntent = block(context) }

    private var privateDeleteIntent: PendingIntent? = null

    /**
     * @see Notification.Builder.setDeleteIntent
     * @see NotificationCompat.Builder.setDeleteIntent
     */
    @NotificationUtilsMarker
    fun deleteIntent(pendingIntent: PendingIntent?) = run { privateDeleteIntent = pendingIntent }

    /**
     * @see Notification.Builder.setDeleteIntent
     * @see NotificationCompat.Builder.setDeleteIntent
     */
    @NotificationUtilsMarker
    fun deleteIntent(block: (context: Context) -> PendingIntent?) = run { privateDeleteIntent = block(context) }

    val actions = mutableListOf<NotificationAction>()

    /**
     * @see Notification.Builder.setActions
     * @see NotificationCompat.Builder.addAction
     */
    @NotificationActionMarker
    fun addReplyAction(block: NotificationAction.Reply.() -> Unit) {
        actions.add(NotificationAction.Reply(context).apply(block))
    }

    /**
     * @see Notification.Builder.setActions
     * @see NotificationCompat.Builder.addAction
     */
    @NotificationActionMarker
    inline fun addAction(block: NotificationAction.Action.() -> Unit) {
        actions.add(NotificationAction.Action(context).apply(block))
    }

    /**
     * Creates a Reply Action
     */
    @NotificationActionMarker
    fun replyAction(block: NotificationAction.Reply.() -> Unit) = NotificationAction.Reply(context).apply(block)

    /**
     * Creates a Normal Action
     */
    @NotificationActionMarker
    fun actionAction(block: NotificationAction.Action.() -> Unit) = NotificationAction.Action(context).apply(block)

    operator fun NotificationAction.unaryPlus() = actions.add(this).let { }

    /**
     * @see Notification.Builder.setAutoCancel
     * @see NotificationCompat.Builder.setAutoCancel
     */
    @NotificationUtilsMarker
    var autoCancel: Boolean = false

    /**
     * @see Notification.Builder.setColorized
     * @see NotificationCompat.Builder.setColorized
     */
    @NotificationUtilsMarker
    var colorized: Boolean = false

    /**
     * @see Notification.Builder.setColor
     * @see NotificationCompat.Builder.setColor
     */
    @ColorInt
    @NotificationUtilsMarker
    var color: Int? = null

    /**
     * @see Notification.Builder.setTimeoutAfter
     * @see NotificationCompat.Builder.setTimeoutAfter
     */
    @NotificationUtilsMarker
    var timeoutAfter: Long? = null

    /**
     * @see Notification.Builder.setShowWhen
     * @see NotificationCompat.Builder.setShowWhen
     */
    @NotificationUtilsMarker
    var showWhen: Boolean = false

    /**
     * @see Notification.Builder.setLocalOnly
     * @see NotificationCompat.Builder.setLocalOnly
     */
    @NotificationUtilsMarker
    var localOnly: Boolean = false

    /**
     * @see Notification.Builder.setOngoing
     * @see NotificationCompat.Builder.setOngoing
     */
    @NotificationUtilsMarker
    var ongoing: Boolean = false

    /**
     * @see Notification.Builder.setNumber
     * @see NotificationCompat.Builder.setNumber
     */
    @NotificationUtilsMarker
    var number: Int = 0

    /**
     * @see Notification.Builder.setSubText
     * @see NotificationCompat.Builder.setSubText
     */
    @NotificationUtilsMarker
    var subText: CharSequence = ""

    /**
     * @see Notification.Builder.setOnlyAlertOnce
     * @see NotificationCompat.Builder.setOnlyAlertOnce
     */
    @NotificationUtilsMarker
    var onlyAlertOnce: Boolean = false

    private var extrasSet: Bundle.() -> Unit = {}

    /**
     * Add some extras to the notification
     */
    @NotificationUtilsMarker
    fun extras(block: Bundle.() -> Unit) = run { extrasSet = block }

    var notificationNotificationStyle: NotificationStyle? = null

    /**
     * @see Notification.InboxStyle
     * @see NotificationCompat.InboxStyle
     */
    @NotificationStyleMarker
    fun inboxStyle(block: NotificationStyle.Inbox.() -> Unit) = run { notificationNotificationStyle = NotificationStyle.Inbox().apply(block) }

    /**
     * @see Notification.MessagingStyle
     * @see NotificationCompat.MessagingStyle
     */
    @NotificationStyleMarker
    fun messageStyle(block: NotificationStyle.Messaging.() -> Unit) =
        run { notificationNotificationStyle = NotificationStyle.Messaging(context).apply(block) }

    /**
     * @see Notification.BigPictureStyle
     * @see NotificationCompat.BigPictureStyle
     */
    @NotificationStyleMarker
    inline fun pictureStyle(block: NotificationStyle.Picture.() -> Unit) = run { notificationNotificationStyle = NotificationStyle.Picture().apply(block) }

    /**
     * @see Notification.BigTextStyle
     * @see NotificationCompat.BigTextStyle
     */
    @NotificationStyleMarker
    inline fun bigTextStyle(block: NotificationStyle.BigText.() -> Unit) =
        run { notificationNotificationStyle = NotificationStyle.BigText().apply(block) }

    /**
     * @see Notification.MediaStyle
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @NotificationStyleMarker
    fun mediaStyle(block: NotificationStyle.Media.() -> Unit) = run { notificationNotificationStyle = NotificationStyle.Media().apply(block) }

    /**
     * Add a custom style
     */
    @NotificationStyleMarker
    fun customStyle(block: NotificationStyle?) = run { notificationNotificationStyle = block }

    private var bubble: NotificationBubble? = null

    /**
     * Add a bubble!
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @NotificationBubbleMarker
    fun addBubble(block: NotificationBubble.() -> Unit) = run { bubble = NotificationBubble(context).apply(block) }

    @NotificationUtilsMarker
    private var person: NotificationPerson? = null

    /**
     * Set the person
     *
     * An example:
     * ```kotlin
     * val chatBot = Person.Builder()
     ****      .setBot(true)
     ****      .setName("BubbleBot")
     ****      .setImportant(true)
     ****      .build()
     * ```
     *
     */
    @RequiresApi(Build.VERSION_CODES.P)
    @NotificationUtilsMarker
    fun setPerson(block: NotificationPerson.() -> Unit) = run { person = NotificationPerson().apply(block) }

    /**
     * @see Notification.Builder.setGroupSummary
     * @see NotificationCompat.Builder.setGroupSummary
     */
    @NotificationUtilsMarker
    var groupSummary: Boolean = false

    /**
     * @see Notification.Builder.setGroupAlertBehavior
     * @see NotificationCompat.Builder.setGroupAlertBehavior
     */
    @NotificationUtilsMarker
    var groupAlertBehavior: GroupBehavior = GroupBehavior.ALL

    private var progress: NotificationProgress? = null

    @NotificationProgressMarker
    fun progress(block: NotificationProgress.() -> Unit) = run { progress = NotificationProgress().apply(block) }

    private var remoteViews: NotificationRemoteView? = null

    /**
     * Add some custom views to your notification
     *
     * **Thanks to [Medium](https://itnext.io/android-custom-notification-in-6-mins-c2e7e2ddadab) for a good article to follow**
     */
    @RemoteMarker
    fun remoteViews(block: RemoteViewBuilder.() -> Unit) = run { remoteViews = RemoteViewBuilder().apply(block).build() }

    /**
     * Deprecated on O and above
     * @deprecated use {@link NotificationChannelImportance} instead.
     */
    @Deprecated(message = "Deprecated on O and above. Use NotificationChannelImportance instead")
    @NotificationUtilsMarker
    var priority: NotificationPriority = NotificationPriority.DEFAULT

    @NotificationUtilsMarker
    var category: NotificationCategory? = null

    fun build() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(context, channelId)
            .setSmallIcon(smallIconId)
            .also { builder -> largeIconBitmap?.let { builder.setLargeIcon(it) } ?: largeIconIcon?.let { builder.setLargeIcon(it) } }
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(autoCancel)
            .setColorized(colorized)
            .also { builder -> color?.let { builder.setColor(it) } }
            .also { builder -> timeoutAfter?.let { builder.setTimeoutAfter(it) } }
            .setLocalOnly(localOnly)
            .setOngoing(ongoing)
            .also { builder -> category?.let { builder.setCategory(it.info) } }
            .setNumber(number)
            .setShowWhen(showWhen)
            .setPriority(priority.idSdk)
            .also { builder -> person?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) builder.addPerson(it.buildSdk()) } }
            .also { builder -> bubble?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) builder.setBubbleMetadata(it.build()) } }
            .also { it.extras.extrasSet() }
            .setSubText(subText)
            .setStyle(notificationNotificationStyle?.buildSdk())
            .setOnlyAlertOnce(onlyAlertOnce)
            .setGroup(groupId.let { if (it.isEmpty()) channelId else it })
            .setGroupSummary(groupSummary)
            .setGroupAlertBehavior(groupAlertBehavior.idSdk)
            .setDeleteIntent(privateDeleteIntent)
            .setContentIntent(privatePendingIntent)
            .also { builder -> progress?.let { builder.setProgress(it.max, it.progress, it.indeterminate) } }
            .also { builder ->
                remoteViews?.let { views ->
                    views.headsUp?.let { builder.setCustomHeadsUpContentView(it) }
                    views.collapsed?.let { builder.setCustomContentView(it) }
                    views.expanded?.let { builder.setCustomBigContentView(it) }
                }
            }
            .also { builder -> builder.setActions(*actions.map(NotificationAction::buildSdk).toTypedArray()) }
            .build()
    } else {
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconId)
            .also { builder -> largeIconBitmap?.let { builder.setLargeIcon(it) } }
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(autoCancel)
            .setColorized(colorized)
            .also { builder -> color?.let { builder.color = it } }
            .also { builder -> timeoutAfter?.let { builder.setTimeoutAfter(it) } }
            .setLocalOnly(localOnly)
            .setOngoing(ongoing)
            .also { builder -> category?.let { builder.setCategory(it.info) } }
            .setNumber(number)
            .setShowWhen(showWhen)
            .setPriority(priority.id)
            .also { builder -> person?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) builder.addPerson(it.uri) } }
            .also { builder -> bubble?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) builder.bubbleMetadata = it.buildSdk(context) } }
            .also { it.extras.extrasSet() }
            .setSubText(subText)
            .setStyle(notificationNotificationStyle?.build())
            .setOnlyAlertOnce(onlyAlertOnce)
            .setGroup(groupId.let { if (it.isEmpty()) channelId else it })
            .setGroupSummary(groupSummary)
            .setGroupAlertBehavior(groupAlertBehavior.id)
            .setDeleteIntent(privateDeleteIntent)
            .setContentIntent(privatePendingIntent)
            .also { builder -> progress?.let { builder.setProgress(it.max, it.progress, it.indeterminate) } }
            .also { builder ->
                remoteViews?.let { views ->
                    views.headsUp?.let { builder.setCustomHeadsUpContentView(it) }
                    views.collapsed?.let { builder.setCustomContentView(it) }
                    views.expanded?.let { builder.setCustomBigContentView(it) }
                }
            }
            .also { builder -> actions.forEach { builder.addAction(it.build()) } }
            .build()
    }

    companion object {
        @JvmStatic
        @NotificationUtilsMarker
        inline fun builder(
            context: Context,
            channelId: String,
            @DrawableRes smallIconId: Int,
            block: NotificationDslBuilder.() -> Unit,
        ): Notification =
            NotificationDslBuilder(context, channelId, smallIconId).apply(block).build()
    }

}

enum class NotificationPriority(internal val idSdk: Int, internal val id: Int) {
    /**
     * @see Notification.PRIORITY_MIN
     * @see NotificationCompat.PRIORITY_MIN
     */
    MIN(Notification.PRIORITY_MIN, NotificationCompat.PRIORITY_MIN),

    /**
     * @see Notification.PRIORITY_LOW
     * @see NotificationCompat.PRIORITY_LOW
     */
    LOW(Notification.PRIORITY_LOW, NotificationCompat.PRIORITY_LOW),

    /**
     * @see Notification.PRIORITY_DEFAULT
     * @see NotificationCompat.PRIORITY_DEFAULT
     */
    DEFAULT(Notification.PRIORITY_DEFAULT, NotificationCompat.PRIORITY_DEFAULT),

    /**
     * @see Notification.PRIORITY_HIGH
     * @see NotificationCompat.PRIORITY_HIGH
     */
    HIGH(Notification.PRIORITY_HIGH, NotificationCompat.PRIORITY_HIGH),

    /**
     * @see Notification.PRIORITY_MAX
     * @see NotificationCompat.PRIORITY_MAX
     */
    MAX(Notification.PRIORITY_MAX, NotificationCompat.PRIORITY_MAX)
}

class NotificationProgress internal constructor() {
    /**
     * @see Notification.Builder.setProgress
     * @see Notification.Builder.setProgress
     */
    @NotificationProgressMarker
    var max: Int by Delegates.notNull()

    /**
     * @see Notification.Builder.setProgress
     * @see Notification.Builder.setProgress
     */
    @NotificationProgressMarker
    var progress: Int by Delegates.notNull()

    /**
     * @see Notification.Builder.setProgress
     * @see Notification.Builder.setProgress
     */
    @NotificationProgressMarker
    var indeterminate: Boolean = false
}

@SuppressLint("InlinedApi")
enum class GroupBehavior(@RequiresApi(Build.VERSION_CODES.O) internal val idSdk: Int, internal val id: Int) {
    /**
     * @see Notification.GROUP_ALERT_ALL
     */
    ALL(Notification.GROUP_ALERT_ALL, NotificationCompat.GROUP_ALERT_ALL),

    /**
     * @see Notification.GROUP_ALERT_CHILDREN
     */
    CHILDREN(Notification.GROUP_ALERT_CHILDREN, NotificationCompat.GROUP_ALERT_ALL),

    /**
     * @see Notification.GROUP_ALERT_SUMMARY
     */
    SUMMARY(Notification.GROUP_ALERT_SUMMARY, NotificationCompat.GROUP_ALERT_ALL)
}

enum class SemanticActions(internal val id: Int) {
    /**
     * []: No semantic action defined.
     */
    NONE(0),

    /**
     * `SemanticAction`: Reply to a conversation, chat, group, or wherever replies
     * may be appropriate.
     */
    REPLY(1),

    /**
     * `SemanticAction`: Mark content as read.
     */
    MARK_AS_READ(2),

    /**
     * `SemanticAction`: Mark content as unread.
     */
    MARK_AS_UNREAD(3),

    /**
     * `SemanticAction`: Delete the content associated with the notification. This
     * could mean deleting an email, message, etc.
     */
    DELETE(4),

    /**
     * `SemanticAction`: Archive the content associated with the notification. This
     * could mean archiving an email, message, etc.
     */
    ARCHIVE(5),

    /**
     * `SemanticAction`: Mute the content associated with the notification. This could
     * mean silencing a conversation or currently playing media.
     */
    MUTE(6),

    /**
     * `SemanticAction`: Unmute the content associated with the notification. This could
     * mean un-silencing a conversation or currently playing media.
     */
    UNMUTE(7),

    /**
     * `SemanticAction`: Mark content with a thumbs up.
     */
    THUMBS_UP(8),

    /**
     * `SemanticAction`: Mark content with a thumbs down.
     */
    THUMBS_DOWN(9),

    /**
     * `SemanticAction`: Call a contact, group, etc.
     */
    CALL(10)

}

//Style Builder
abstract class NotificationStyle {

    /**
     * Used on versions below O
     */
    abstract fun build(): NotificationCompat.Style

    /**
     * Used on versions O and up
     */
    abstract fun buildSdk(): Notification.Style

    class Inbox : NotificationStyle() {
        private val lines = mutableListOf<CharSequence>()

        /**
         * @see Notification.InboxStyle.addLine
         */
        @NotificationStyleMarker
        fun addLine(vararg cs: CharSequence) = lines.addAll(cs).let { }

        /**
         * @see Notification.InboxStyle.setBigContentTitle
         */
        @NotificationStyleMarker
        var contentTitle: CharSequence = ""

        /**
         * @see Notification.InboxStyle.setSummaryText
         */
        @NotificationStyleMarker
        var summaryText: CharSequence = ""

        override fun buildSdk(): Notification.Style = Notification.InboxStyle()
            .setBigContentTitle(contentTitle)
            .setSummaryText(summaryText)
            .also { builder -> lines.forEach { builder.addLine(it) } }

        override fun build(): NotificationCompat.Style = NotificationCompat.InboxStyle()
            .setBigContentTitle(contentTitle)
            .setSummaryText(summaryText)
            .also { builder -> lines.forEach { builder.addLine(it) } }

    }

    class Picture: NotificationStyle() {
        /**
         * @see Notification.BigPictureStyle.setBigContentTitle
         */
        @NotificationStyleMarker
        var contentTitle: CharSequence = ""

        /**
         * @see Notification.BigPictureStyle.setSummaryText
         */
        @NotificationStyleMarker
        var summaryText: CharSequence = ""

        /**
         * @see Notification.BigPictureStyle.bigPicture
         */
        @NotificationStyleMarker
        var bigPicture: Bitmap? = null

        /**
         * @see Notification.BigPictureStyle.bigLargeIcon
         */
        @NotificationStyleMarker
        var largeIcon: Bitmap? = null

        override fun buildSdk(): Notification.Style = Notification.BigPictureStyle()
            .bigLargeIcon(largeIcon)
            .bigPicture(bigPicture)
            .setBigContentTitle(contentTitle)
            .setSummaryText(summaryText)

        override fun build(): NotificationCompat.Style = NotificationCompat.BigPictureStyle()
            .bigLargeIcon(largeIcon)
            .bigPicture(bigPicture)
            .setBigContentTitle(contentTitle)
            .setSummaryText(summaryText)
    }

    class BigText : NotificationStyle() {

        /**
         * @see Notification.BigTextStyle.setBigContentTitle
         */
        @NotificationStyleMarker
        var contentTitle: CharSequence = ""

        /**
         * @see Notification.BigTextStyle.setSummaryText
         */
        @NotificationStyleMarker
        var summaryText: CharSequence = ""

        /**
         * @see Notification.BigTextStyle.bigText
         */
        @NotificationStyleMarker
        var bigText: CharSequence = ""

        override fun buildSdk(): Notification.Style = Notification.BigTextStyle()
            .bigText(bigText)
            .setBigContentTitle(contentTitle)
            .setSummaryText(summaryText)

        override fun build(): NotificationCompat.Style = NotificationCompat.BigTextStyle()
            .bigText(bigText)
            .setBigContentTitle(contentTitle)
            .setSummaryText(summaryText)

    }

    class Messaging internal constructor(private val context: Context) : NotificationStyle() {

        class Message {
            @NotificationStyleMarker
            var message: String by Delegates.notNull()

            @NotificationStyleMarker
            var timestamp: Long = System.currentTimeMillis()

            internal var person: NotificationPerson by Delegates.notNull()

            @NotificationStyleMarker
            fun setPerson(block: NotificationPerson.() -> Unit) = run { person = NotificationPerson().apply(block) }

            @NotificationStyleMarker
            fun setPerson(person: NotificationPerson) = run { this.person = person }
        }

        private var person: NotificationPerson by Delegates.notNull()

        @NotificationStyleMarker
        fun setMainPerson(block: NotificationPerson.() -> Unit) = run { person = NotificationPerson().apply(block) }

        /**
         * @see Notification.MessagingStyle.setGroupConversation
         * @see NotificationCompat.MessagingStyle.setGroupConversation
         */
        @NotificationStyleMarker
        var isGroupConversation: Boolean = false

        /**
         * @see Notification.MessagingStyle.setConversationTitle
         * @see NotificationCompat.MessagingStyle.setConversationTitle
         */
        @NotificationStyleMarker
        var conversationTitle: CharSequence? = null

        private val messages = mutableListOf<Message>()

        @NotificationStyleMarker
        fun message(block: Message.() -> Unit) = run { messages.add(Message().apply(block)) }

        private val historicMessages = mutableListOf<Message>()

        @RequiresApi(Build.VERSION_CODES.P)
        @NotificationStyleMarker
        fun historicMessage(block: Message.() -> Unit) = run { historicMessages.add(Message().apply(block)) }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun build(): NotificationCompat.Style = NotificationCompat.MessagingStyle(person.build(context))
            .setConversationTitle(conversationTitle)
            .setGroupConversation(isGroupConversation)
            .also { builder -> messages.forEach { builder.addMessage(it.message, it.timestamp, it.person.build(context)) } }

        @RequiresApi(Build.VERSION_CODES.P)
        override fun buildSdk(): Notification.Style = Notification.MessagingStyle(person.buildSdk())
            .setConversationTitle(conversationTitle)
            .setGroupConversation(isGroupConversation)
            .also { builder -> messages.forEach { builder.addMessage(it.message, it.timestamp, it.person.buildSdk()) } }
            .also { builder ->
                historicMessages.forEach {
                    builder.addHistoricMessage(Notification.MessagingStyle.Message(it.message, it.timestamp, it.person.buildSdk()))
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    class Media internal constructor() : NotificationStyle() {

        /**
         * @see Notification.MediaStyle.setMediaSession
         */
        @NotificationStyleMarker
        var mediaSessionToken: MediaSession.Token by Delegates.notNull()

        private var actions: IntArray? = null

        /**
         * @see Notification.MediaStyle.setShowActionsInCompactView
         */
        @NotificationStyleMarker
        fun actions(vararg action: Int) = run { actions = action }

        override fun buildSdk(): Notification.Style = Notification.MediaStyle()
            .setMediaSession(mediaSessionToken)
            .also { if (actions != null) it.setShowActionsInCompactView(*actions!!) }

        override fun build(): NotificationCompat.Style = throw NotificationException("Media Style is only usable on version O and up")

    }
}

//Action Builder
sealed class NotificationAction(private val context: Context) {

    /**
     * @see Notification.Action.title
     */
    @NotificationActionMarker
    var actionTitle: CharSequence by Delegates.notNull()

    /**
     * @see Notification.Action.icon
     */
    @NotificationActionMarker
    var actionIcon: Int by Delegates.notNull()

    class Reply internal constructor(context: Context) : NotificationAction(context) {

        /**
         * @see RemoteInput.Builder.mResultKey
         */
        @NotificationActionMarker
        var resultKey: String by Delegates.notNull()

        /**
         * @see RemoteInput.Builder.setLabel
         */
        @NotificationActionMarker
        var label: String by Delegates.notNull()

        /**
         * @see RemoteInput.Builder.setAllowFreeFormInput
         */
        @NotificationActionMarker
        var allowFreeFormInput: Boolean = true

        private val choices = mutableListOf<CharSequence>()

        operator fun CharSequence.unaryPlus() = choices.add(this).let { }

        /**
         * @see RemoteInput.Builder.setChoices
         */
        @NotificationActionMarker
        fun addChoice(s: CharSequence) = choices.add(s).let { }

        /**
         * @see RemoteInput.Builder.setChoices
         */
        @NotificationActionMarker
        fun addChoices(vararg s: CharSequence) = choices.addAll(s).let { }

        internal fun buildRemoteInput() = RemoteInput.Builder(resultKey)
            .setLabel(label)
            .setAllowFreeFormInput(allowFreeFormInput)
            .also { if (choices.isNotEmpty()) it.setChoices(choices.toTypedArray()) }
            .build()

        @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
        internal fun buildRemoteInputSdk() = android.app.RemoteInput.Builder(resultKey)
            .setLabel(label)
            .setAllowFreeFormInput(allowFreeFormInput)
            .also { if (choices.isNotEmpty()) it.setChoices(choices.toTypedArray()) }
            .build()

    }

    class Action(context: Context) : NotificationAction(context)

    /**
     * @see Notification.Action.Builder.setContextual
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @NotificationActionMarker
    var contextual: Boolean = false

    /**
     * @see Notification.Action.Builder.setAllowGeneratedReplies
     */
    @RequiresApi(Build.VERSION_CODES.N)
    @NotificationActionMarker
    var allowGeneratedReplies: Boolean = true

    /**
     * @see Notification.Action.Builder.setSemanticAction
     */
    @RequiresApi(Build.VERSION_CODES.P)
    @NotificationActionMarker
    var semanticAction: SemanticActions = SemanticActions.NONE

    @RequiresApi(Build.VERSION_CODES.M)
    internal fun buildSdk() = Notification.Action.Builder(Icon.createWithResource(context, actionIcon), actionTitle, pendingIntentAction)
        .also { if (this is Reply) it.addRemoteInput(buildRemoteInputSdk()) }
        .also { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) it.setAllowGeneratedReplies(allowGeneratedReplies) }
        .also { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) it.setContextual(contextual) }
        .also { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.setSemanticAction(semanticAction.id) }
        .build()

    internal fun build() = NotificationCompat.Action.Builder(actionIcon, actionTitle, pendingIntentAction)
        .also { if (this is Reply) it.addRemoteInput(buildRemoteInput()) }
        .setAllowGeneratedReplies(allowGeneratedReplies)
        .let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) it.setContextual(contextual) else it }
        .setSemanticAction(semanticAction.id)
        .build()

    private var pendingIntentAction: PendingIntent? = null

    @NotificationActionMarker
    fun pendingActionIntent(gotoActivity: Class<*>, requestCode: Int = 0, block: Intent.() -> Unit = {}) {
        pendingIntentAction = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, gotoActivity).apply(block),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @NotificationActionMarker
    fun pendingActionIntent(pendingIntent: PendingIntent?) = run { pendingIntentAction = pendingIntent }

    @NotificationActionMarker
    fun pendingActionIntent(block: (context: Context) -> PendingIntent?) = run { pendingIntentAction = block(context) }

}

class NotificationBubble internal constructor(private val context: Context) {

    /**
     * @see Notification.BubbleMetadata.Builder.setDesiredHeight
     * @see NotificationCompat.BubbleMetadata.Builder.setDesiredHeight
     */
    @NotificationBubbleMarker
    var desiredHeight: Int = 0

    /**
     * @see Notification.BubbleMetadata.Builder.setDesiredHeight
     * @see NotificationCompat.BubbleMetadata.Builder.setDesiredHeight
     */
    @NotificationBubbleMarker
    @get:DimenRes
    @setparam:DimenRes
    var desiredHeightRes: Int = 0

    /**
     * @see Notification.BubbleMetadata.Builder.setIcon
     * @see NotificationCompat.BubbleMetadata.Builder.setIcon
     */
    @NotificationBubbleMarker
    var icon: Icon by Delegates.notNull()

    /**
     * @see Notification.BubbleMetadata.Builder.setIcon
     * @see NotificationCompat.BubbleMetadata.Builder.setIcon
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @NotificationBubbleMarker
    fun setIconById(@DrawableRes id: Int) = run { icon = Icon.createWithResource(context, id) }

    /**
     * @see Notification.BubbleMetadata.Builder.setSuppressNotification
     * @see NotificationCompat.BubbleMetadata.Builder.setSuppressNotification
     */
    @NotificationBubbleMarker
    var suppressNotification: Boolean = false

    /**
     * @see Notification.BubbleMetadata.Builder.setAutoExpandBubble
     * @see NotificationCompat.BubbleMetadata.Builder.setAutoExpandBubble
     */
    @NotificationBubbleMarker
    var autoExpandBubble: Boolean = false

    private var bubbleIntent: PendingIntent? = null

    /**
     * The activity to show
     *
     * The activity must have the three properties below
     * ```xml
     * <activity
     **   ...
     **   android:allowEmbedded="true"
     **   android:documentLaunchMode="always"
     **   android:resizeableActivity="true"/>
     * ```
     */
    @NotificationBubbleMarker
    fun bubbleIntent(pendingIntent: PendingIntent?) = run { bubbleIntent = pendingIntent }

    /**
     * The activity to show
     *
     * The activity must have the three properties below
     * ```xml
     * <activity
     **   ...
     **   android:allowEmbedded="true"
     **   android:documentLaunchMode="always"
     **   android:resizeableActivity="true"/>
     * ```
     */
    @NotificationBubbleMarker
    fun bubbleIntent(block: (context: Context) -> PendingIntent?) = run { bubbleIntent = block(context) }

    private var deleteIntent: PendingIntent? = null

    /**
     * @see Notification.BubbleMetadata.Builder.setDeleteIntent
     * @see NotificationCompat.BubbleMetadata.Builder.setDeleteIntent
     */
    @NotificationBubbleMarker
    fun deleteIntent(pendingIntent: PendingIntent?) = run { deleteIntent = pendingIntent }

    /**
     * @see Notification.BubbleMetadata.Builder.setDeleteIntent
     * @see NotificationCompat.BubbleMetadata.Builder.setDeleteIntent
     */
    @NotificationBubbleMarker
    fun deleteIntent(block: (context: Context) -> PendingIntent?) = run { deleteIntent = block(context) }

    @RequiresApi(Build.VERSION_CODES.Q)
    internal fun build() = Notification.BubbleMetadata.Builder()
        .also { builder ->
            when {
                desiredHeight == 0 -> builder.setDesiredHeightResId(desiredHeightRes)
                desiredHeightRes == 0 -> builder.setDesiredHeight(desiredHeight)
                else -> throw NotificationException("Desired Height cannot be 0")
            }
        }
        .setIcon(icon)
        .also { builder -> bubbleIntent?.let { builder.setIntent(it) } }
        .setSuppressNotification(suppressNotification)
        .setAutoExpandBubble(autoExpandBubble)
        .setDeleteIntent(deleteIntent)
        .build()

    @RequiresApi(Build.VERSION_CODES.M)
    internal fun buildSdk(context: Context) = NotificationCompat.BubbleMetadata.Builder()
        .also { builder ->
            when {
                desiredHeight == 0 -> builder.setDesiredHeightResId(desiredHeightRes)
                desiredHeightRes == 0 -> builder.setDesiredHeight(desiredHeight)
                else -> throw NotificationException("Desired Height cannot be 0")
            }
        }
        .setDesiredHeight(desiredHeight)
        .setIcon(IconCompat.createFromIcon(context, icon)!!)
        .also { builder -> bubbleIntent?.let { builder.setIntent(it) } }
        .setSuppressNotification(suppressNotification)
        .setAutoExpandBubble(autoExpandBubble)
        .setDeleteIntent(deleteIntent)
        .build()
}

class RemoteViewBuilder internal constructor() {

    private var portraitHeadsUp: RemoteViews? = null
    private var landscapeHeadsUp: RemoteViews? = null

    /**
     * @see Notification.Builder.setCustomHeadsUpContentView
     * @see NotificationCompat.Builder.setCustomHeadsUpContentView
     */
    @RemoteMarker
    fun portraitHeadsUp(packageName: String, @LayoutRes layout: Int, block: RemoteViews.() -> Unit = {}) {
        portraitHeadsUp = RemoteViews(packageName, layout).apply(block)
    }

    /**
     * @see Notification.Builder.setCustomHeadsUpContentView
     * @see NotificationCompat.Builder.setCustomHeadsUpContentView
     */
    @RemoteMarker
    fun landscapeHeadsUp(packageName: String, @LayoutRes layout: Int, block: RemoteViews.() -> Unit = {}) {
        landscapeHeadsUp = RemoteViews(packageName, layout).apply(block)
    }

    private var portraitCollapsed: RemoteViews? = null
    private var landscapeCollapsed: RemoteViews? = null

    /**
     * @see Notification.Builder.setCustomContentView
     * @see NotificationCompat.Builder.setCustomContentView
     */
    @RemoteMarker
    fun portraitCollapsed(packageName: String, @LayoutRes layout: Int, block: RemoteViews.() -> Unit = {}) {
        portraitCollapsed = RemoteViews(packageName, layout).apply(block)
    }

    /**
     * @see Notification.Builder.setCustomContentView
     * @see NotificationCompat.Builder.setCustomContentView
     */
    @RemoteMarker
    fun landscapeCollapsed(packageName: String, @LayoutRes layout: Int, block: RemoteViews.() -> Unit = {}) {
        landscapeCollapsed = RemoteViews(packageName, layout).apply(block)
    }

    private var portraitExpanded: RemoteViews? = null
    private var landscapeExpanded: RemoteViews? = null

    /**
     * @see Notification.Builder.setCustomBigContentView
     * @see NotificationCompat.Builder.setCustomBigContentView
     */
    @RemoteMarker
    fun portraitExpanded(packageName: String, @LayoutRes layout: Int, block: RemoteViews.() -> Unit = {}) {
        portraitExpanded = RemoteViews(packageName, layout).apply(block)
    }

    /**
     * @see Notification.Builder.setCustomBigContentView
     * @see NotificationCompat.Builder.setCustomBigContentView
     */
    @RemoteMarker
    fun landscapeExpanded(packageName: String, @LayoutRes layout: Int, block: RemoteViews.() -> Unit = {}) {
        landscapeExpanded = RemoteViews(packageName, layout).apply(block)
    }

    internal fun build() = NotificationRemoteView(
        headsUp = if (landscapeHeadsUp == null || portraitHeadsUp == null) null else RemoteViews(landscapeHeadsUp, portraitHeadsUp),
        collapsed = if (landscapeCollapsed == null || portraitCollapsed == null) null else RemoteViews(landscapeCollapsed, portraitCollapsed),
        expanded = if (landscapeExpanded == null || portraitExpanded == null) null else RemoteViews(landscapeExpanded, portraitExpanded)
    )

}

internal class NotificationRemoteView(internal val headsUp: RemoteViews?, internal val collapsed: RemoteViews?, internal val expanded: RemoteViews?)

enum class NotificationCategory(internal val info: String) {
    /**
     * Notification category: incoming call (voice or video) or similar synchronous communication request.
     */
    CALL("call"),

    /**
     * Notification category: map turn-by-turn navigation.
     */
    NAVIGATION("navigation"),

    /**
     * Notification category: incoming direct message (SMS, instant message, etc.).
     */
    MESSAGE("msg"),

    /**
     * Notification category: asynchronous bulk message (email).
     */
    EMAIL("email"),

    /**
     * Notification category: calendar event.
     */
    EVENT("event"),

    /**
     * Notification category: promotion or advertisement.
     */
    PROMO("promo"),

    /**
     * Notification category: alarm or timer.
     */
    ALARM("alarm"),

    /**
     * Notification category: progress of a long-running background operation.
     */
    PROGRESS("progress"),

    /**
     * Notification category: social network or sharing update.
     */
    SOCIAL("social"),

    /**
     * Notification category: error in background operation or authentication status.
     */
    ERROR("err"),

    /**
     * Notification category: media transport control for playback.
     */
    TRANSPORT("transport"),

    /**
     * Notification category: system or device status update.  Reserved for system use.
     */
    SYSTEM("sys"),

    /**
     * Notification category: indication of running background service.
     */
    SERVICE("service"),

    /**
     * Notification category: a specific, timely recommendation for a single thing.
     * For example, a news app might want to recommend a news story it believes the user will
     * want to read next.
     */
    RECOMMENDATION("recommendation"),

    /**
     * Notification category: ongoing information about device or contextual status.
     */
    STATUS("status"),

    /**
     * Notification category: user-scheduled reminder.
     */
    REMINDER("reminder")
}

class NotificationPerson {

    @NotificationPersonBuilder
    var isBot: Boolean = false

    @NotificationPersonBuilder
    var isImportant: Boolean = false

    @NotificationPersonBuilder
    var name: String by Delegates.notNull()

    @NotificationPersonBuilder
    var icon: Icon? = null

    @NotificationPersonBuilder
    var key: String? = null

    @NotificationPersonBuilder
    var uri: String? = null

    @RequiresApi(Build.VERSION_CODES.M)
    internal fun build(context: Context) = androidx.core.app.Person.Builder()
        .setBot(isBot)
        .setIcon(icon?.let { IconCompat.createFromIcon(context, it) })
        .setImportant(isImportant)
        .whatIfNotNull(key) { setKey(it) }
        .setName(name)
        .setUri(uri)
        .build()

    @RequiresApi(Build.VERSION_CODES.P)
    internal fun buildSdk() = Person.Builder()
        .setBot(isBot)
        .setIcon(icon)
        .setImportant(isImportant)
        .whatIfNotNull(key) { setKey(it) }
        .setName(name)
        .setUri(uri)
        .build()
}

inline fun <T, R> T.whatIfNotNull(
    given: R?,
    whatIfNotNull: T.(R) -> Unit,
) = apply { given?.let { whatIfNotNull(it) } }