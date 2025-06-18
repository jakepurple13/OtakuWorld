package com.programmersbox.uiviews.utils

import android.content.Context
import com.programmersbox.helpfulutils.NotificationChannelImportance
import com.programmersbox.helpfulutils.createNotificationChannel
import com.programmersbox.helpfulutils.createNotificationGroup

enum class NotificationChannels(
    val id: String,
    val importance: NotificationChannelImportance = NotificationChannelImportance.DEFAULT,
) {
    Otaku("otakuChannel", NotificationChannelImportance.HIGH),
    UpdateCheck("updateCheckChannel", NotificationChannelImportance.MIN),
    AppUpdate("appUpdate", NotificationChannelImportance.HIGH),
    SourceUpdate("sourceUpdate", NotificationChannelImportance.DEFAULT),
    Download("download_channel", NotificationChannelImportance.DEFAULT);

    companion object {
        fun setupNotificationChannels(context: Context) {
            entries.forEach {
                context.createNotificationChannel(it.id, importance = it.importance)
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
            entries.forEach {
                context.createNotificationGroup(it.id)
            }
        }
    }
}