package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyOverviewSettingsTest {
    private val handling = createTestDataStoreHandling()
    private val settings = DailyOverviewSettings(handling)

    @Test
    fun defaultsMatchConstraints() = runTest {
        assertEquals(
            DailyOverviewPrefs(
                enabled = true,
                notify = false,
                minuteOfDay = 480,
                skipQuiet = true,
                engine = SummaryEngineChoice.Automatic,
            ),
            settings.prefs.first()
        )
        assertNull(settings.cached())
    }

    @Test
    fun settersRoundTrip() = runTest {
        settings.setEnabled(false)
        settings.setNotify(true)
        settings.setMinuteOfDay(615)
        settings.setSkipQuiet(false)
        settings.setEngine(SummaryEngineChoice.Gemma)
        assertEquals(
            DailyOverviewPrefs(false, true, 615, false, SummaryEngineChoice.Gemma),
            settings.prefs.first()
        )
    }

    @Test
    fun unknownEngineReadsAsAutomatic() = runTest {
        handling.dailyOverviewEngine.set("Bogus")
        assertEquals(SummaryEngineChoice.Automatic, settings.prefs.first().engine)
    }

    @Test
    fun cacheRoundTrips() = runTest {
        val s = DailySummary(LocalDate(2026, 11, 10), SummaryEngineId.Template, "hello")
        settings.cache(s)
        assertEquals(s, settings.cached())
    }

    @Test
    fun corruptCacheReadsAsNull() = runTest {
        handling.dailyOverviewCache.set("{not json")
        assertNull(settings.cached())
    }
}
