package com.programmersbox.uiviews.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.checkers.SavedNotifications
import com.programmersbox.uiviews.utils.NotificationLogo
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceived : BroadcastReceiver(), KoinComponent {

    private val logo: NotificationLogo by inject()
    private val info: GenericInfo by inject()
    private val sourceRepository: SourceRepository by inject()
    private val settingsHandling: NewSettingsHandling by inject()

    private val itemDao: ItemDao by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        println("BootReceived")
        println(intent?.action)
        runCatching {
            runBlocking {
                if (settingsHandling.notifyOnReboot.get()) {
                    context?.let {
                        SavedNotifications.viewNotificationsFromDb(
                            context = it,
                            logo = logo,
                            info = info,
                            sourceRepository = sourceRepository,
                            dao = itemDao
                        )
                    }
                }
            }
        }
    }
}