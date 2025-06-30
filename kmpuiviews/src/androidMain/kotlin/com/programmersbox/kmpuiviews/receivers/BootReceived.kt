package com.programmersbox.kmpuiviews.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.workers.SavedNotifications
import com.programmersbox.kmpuiviews.workers.UpdateNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class BootReceived : BroadcastReceiver(), KoinComponent {

    private val logo: NotificationLogo by inject()
    private val info: PlatformGenericInfo by inject()
    private val sourceRepository: SourceRepository by inject()
    private val settingsHandling: NewSettingsHandling by inject()
    private val updateNotification: UpdateNotification by inject()
    private val itemDao: ItemDao by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        logFirebaseMessage("BootReceived ${intent?.action}")
        runCatching {
            goAsync {
                if (settingsHandling.notifyOnReboot.get()) {
                    context?.let {
                        SavedNotifications.viewNotificationsFromDb(
                            context = it,
                            logo = logo,
                            info = info,
                            sourceRepository = sourceRepository,
                            dao = itemDao,
                            update = updateNotification
                        )
                    }
                }
            }
        }
    }
}

fun BroadcastReceiver.goAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.(BroadcastReceiver.PendingResult) -> Unit,
) {
    val pendingResult = goAsync()
    @OptIn(DelicateCoroutinesApi::class) // Must run globally; there's no teardown callback.
    GlobalScope.launch(context) {
        try {
            block(pendingResult)
        } finally {
            pendingResult.finish()
        }
    }
}