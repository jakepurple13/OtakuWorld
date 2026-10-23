package com.programmersbox.anime.shared.cast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Keeps the process alive while casting so the local media server keeps serving the receiver. */
class CastSessionService : Service(), KoinComponent {

    private val logo: NotificationLogo by inject()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val device = intent?.getStringExtra(EXTRA_DEVICE).orEmpty()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Casting", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(logo.notificationId)
            .setContentTitle("Casting to $device")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        return START_NOT_STICKY
    }

    companion object {
        private const val TAG = "CastSessionService"
        private const val CHANNEL_ID = "cast_session"
        private const val NOTIFICATION_ID = 0xCA57
        private const val EXTRA_DEVICE = "device"

        fun start(context: Context, deviceName: String) {
            val intent = Intent(context, CastSessionService::class.java).putExtra(EXTRA_DEVICE, deviceName)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException (Android 12+) when not started from the foreground.
                Log.w(TAG, "Could not start cast foreground service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CastSessionService::class.java))
        }
    }
}
