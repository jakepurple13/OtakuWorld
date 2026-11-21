package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.aitext.AiSettings
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.Buckets
import com.programmersbox.telemetry.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Applies the telemetry switches and keeps user properties and crash keys current.
 * Start once per process: KmpOtakuApp on Android, the desktop app's first composition on JVM.
 */
class TelemetryController(
    private val analytics: Analytics,
    private val crashReporter: CrashReporter,
    private val dataStore: DataStoreHandling,
    private val newSettings: NewSettingsHandling,
    private val aiSettings: AiSettings,
    private val sourceRepository: SourceRepository,
    private val dao: ItemDao,
    private val currentSource: CurrentSourceRepository,
    private val namer: TelemetrySourceNamer,
    private val appConfig: AppConfig,
) {
    fun start(scope: CoroutineScope) {
        namer.start(scope)

        dataStore.shareUsageAnalytics.asFlow().collectDistinct(scope) { analytics.setEnabled(it) }
        dataStore.sendCrashReports.asFlow().collectDistinct(scope) { crashReporter.setEnabled(it) }

        runCatching { analytics.setUserProperty("app_name", appConfig.appName) }
        sourceRepository.sources.map { Buckets.count(it.size) }
            .collectDistinct(scope) { analytics.setUserProperty("extension_count", it) }
        dao.getAllFavoritesCount().map(Buckets::count)
            .collectDistinct(scope) { analytics.setUserProperty("favorites_count", it) }
        aiSettings.preference.map { it.name.lowercase() }
            .collectDistinct(scope) {
                analytics.setUserProperty("ai_engine", it)
                crashReporter.setKey("ai_engine", it)
            }
        newSettings.preferences.data.map { it.themeSetting.name.lowercase() }
            .collectDistinct(scope) { analytics.setUserProperty("theme_mode", it) }
        dataStore.floatingNavigation.asFlow().map { if (it) "on" else "off" }
            .collectDistinct(scope) { analytics.setUserProperty("floating_nav", it) }

        combine(currentSource.asFlow(), namer.changes) { service, _ -> service?.let { namer.name(it.serviceName) } ?: "none" }
            .collectDistinct(scope) {
                crashReporter.setKey("current_source", it)
                crashReporter.setKey("incognito", (it == TelemetrySourceNamer.INCOGNITO).toString())
            }
    }

    private fun <T> Flow<T>.collectDistinct(scope: CoroutineScope, block: (T) -> Unit) {
        distinctUntilChanged()
            .onEach { runCatching { block(it) } }
            .launchIn(scope)
    }
}
