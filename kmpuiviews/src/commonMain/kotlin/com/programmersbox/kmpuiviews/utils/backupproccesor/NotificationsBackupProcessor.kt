package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import okio.BufferedSink
import okio.BufferedSource

class NotificationsBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor() {
    override val fileName: String
        get() = "notifications.json"

    override suspend fun backup(sink: BufferedSink) {
        itemDao
            .getAllNotifications()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<NotificationItem>>()
            .forEach { itemDao.insertNotification(it) }
    }
}