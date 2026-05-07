@file:OptIn(DelicateCoroutinesApi::class)

package com.programmersbox.uiviews

import android.util.Log
import androidx.annotation.CallSuper
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.configUpdates
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.KmpOtakuApp
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.datastore.OtakuDataStoreHandling
import com.programmersbox.uiviews.datastore.RemoteConfigKeys
import com.programmersbox.uiviews.di.androidViewModels
import com.programmersbox.uiviews.di.appModules
import com.programmersbox.uiviews.di.kmpInterop
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.core.module.Module

abstract class OtakuApp : KmpOtakuApp(), Configuration.Provider {
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeRuntimeApi::class, ExperimentalFoundationApi::class)
    @CallSuper
    override fun onCreate() {
        super.onCreate()
        // This acts funky if user enabled force dark mode from developer options
        DynamicColors.applyToActivitiesIfAvailable(this)

        firebaseSetup()

        val dataStoreHandling = get<DataStoreHandling>()
        val otakuDataStoreHandling = get<OtakuDataStoreHandling>()
        val newSettingsHandling = get<NewSettingsHandling>()

        runCatching {
            val appConfig = get<AppConfig>()
            if (appConfig.buildType != BuildType.NoFirebase) {
                remoteConfigSetup(
                    dataStoreHandling = dataStoreHandling,
                    otakuDataStoreHandling = otakuDataStoreHandling,
                    newSettingsHandling = newSettingsHandling
                )
            }
        }
    }

    override val isDebug: Boolean = BuildConfig.DEBUG

    override fun Module.models() {
        includes(
            appModules,
            androidViewModels,
            kmpInterop,
        )
    }

    override fun getForLaterString(): Int = R.string.for_later

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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setWorkerExecutionExceptionHandler { it.throwable.printStackTrace() }
            .build()

    abstract fun createFirebaseIds(): FirebaseIds

    private fun remoteConfigSetup(
        dataStoreHandling: DataStoreHandling,
        otakuDataStoreHandling: OtakuDataStoreHandling,
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