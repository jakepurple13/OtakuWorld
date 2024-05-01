package com.programmersbox.uiviews

import android.app.Application
import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.ktx.Firebase
import com.programmersbox.extensionloader.SourceLoader
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.helpfulutils.NotificationChannelImportance
import com.programmersbox.helpfulutils.createNotificationChannel
import com.programmersbox.helpfulutils.createNotificationGroup
import com.programmersbox.loggingutils.Loged
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.sharedutils.FirebaseUIStyle
import com.programmersbox.uiviews.checkers.AppCheckWorker
import com.programmersbox.uiviews.checkers.SourceUpdateChecker
import com.programmersbox.uiviews.checkers.UpdateFlowWorker
import com.programmersbox.uiviews.utils.SettingsHandling
import com.programmersbox.uiviews.utils.shouldCheckFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.util.Locale
import java.util.concurrent.TimeUnit

abstract class OtakuApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        Firebase.crashlytics.setCustomKeys {
            key("buildType", BuildConfig.BUILD_TYPE)
            key("buildFlavor", BuildConfig.FLAVOR)
        }
        Firebase.analytics.setUserProperty("buildType", BuildConfig.BUILD_TYPE)
        Firebase.analytics.setUserProperty("buildFlavor", BuildConfig.FLAVOR)

        // This acts funky if user enabled force dark mode from developer options
        DynamicColors.applyToActivitiesIfAvailable(this)

        //GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        Loged.FILTER_BY_PACKAGE_NAME = "programmersbox"
        Loged.TAG = this::class.java.simpleName

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("otakuChannel", importance = NotificationChannelImportance.HIGH)
            createNotificationGroup("otakuGroup")
            createNotificationChannel("updateCheckChannel", importance = NotificationChannelImportance.MIN)
            createNotificationChannel("appUpdate", importance = NotificationChannelImportance.HIGH)
            createNotificationChannel("sourceUpdate", importance = NotificationChannelImportance.DEFAULT)
            createNotificationGroup("sources")
        }

        startKoin {
            androidLogger()
            androidContext(this@OtakuApp)
            loadKoinModules(
                module {
                    single { FirebaseUIStyle(R.style.Theme_OtakuWorldBase) }
                    single { SettingsHandling(get()) }
                    single { AppLogo(applicationInfo.loadIcon(packageManager), applicationInfo.icon) }
                }
            )
        }

        onCreated()

        loadKoinModules(
            module {
                single { SourceRepository() }
                single { CurrentSourceRepository() }
                single { ChangingSettingsRepository() }
                single { ItemDatabase.getInstance(get()) }
                single { HistoryDatabase.getInstance(get()) }
                single { ListDatabase.getInstance(get()) }
                single { SourceLoader(this@OtakuApp, get(), get<GenericInfo>().sourceType, get()) }
                single {
                    OtakuWorldCatalog(
                        get<GenericInfo>().sourceType
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    )
                }
            }
        )

        get<SourceLoader>().load()

        runCatching {
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

            work.enqueueUniquePeriodicWork(
                "sourceChecks",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(SourceUpdateChecker::class.java, 1, TimeUnit.DAYS)
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
        }

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
            updateSetupNow(context, runBlocking { context.shouldCheckFlow.first() })
        }

        fun updateSetupNow(context: Context, check: Boolean) {
            val work = WorkManager.getInstance(context)
            work.cancelUniqueWork("updateChecks")
            //work.cancelAllWork()
            //if (context.shouldCheck) {
            if (check) {
                work.enqueueUniquePeriodicWork(
                    "updateFlowChecks",
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<UpdateFlowWorker>(
                        1, TimeUnit.HOURS,
                        5, TimeUnit.MINUTES
                    )
                        .setInputData(
                            Data.Builder()
                                .putAll(
                                    mapOf(UpdateFlowWorker.CHECK_ALL to false)
                                )
                                .build()
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
                work.cancelUniqueWork("updateFlowChecks")
                work.pruneWork()
            }
        }
    }
}