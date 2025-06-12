@file:OptIn(DelicateCoroutinesApi::class)

package com.programmersbox.uiviews

import android.app.Application
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.util.Log
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.configUpdates
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.di.kmpModule
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.loggingutils.Loged
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.datastore.OtakuDataStoreHandling
import com.programmersbox.uiviews.datastore.RemoteConfigKeys
import com.programmersbox.uiviews.datastore.SettingsHandling
import com.programmersbox.uiviews.datastore.migrateSettings
import com.programmersbox.uiviews.di.androidViewModels
import com.programmersbox.uiviews.di.appModules
import com.programmersbox.uiviews.di.kmpInterop
import com.programmersbox.uiviews.di.workers
import com.programmersbox.uiviews.utils.NotificationChannels
import com.programmersbox.uiviews.utils.NotificationGroups
import com.programmersbox.uiviews.utils.logFirebaseMessage
import com.programmersbox.uiviews.utils.recordFirebaseException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.UUID

abstract class OtakuApp : Application(), Configuration.Provider {
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeRuntimeApi::class)
    @CallSuper
    override fun onCreate() {
        super.onCreate()
        //If firebase is giving issues, comment these lines out
        ComposeUiFlags.isSemanticAutofillEnabled = true
        Composer.setDiagnosticStackTraceEnabled(BuildConfig.DEBUG)

        DataStoreSettings { filesDir.resolve(it).absolutePath }

        // This acts funky if user enabled force dark mode from developer options
        DynamicColors.applyToActivitiesIfAvailable(this)

        //GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        Loged.FILTER_BY_PACKAGE_NAME = "programmersbox"
        Loged.TAG = this::class.java.simpleName

        firebaseSetup()

        NotificationChannels.setupNotificationChannels(this)
        NotificationGroups.setupNotificationGroups(this)

        koinSetup()

        onCreated()

        get<SourceLoader>().load()

        val dataStoreHandling = get<DataStoreHandling>()
        val otakuDataStoreHandling = get<OtakuDataStoreHandling>()
        val newSettingsHandling = get<NewSettingsHandling>()
        val settingsHandling = get<SettingsHandling>()

        //TODO: Remove the migration after the next full release
        migrateSettings(
            context = this,
            dataStoreHandling = dataStoreHandling,
            settingsHandling = settingsHandling,
            newSettingsHandling = newSettingsHandling
        )

        forLaterSetup()

        runCatching {
            val backgroundWorkHandler = get<BackgroundWorkHandler>()
            backgroundWorkHandler.setupPeriodicCheckers()
        }

        shortcutSetup()

        runCatching {
            val appConfig = get<AppConfig>()
            if (appConfig.buildType != BuildType.NoFirebase) {
                remoteConfigSetup(
                    dataStoreHandling = dataStoreHandling,
                    otakuDataStoreHandling = otakuDataStoreHandling,
                    settingsHandling = settingsHandling,
                    newSettingsHandling = newSettingsHandling
                )
            }
        }
    }

    private fun koinSetup() {
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.INFO)
            androidContext(this@OtakuApp)
            workManagerFactory()
            loadKoinModules(
                module {
                    includes(
                        buildModules,
                        appModules,
                        androidViewModels,
                        kmpInterop,
                        kmpModule
                    )
                    workers()
                }
            )
        }
    }

    private fun firebaseSetup() {
        runCatching {
            FirebaseApp.initializeApp(this)
            Firebase.crashlytics.setCustomKeys {
                key("buildType", BuildConfig.BUILD_TYPE)
                key("buildFlavor", BuildConfig.FLAVOR)
            }
            Firebase.analytics.setUserProperty("buildType", BuildConfig.BUILD_TYPE)
            Firebase.analytics.setUserProperty("buildFlavor", BuildConfig.FLAVOR)
        }

        createFirebaseIds().let {
            FirebaseDb.DOCUMENT_ID = it.documentId
            FirebaseDb.CHAPTERS_ID = it.chaptersId
            FirebaseDb.COLLECTION_ID = it.collectionId
            FirebaseDb.ITEM_ID = it.itemId
            FirebaseDb.READ_OR_WATCHED_ID = it.readOrWatchedId
        }
    }

    private fun forLaterSetup() {
        GlobalScope.launch(Dispatchers.IO) {
            val forLaterName = getString(R.string.for_later)
            val forLaterUUID = UUID.nameUUIDFromBytes(forLaterName.toByteArray())
                .toString()
                .also { AppConfig.forLaterUuid = it }

            runCatching {
                get<ListDao>().createList(
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
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setWorkerExecutionExceptionHandler { it.throwable.printStackTrace() }
            .build()

    open fun onCreated() {}

    abstract val buildModules: Module
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

    private fun remoteConfigSetup(
        dataStoreHandling: DataStoreHandling,
        otakuDataStoreHandling: OtakuDataStoreHandling,
        settingsHandling: SettingsHandling,
        newSettingsHandling: NewSettingsHandling,
    ) {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            //Official docs say to only have this set for debug builds
            if (BuildConfig.DEBUG) minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                GlobalScope.launch {
                    RemoteConfigKeys.entries.forEach {
                        it.setDataStoreValue(
                            dataStoreHandling = dataStoreHandling,
                            otakuDataStoreHandling = otakuDataStoreHandling,
                            settingsHandling = settingsHandling,
                            newSettingsHandling = newSettingsHandling,
                            remoteConfig = remoteConfig,
                        )
                    }
                }
            }
        }

        //Updates
        remoteConfig
            .configUpdates
            .onEach { configUpdate ->
                remoteConfig.activate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        GlobalScope.launch {
                            val dataStoreKeys = RemoteConfigKeys.entries
                            configUpdate.updatedKeys.forEach { t ->
                                logFirebaseMessage("Updated key: $t")
                                runCatching {
                                    dataStoreKeys.first { keys -> keys.key == t }
                                }
                                    .onSuccess {
                                        it.setDataStoreValue(
                                            dataStoreHandling = dataStoreHandling,
                                            otakuDataStoreHandling = otakuDataStoreHandling,
                                            settingsHandling = settingsHandling,
                                            newSettingsHandling = newSettingsHandling,
                                            remoteConfig = remoteConfig
                                        )
                                    }
                                    .onFailure { it.printStackTrace() }
                            }
                        }
                    } else {
                        task.exception?.let(::recordFirebaseException)
                    }
                }
            }
            .launchIn(GlobalScope)
    }

    data class FirebaseIds(
        val documentId: String,
        val chaptersId: String,
        val collectionId: String,
        val itemId: String,
        val readOrWatchedId: String,
    )
}