package com.programmersbox.kmpuiviews.utils

import android.app.NotificationChannelGroup
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

enum class NotificationChannels(
    val id: String,
    val importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT,
) {
    Otaku("otakuChannel", NotificationManagerCompat.IMPORTANCE_HIGH),
    UpdateCheck("updateCheckChannel", NotificationManagerCompat.IMPORTANCE_MIN),
    AppUpdate("appUpdate", NotificationManagerCompat.IMPORTANCE_HIGH),
    SourceUpdate("sourceUpdate", NotificationManagerCompat.IMPORTANCE_DEFAULT),
    Download("download_channel", NotificationManagerCompat.IMPORTANCE_DEFAULT);

    companion object {
        fun setupNotificationChannels(context: Context) {
            val notificationManager = NotificationManagerCompat.from(context)
            entries.forEach {
                notificationManager.createNotificationChannel(
                    NotificationChannelCompat.Builder(it.id, it.importance)
                        .setName(it.id)
                        .build()
                )
            }
        }
    }
}

enum class NotificationGroups(
    val id: String,
) {
    Otaku("otakuGroup"),
    Sources("sources");

    companion object {
        fun setupNotificationGroups(context: Context) {
            val notificationManager = NotificationManagerCompat.from(context)
            entries.forEach {
                notificationManager.createNotificationChannelGroup(
                    NotificationChannelGroup(it.id, it.id)
                )
            }
        }
    }
}