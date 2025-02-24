package com.programmersbox.uiviews.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.loggingutils.Loged
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.checkers.SavedNotifications
import com.programmersbox.uiviews.datastore.SettingsHandling
import com.programmersbox.uiviews.utils.NotificationLogo
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceived : BroadcastReceiver(), KoinComponent {

    private val logo: NotificationLogo by inject()
    private val info: GenericInfo by inject()
    private val sourceRepository: SourceRepository by inject()
    private val settingsHandling: SettingsHandling by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        Loged.d("BootReceived")
        println(intent?.action)
        runCatching {
            runBlocking {
                if (settingsHandling.notifyOnReboot.get()) {
                    context?.let { SavedNotifications.viewNotificationsFromDb(it, logo, info, sourceRepository) }
                }
            }
        }
    }
}