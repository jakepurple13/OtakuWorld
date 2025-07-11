package com.programmersbox.kmpuiviews

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.serialization.generateRouteWithArgs
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.presentation.Screen

actual interface PlatformGenericInfo : KmpGenericInfo {
    val deepLinkUri: String

    fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent?

    fun deepLinkSettings(context: Context): PendingIntent?

    @SuppressLint("RestrictedApi")
    fun deepLinkDetailsUri(itemModel: KmpItemModel?): Uri {
        @Suppress("UNCHECKED_CAST")
        val route = generateRouteWithArgs(
            Screen.DetailsScreen.Details(
                title = itemModel?.title ?: "",
                description = itemModel?.description ?: "",
                url = itemModel?.url ?: "",
                imageUrl = itemModel?.imageUrl ?: "",
                source = itemModel?.source?.serviceName ?: "",
            ),
            mapOf(
                "title" to NavType.StringType as NavType<Any?>,
                "description" to NavType.StringType as NavType<Any?>,
                "url" to NavType.StringType as NavType<Any?>,
                "imageUrl" to NavType.StringType as NavType<Any?>,
                "source" to NavType.StringType as NavType<Any?>,
            )
        )

        return "$deepLinkUri$route".toUri()
    }

    fun deepLinkSettingsUri() = "$deepLinkUri${Screen.NotificationScreen.route}".toUri()
}