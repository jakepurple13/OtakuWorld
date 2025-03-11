@file:OptIn(DelicateCoroutinesApi::class)

package com.programmersbox.uiviews

import android.app.Application
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
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.programmersbox.extensionloader.SourceLoader
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.helpfulutils.NotificationChannelImportance
import com.programmersbox.helpfulutils.createNotificationChannel
import com.programmersbox.helpfulutils.createNotificationGroup
import com.programmersbox.loggingutils.Loged
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.FirebaseUIStyle
import com.programmersbox.uiviews.checkers.AppCheckWorker
import com.programmersbox.uiviews.checkers.NotifySingleWorker
import com.programmersbox.uiviews.checkers.SourceUpdateChecker
import com.programmersbox.uiviews.checkers.UpdateFlowWorker
import com.programmersbox.uiviews.checkers.UpdateNotification
import com.programmersbox.uiviews.datastore.DataStoreHandling
import com.programmersbox.uiviews.datastore.RemoteConfigKeys
import com.programmersbox.uiviews.datastore.SettingsHandling
import com.programmersbox.uiviews.di.databases
import com.programmersbox.uiviews.di.repository
import com.programmersbox.uiviews.di.viewModels
import com.programmersbox.uiviews.utils.DownloadAndInstaller
import com.programmersbox.uiviews.utils.PerformanceClass
import com.programmersbox.uiviews.utils.recordFirebaseException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
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
                    singleOf(::SettingsHandling)
                    single {
                        AppLogo(
                            logo = applicationInfo.loadIcon(packageManager),
                            logoId = applicationInfo.icon
                        )
                    }
                    single { UpdateNotification(get()) }
                    single { DataStoreHandling(get()) }
                    single { DownloadAndInstaller(get()) }
                    single { PerformanceClass.create() }
                    workerOf(::UpdateFlowWorker)
                    workerOf(::AppCheckWorker)
                    workerOf(::SourceUpdateChecker)
                    workerOf(::NotifySingleWorker)
                    viewModels()
                    databases()
                    repository()

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

        val dataStoreHandling = get<DataStoreHandling>()

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
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequest.Builder(
                    workerClass = AppCheckWorker::class.java,
                    repeatInterval = 7,
                    repeatIntervalTimeUnit = TimeUnit.DAYS
                )
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
                PeriodicWorkRequest.Builder(
                    workerClass = SourceUpdateChecker::class.java,
                    repeatInterval = 1,
                    repeatIntervalTimeUnit = TimeUnit.DAYS
                )
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

            //runBlocking { updateSetup(this@OtakuApp, dataStoreHandling.shouldCheck.get()) }
            setupCheckWorker(dataStoreHandling)
        }

        shortcutSetup()

        runCatching { if (BuildConfig.FLAVOR != "noFirebase") remoteConfigSetup(dataStoreHandling) }
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

    private fun remoteConfigSetup(dataStoreHandling: DataStoreHandling) {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            //Official docs say to only have this set for debug builds
            if (BuildConfig.DEBUG) minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        //Updates
        remoteConfig.addOnConfigUpdateListener(
            object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    remoteConfig.activate().addOnCompleteListener {
                        if (it.isSuccessful) {
                            GlobalScope.launch {
                                val dataStoreKeys = RemoteConfigKeys.entries
                                configUpdate.updatedKeys.forEach { t ->
                                    if (BuildConfig.DEBUG) println("Updated key: $t")
                                    runCatching {
                                        dataStoreKeys.first { keys -> keys.key == t }
                                    }
                                        .onSuccess {
                                            it.setDataStoreValue(
                                                dataStoreHandling = dataStoreHandling,
                                                remoteConfig = remoteConfig
                                            )
                                        }
                                        .onFailure { it.printStackTrace() }
                                }
                            }
                        }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    error.printStackTrace()
                    Firebase.crashlytics.recordException(error)
                }
            }
        )
    }

    data class FirebaseIds(
        val documentId: String,
        val chaptersId: String,
        val collectionId: String,
        val itemId: String,
        val readOrWatchedId: String,
    )

    private fun setupCheckWorker(dataStoreHandling: DataStoreHandling) {
        val work = WorkManager.getInstance(this)
        combine(
            dataStoreHandling
                .shouldCheck
                .asFlow()
                .distinctUntilChanged(),
            dataStoreHandling
                .updateHourCheck
                .asFlow()
                .distinctUntilChanged()
        ) { should, interval -> should to interval }
            .distinctUntilChanged()
            .onEach { check ->
                if (check.first) {
                    work.enqueueUniquePeriodicWork(
                        "updateFlowChecks",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        PeriodicWorkRequestBuilder<UpdateFlowWorker>(
                            check.second, TimeUnit.HOURS,
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
                    )
                } else {
                    work.cancelUniqueWork("updateFlowChecks")
                }
            }
            .launchIn(GlobalScope)

        /*dataStoreHandling
            .shouldCheck
            .asFlow()
            .distinctUntilChanged()
            .onEach { check ->
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
                    )
                } else {
                    work.cancelUniqueWork("updateFlowChecks")
                }
            }
            .launchIn(GlobalScope)*/
    }

    companion object {
        var forLaterUuid: UUID? = null
    }
}