package com.programmersbox.kmpuiviews.presentation.notifications

import androidx.compose.runtime.Composable
import com.programmersbox.favoritesdatabase.NotificationItem

interface NotificationScreenInterface {
    suspend fun notifyItem(notificationItem: NotificationItem)

    @Composable
    fun NotifyAt(
        item: NotificationItem,
        content: @Composable (() -> Unit) -> Unit,
    )
}