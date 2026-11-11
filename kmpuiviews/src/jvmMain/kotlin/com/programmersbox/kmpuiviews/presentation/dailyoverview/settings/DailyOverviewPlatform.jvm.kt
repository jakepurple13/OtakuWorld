package com.programmersbox.kmpuiviews.presentation.dailyoverview.settings

import androidx.compose.runtime.Composable

actual val supportsMorningNotification: Boolean = false

@Composable
actual fun rememberNotificationPermission(): NotificationPermission = NotificationPermission(granted = true, request = {})
