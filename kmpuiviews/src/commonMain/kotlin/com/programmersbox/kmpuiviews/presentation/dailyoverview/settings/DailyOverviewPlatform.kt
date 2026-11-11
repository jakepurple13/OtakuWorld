package com.programmersbox.kmpuiviews.presentation.dailyoverview.settings

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

/** Only Android posts the morning notification; elsewhere the settings hide that group. */
expect val supportsMorningNotification: Boolean

/** The AI Helper's provider settings screen, or null where there is no AI Helper (iOS). */
expect val aiHelperSettingsRoute: NavKey?

/** Whether this app may post notifications, and how to ask for it. */
class NotificationPermission(val granted: Boolean, val request: () -> Unit)

/** Always granted where there is no morning notification. */
@Composable
expect fun rememberNotificationPermission(): NotificationPermission
