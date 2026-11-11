package com.programmersbox.kmpuiviews.presentation.dailyoverview.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

actual val supportsMorningNotification: Boolean = true

/**
 * Re-checked on every resume, so coming back from the system settings updates it. Asks with the
 * runtime prompt while Android still shows it, otherwise opens this app's notification settings.
 */
@Composable
actual fun rememberNotificationPermission(): NotificationPermission {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var granted by remember { mutableStateOf(context.notificationsAllowed()) }
    LifecycleResumeEffect(Unit) {
        granted = context.notificationsAllowed()
        onPauseOrDispose {}
    }
    val openSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = context.notificationsAllowed()
        // No prompt was shown (denied for good), so the settings page is the only way left.
        if (!ok && activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == false) {
            openSettings()
        }
    }
    return NotificationPermission(granted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.hasPostPermission()) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openSettings()
        }
    }
}

private fun Context.hasPostPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.notificationsAllowed() = NotificationManagerCompat.from(this).areNotificationsEnabled()
