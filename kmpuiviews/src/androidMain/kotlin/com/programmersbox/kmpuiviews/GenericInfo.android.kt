package com.programmersbox.kmpuiviews

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.settings.ScreensaverTypeSettings
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.DeepLinks
import com.programmersbox.kmpuiviews.utils.composables.WidgetAddCard
import com.programmersbox.kmpuiviews.utils.composables.widgetChecker
import com.programmersbox.kmpuiviews.widget.notification.NotificationWidgetReceiver
import org.koin.compose.koinInject

actual interface PlatformGenericInfo : KmpGenericInfo {
    val deepLinkUri: String

    val deepLinks: DeepLinks
        get() = DeepLinks(this)

    fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent?

    fun deepLinkSettings(context: Context): PendingIntent?

    fun deepLinkUpdateCheckErrors(context: Context): PendingIntent?

    /**
     * Opens [uri] in this app's main activity, which matches it against [DeepLinks]. The activity
     * cancels [cancelNotificationId] when set, because notification actions don't auto-cancel.
     */
    fun deepLink(context: Context, uri: Uri, cancelNotificationId: Int? = null): PendingIntent?

    @SuppressLint("RestrictedApi")
    fun deepLinkDetailsUri(itemModel: KmpItemModel?): Uri {
        return itemModel?.let {
            deepLinkDetailsUri(url = it.url, imageUrl = it.imageUrl, source = it.source.serviceName)
        } ?: "$deepLinkUri${Screen.DetailsScreen.route}".toUri()
    }

    fun deepLinkDetailsUri(url: String, imageUrl: String, source: String): Uri = deepLinks
        .details
        .createUri(
            Screen.DetailsScreen.Details(
                title = "title",
                description = "description",
                url = url,
                imageUrl = imageUrl,
                source = source
            )
        )
        .toUri()

    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {
        generalSettings {
            val state = widgetChecker(NotificationWidgetReceiver::class.java)
            WidgetAddCard(
                widgetCheckerState = state.value,
                description = "View saved notifications straight on your home screen!"
            )
        }

        layoutSettings {
            ScreensaverTypeSettings(handling = koinInject())
        }
    }

    fun deepLinkSettingsUri() = deepLinks
        .notification
        .createUri(Screen.NotificationScreen)
        .toUri()

    fun deepLinkUpdateCheckErrorsUri(): Uri = deepLinks
        .updateCheckErrors
        .createUri(Screen.UpdateCheckErrors)
        .toUri()

    fun deepLinkDailyOverviewUri(): Uri = deepLinks
        .dailyOverview
        .createUri(Screen.DailyOverview)
        .toUri()

    fun <T : FragmentActivity> deepLinkSetup(
        context: Context,
        uri: Uri,
        activity: Class<T>,
        cancelNotificationId: Int? = null,
    ): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            uri,
            context,
            activity
        )
        cancelNotificationId?.let { deepLinkIntent.putExtra(EXTRA_CANCEL_NOTIFICATION_ID, it) }

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            // Intents that cancel a notification get their own request code, so FLAG_UPDATE_CURRENT
            // never swaps extras with another notification's intent for the same uri.
            getPendingIntent(cancelNotificationId ?: 13, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}

/** Int extra on a deep-link intent: the notification to cancel when the activity opens it. */
const val EXTRA_CANCEL_NOTIFICATION_ID = "cancel_notification_id"
