package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class NotificationsBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "notifications.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Saved Notifications"
    override val description: String? get() = "Notification inbox items"
    override val icon get() = Icons.Default.Notifications

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

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getAllNotifications().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<NotificationItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
