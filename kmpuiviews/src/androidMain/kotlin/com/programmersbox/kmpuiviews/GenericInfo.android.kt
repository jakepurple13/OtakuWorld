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
import com.programmersbox.kmpuiviews.utils.DeepLinks

actual interface PlatformGenericInfo : KmpGenericInfo {
    val deepLinkUri: String

    val deepLinks: DeepLinks
        get() = DeepLinks(this)

    fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent?

    fun deepLinkSettings(context: Context): PendingIntent?

    @SuppressLint("RestrictedApi")
    fun deepLinkDetailsUri(itemModel: KmpItemModel?): Uri {
        return itemModel?.let {
            deepLinks
                .details
                .createUri(
                    Screen.DetailsScreen.Details(
                        title = "title",
                        description = "description",
                        url = it.url,
                        imageUrl = it.imageUrl,
                        source = it.source.serviceName
                    )
                )
                .toUri()
        } ?: "$deepLinkUri${Screen.DetailsScreen.route}".toUri()
    }

    fun deepLinkSettingsUri() = deepLinks
        .notification
        .createUri(Screen.NotificationScreen)
        .toUri()

    fun <T : FragmentActivity> deepLinkSetup(
        context: Context,
        uri: Uri,
        activity: Class<T>,
    ): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            uri,
            context,
            activity
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(13, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}