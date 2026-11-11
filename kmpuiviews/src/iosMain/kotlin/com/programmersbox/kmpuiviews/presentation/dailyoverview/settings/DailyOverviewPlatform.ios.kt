package com.programmersbox.kmpuiviews.presentation.dailyoverview.settings

import androidx.compose.runtime.Composable

import androidx.navigation3.runtime.NavKey

actual val supportsMorningNotification: Boolean = false

actual val aiHelperSettingsRoute: NavKey? = null

@Composable
actual fun rememberNotificationPermission(): NotificationPermission = NotificationPermission(granted = true, request = {})
