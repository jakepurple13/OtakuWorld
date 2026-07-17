package com.programmersbox.jsextensionloader

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

class JsExtensionUpdateWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val notificationManager by lazy { applicationContext.getSystemService<NotificationManager>() }

    override suspend fun doWork(): Result {
        return try {
            val repository = get<JsExtensionRepository>()
            val discovery = get<ExtensionDiscovery>()
            val loader = get<JSExtensionLoader>()
            val updateChecker = get<ExtensionUpdateChecker>()
            val settings = get<JsExtensionUpdateSettings>()
            val registryEndpoint = getKoin().getOrNull<String>(named("jsExtensionRegistryEndpoint"))

            val runner = JsExtensionUpdateRunner(
                repository = repository,
                discovery = discovery,
                loader = loader,
                updateChecker = updateChecker,
                settings = settings,
                registryEndpoint = registryEndpoint,
                onUpdateAvailable = { update ->
                    val notification = NotificationCompat.Builder(applicationContext, JS_EXTENSION_UPDATE_CHANNEL_ID)
                        .setContentTitle("${update.id} has an update!")
                        .setContentText("${update.latestVersion} is available.")
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .build()
                    notificationManager?.notify(update.id.hashCode(), notification)
                },
            )
            runner.run()
            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }

    companion object {
        const val JS_EXTENSION_UPDATE_CHANNEL_ID = "jsExtensionUpdateChannel"
    }
}
