package com.programmersbox.uiviews

import android.app.Application
import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.*
import com.facebook.stetho.Stetho
import com.programmersbox.helpfulutils.NotificationChannelImportance
import com.programmersbox.helpfulutils.createNotificationChannel
import com.programmersbox.helpfulutils.createNotificationGroup
import com.programmersbox.loggingutils.Loged
import com.programmersbox.sharedutils.FirebaseUIStyle
import com.programmersbox.uiviews.utils.shouldCheck
import io.reactivex.plugins.RxJavaPlugins
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

abstract class OtakuApp : Application() {

    override fun onCreate() {
        super.onCreate()
        //TODO: Will add this in the future when material3 is more stable
        //DynamicColors.applyToActivitiesIfAvailable(this)

        if (BuildConfig.DEBUG) Stetho.initializeWithDefaults(this)

        Loged.FILTER_BY_PACKAGE_NAME = "programmersbox"
        Loged.TAG = this::class.java.simpleName

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("otakuChannel", importance = NotificationChannelImportance.HIGH)
            createNotificationGroup("otakuGroup")
            createNotificationChannel("updateCheckChannel", importance = NotificationChannelImportance.MIN)
            createNotificationChannel("appUpdate", importance = NotificationChannelImportance.HIGH)
        }

        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            //FirebaseCrashlytics.getInstance().recordException(it)
        }

        startKoin {
            androidLogger()
            androidContext(this@OtakuApp)
            loadKoinModules(module { single { FirebaseUIStyle(R.style.Theme_OtakuWorldBase) } })
        }

        onCreated()

        val work = WorkManager.getInstance(this)

        work.enqueueUniquePeriodicWork(
            "appChecks",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(AppCheckWorker::class.java, 1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setRequiresStorageNotLow(false)
                        .build()
                )
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build()
        ).state.observeForever { println(it) }

        updateSetup(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) shortcutSetup()

    }

    abstract fun onCreated()

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    protected open fun shortcuts(): List<ShortcutInfo> = emptyList()

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun shortcutSetup() {
        val manager = getSystemService(ShortcutManager::class.java)
        if (manager.dynamicShortcuts.size == 0) {
            // Application restored. Need to re-publish dynamic shortcuts.
            if (manager.pinnedShortcuts.size > 0) {
                // Pinned shortcuts have been restored. Use
                // updateShortcuts() to make sure they contain
                // up-to-date information.
                manager.removeAllDynamicShortcuts()
            }
        }

        val shortcuts = mutableListOf<ShortcutInfo>()

        shortcuts.addAll(shortcuts())

        manager.dynamicShortcuts = shortcuts
    }

    companion object {

        fun updateSetup(context: Context) {
            val work = WorkManager.getInstance(context)
            //work.cancelAllWork()
            if (context.shouldCheck) {
                work.enqueueUniquePeriodicWork(
                    "updateChecks",
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<UpdateWorker>(
                        1, TimeUnit.HOURS,
                        5, TimeUnit.MINUTES
                    )
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .setInitialDelay(10, TimeUnit.SECONDS)
                        .build()
                ).state.observeForever { println(it) }
            } else {
                work.cancelUniqueWork("updateChecks")
                work.pruneWork()
            }
        }
    }

}