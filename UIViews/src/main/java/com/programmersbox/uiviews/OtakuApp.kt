package com.programmersbox.uiviews

import android.app.Application
import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import androidx.annotation.CallSuper
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.ktx.Firebase
import com.programmersbox.extensionloader.SourceLoader
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.helpfulutils.NotificationChannelImportance
import com.programmersbox.helpfulutils.createNotificationChannel
import com.programmersbox.helpfulutils.createNotificationGroup
import com.programmersbox.loggingutils.Loged
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.FirebaseUIStyle
import com.programmersbox.uiviews.checkers.AppCheckWorker
import com.programmersbox.uiviews.checkers.SourceUpdateChecker
import com.programmersbox.uiviews.checkers.UpdateFlowWorker
import com.programmersbox.uiviews.checkers.UpdateNotification
import com.programmersbox.uiviews.di.viewModels
import com.programmersbox.uiviews.utils.SettingsHandling
import com.programmersbox.uiviews.utils.blurhash.BlurHashDatabase
import com.programmersbox.uiviews.utils.components.DataStoreHandling
import com.programmersbox.uiviews.utils.recordFirebaseException
import com.programmersbox.uiviews.utils.shouldCheckFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

abstract class OtakuApp : Application(), Configuration.Provider {

    @OptIn(ExperimentalComposeUiApi::class)
    @CallSuper
    override fun onCreate() {
        super.onCreate()
        //If firebase is giving issues, comment these lines out
        ComposeUiFlags.isSemanticAutofillEnabled = true

        runCatching {
            FirebaseApp.initializeApp(this)

            Firebase.crashlytics.setCustomKeys {
                key("buildType", BuildConfig.BUILD_TYPE)
                key("buildFlavor", BuildConfig.FLAVOR)
            }
            Firebase.analytics.setUserProperty("buildType", BuildConfig.BUILD_TYPE)
            Firebase.analytics.setUserProperty("buildFlavor", BuildConfig.FLAVOR)
        }

        // This acts funky if user enabled force dark mode from developer options
        DynamicColors.applyToActivitiesIfAvailable(this)

        //GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        Loged.FILTER_BY_PACKAGE_NAME = "programmersbox"
        Loged.TAG = this::class.java.simpleName

        createFirebaseIds().let {
            FirebaseDb.DOCUMENT_ID = it.documentId
            FirebaseDb.CHAPTERS_ID = it.chaptersId
            FirebaseDb.COLLECTION_ID = it.collectionId
            FirebaseDb.ITEM_ID = it.itemId
            FirebaseDb.READ_OR_WATCHED_ID = it.readOrWatchedId
        }

        createNotificationChannel("otakuChannel", importance = NotificationChannelImportance.HIGH)
        createNotificationGroup("otakuGroup")
        createNotificationChannel("updateCheckChannel", importance = NotificationChannelImportance.MIN)
        createNotificationChannel("appUpdate", importance = NotificationChannelImportance.HIGH)
        createNotificationChannel("sourceUpdate", importance = NotificationChannelImportance.DEFAULT)
        createNotificationGroup("sources")

        startKoin {
            androidLogger()
            androidContext(this@OtakuApp)
            loadKoinModules(
                module {
                    buildModules()
                    single { FirebaseUIStyle(R.style.Theme_OtakuWorldBase) }
                    single { SettingsHandling(get()) }
                    single {
                        AppLogo(
                            logo = applicationInfo.loadIcon(packageManager),
                            logoId = applicationInfo.icon
                        )
                    }
                    single { UpdateNotification(get()) }
                    single { DataStoreHandling(get()) }
                    workerOf(::UpdateFlowWorker)
                    workerOf(::AppCheckWorker)
                    workerOf(::SourceUpdateChecker)
                    viewModels()

                    single { SourceRepository() }
                    single { CurrentSourceRepository() }
                    single { ChangingSettingsRepository() }
                    single { ItemDatabase.getInstance(get()) }
                    single { BlurHashDatabase.getInstance(get()) }
                    single { HistoryDatabase.getInstance(get()) }
                    single { ListDatabase.getInstance(get()) }
                    single { get<ListDatabase>().listDao() }
                    single { get<ItemDatabase>().itemDao() }
                    single { get<BlurHashDatabase>().blurDao() }
                    single { get<HistoryDatabase>().historyDao() }
                    single { SourceLoader(this@OtakuApp, get(), get<GenericInfo>().sourceType, get()) }
                    single {
                        OtakuWorldCatalog(
                            get<GenericInfo>().sourceType
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        )
                    }
                }
            )
            workManagerFactory()
        }

        onCreated()

        get<SourceLoader>().load()

        GlobalScope.launch(Dispatchers.IO) {
            val forLaterName = getString(R.string.for_later)
            val forLaterUUID = UUID.nameUUIDFromBytes(forLaterName.toByteArray()).also { forLaterUuid = it }
            runCatching {
                get<ListDatabase>()
                    .listDao()
                    .createList(
                        CustomListItem(
                            uuid = forLaterUUID,
                            name = forLaterName,
                        )
                    )
            }
                .onSuccess { println("For later list id: $it") }
                .onFailure {
                    recordFirebaseException(it)
                    it.printStackTrace()
                }
        }

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

        shortcutSetup()

    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    open fun onCreated() {}

    abstract fun Module.buildModules()
    abstract fun createFirebaseIds(): FirebaseIds

    protected open fun shortcuts(): List<ShortcutInfo> = emptyList()

    private fun shortcutSetup() {
        val manager = getSystemService(ShortcutManager::class.java)
        if (manager.dynamicShortcuts.isEmpty()) {
            // Application restored. Need to re-publish dynamic shortcuts.
            if (manager.pinnedShortcuts.isNotEmpty()) {
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

    data class FirebaseIds(
        val documentId: String,
        val chaptersId: String,
        val collectionId: String,
        val itemId: String,
        val readOrWatchedId: String,
    )

    companion object {

        var forLaterUuid: UUID? = null

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
                        .setInputData(workDataOf(UpdateFlowWorker.CHECK_ALL to false))
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