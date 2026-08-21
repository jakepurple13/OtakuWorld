package com.programmersbox.kmpuiviews.widget.notification

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.programmersbox.datastore.PlatformDataStoreHandling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationWidgetReceiver : GlanceAppWidgetReceiver(), KoinComponent {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val platformDataStoreHandling by inject<PlatformDataStoreHandling>()

    override val glanceAppWidget: GlanceAppWidget = NotificationWidget()

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        scope.launch { platformDataStoreHandling.hasWidget.set(true) }
    }

    override fun onDisabled(context: Context?) {
        scope.launch { platformDataStoreHandling.hasWidget.set(false) }
        super.onDisabled(context)
    }
}


