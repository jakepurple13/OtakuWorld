package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.kmpuiviews.testing.FakeAnalytics
import com.programmersbox.kmpuiviews.testing.FakeApiService
import com.programmersbox.kmpuiviews.testing.FakeCrashReporter
import com.programmersbox.kmpuiviews.testing.InMemoryAiSettings
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import com.programmersbox.kmpuiviews.testing.createTestNewSettingsHandling
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.CrashReporter
import com.programmersbox.telemetry.NoOpAnalytics
import com.programmersbox.telemetry.NoOpCrashReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelemetryControllerTest {
    private lateinit var database: ItemDatabase
    private lateinit var dataStore: DataStoreHandling
    private lateinit var sourceRepository: SourceRepository
    private val currentSource = CurrentSourceRepository()
    private val analytics = FakeAnalytics()
    private val crash = FakeCrashReporter()

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
        dataStore = createTestDataStoreHandling()
        sourceRepository = SourceRepository()
        sourceRepository.addSources(listOf(KmpSourceInformation(FakeApiService("Src"), "Src", null, "pkg.src")))
    }

    @AfterTest
    fun tearDown() = database.close()

    private fun TestScope.controller(
        analytics: Analytics = this@TelemetryControllerTest.analytics,
        crashReporter: CrashReporter = crash,
    ) = TelemetryController(
        analytics = analytics,
        crashReporter = crashReporter,
        dataStore = dataStore,
        newSettings = createTestNewSettingsHandling(),
        aiSettings = InMemoryAiSettings(),
        sourceRepository = sourceRepository,
        dao = database.itemDao(),
        currentSource = currentSource,
        namer = TelemetrySourceNamer(sourceRepository, database.itemDao()),
        appConfig = AppConfig("test", BuildType.NoFirebase, isDebug = true),
    ).also { it.start(backgroundScope) }

    // DataStore and Room emit outside runTest's virtual clock
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(10_000) { while (!condition()) delay(10) }
        }
    }

    @Test
    fun `switches drive setEnabled`() = runTest {
        analytics.collecting = false
        crash.collecting = false
        controller()
        awaitCondition { analytics.collecting && crash.collecting }

        dataStore.shareUsageAnalytics.set(false)
        dataStore.sendCrashReports.set(false)
        awaitCondition { !analytics.collecting && !crash.collecting }

        dataStore.shareUsageAnalytics.set(true)
        awaitCondition { analytics.collecting }
        assertTrue(analytics.collecting)
    }

    @Test
    fun `user properties are bucketed`() = runTest {
        repeat(7) { database.itemDao().insertFavorite(DbModel("t$it", "", "u$it", "", "Src", 0)) }
        controller()
        awaitCondition { analytics.userProperties["favorites_count"] == "6-20" }

        assertEquals("test", analytics.userProperties["app_name"])
        assertEquals("automatic", analytics.userProperties["ai_engine"])
        assertEquals("1-5", analytics.userProperties["extension_count"])
        assertEquals("on", analytics.userProperties["floating_nav"])
        awaitCondition { analytics.userProperties["theme_mode"] != null }
    }

    @Test
    fun `current source key uses the namer`() = runTest {
        database.itemDao().insertIncognitoSource(IncognitoSource("pkg.src", "Src", isIncognito = true))
        currentSource.emit(sourceRepository.list.single().apiService)
        controller()

        awaitCondition { crash.keys["current_source"] == "incognito" }
        assertEquals("true", crash.keys["incognito"])
    }

    @Test
    fun `no current source sends none`() = runTest {
        controller()
        awaitCondition { crash.keys["current_source"] == "none" }
        assertEquals("false", crash.keys["incognito"])
    }

    @Test
    fun `runs with no-op telemetry`() = runTest {
        controller(analytics = NoOpAnalytics, crashReporter = NoOpCrashReporter)
        awaitCondition { true }
    }
}
