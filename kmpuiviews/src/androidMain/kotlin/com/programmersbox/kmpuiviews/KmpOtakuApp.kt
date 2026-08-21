package com.programmersbox.kmpuiviews

import android.app.Application
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.StrictMode
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.datastore.PlatformDataStoreHandling
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpuiviews.di.kmpModule
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.utils.AndroidLogger
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationGroups
import com.programmersbox.kmpuiviews.utils.OtakuLogger
import com.programmersbox.kmpuiviews.utils.printLogs
import com.programmersbox.kmpuiviews.widget.notification.NotificationWidget
import com.programmersbox.supabaseintegration.repository.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flowOn
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

abstract class KmpOtakuApp : Application(), Configuration.Provider {
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeRuntimeApi::class, ExperimentalFoundationApi::class)
    @CallSuper
    override fun onCreate() {
        super.onCreate()
        //If firebase is giving issues, comment these lines out
        //ComposeRuntimeFlags.isLinkBufferComposerEnabled = true
        ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled = true
        Composer.setDiagnosticStackTraceMode(if (isDebug) ComposeStackTraceMode.SourceInformation else ComposeStackTraceMode.None)

        //TODO: Create an abstract class for KMPOtakuApp that handles some of this stuff
        DataStoreSettings { filesDir.resolve(it).absolutePath }

        OtakuLogger.shouldPrintLogs = isDebug
        OtakuLogger.init(AndroidLogger())

        if (isDebug) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        NotificationChannels.setupNotificationChannels(this)
        NotificationGroups.setupNotificationGroups(this)

        koinSetup()

        GlobalScope.launch(Dispatchers.IO) { get<SourceLoader>().blockingLoad() }

        forLaterSetup()

        runCatching {
            val backgroundWorkHandler = get<BackgroundWorkHandler>()
            backgroundWorkHandler.setupPeriodicCheckers()
        }

        shortcutSetup()

        val activityRepository = get<ActivityRepository>()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                GlobalScope.launch(Dispatchers.IO) { activityRepository.onActivityStop() }
            }
        })

        val platformDataStoreHandling = get<PlatformDataStoreHandling>()
        val itemDao = get<ItemDao>()

        GlobalScope.launch(Dispatchers.IO) {
            platformDataStoreHandling.hasWidget
                .asFlow()
                .onEach {
                    if (it) {
                        NotificationWidget().updateAll(this@KmpOtakuApp)
                        itemDao
                            .getTotalCountFlow()
                            .flowOn(Dispatchers.IO)
                            .collect { NotificationWidget().updateAll(this@KmpOtakuApp) }
                    }
                }
                .launchIn(this)
        }
    }

    abstract val isDebug: Boolean

    abstract fun Module.models()

    @StringRes
    protected abstract fun getForLaterString(): Int

    private fun koinSetup() {
        startKoin {
            androidLogger(if (isDebug) Level.DEBUG else Level.INFO)
            androidContext(this@KmpOtakuApp)
            workManagerFactory()
            loadKoinModules(
                module {
                    includes(buildModules, kmpModule)
                    models()
                }
            )
        }
    }

    private fun forLaterSetup() {
        GlobalScope.launch(Dispatchers.IO) {
            val forLaterName = getString(getForLaterString())
            val forLaterUUID = UUID.nameUUIDFromBytes(forLaterName.toByteArray())
                .toString()
                .also { AppConfig.forLaterUuid = it }

            runCatching {
                get<ListRepository>().createList(
                    CustomListItem(
                        uuid = forLaterUUID,
                        name = forLaterName,
                    )
                )
            }
                .onSuccess { printLogs { "For later list id: $it" } }
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

    abstract val buildModules: Module

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
}