package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.aitext.AiEnginePreference
import com.programmersbox.kmpuiviews.domain.aitext.AiToggle
import com.programmersbox.kmpuiviews.domain.aitext.DataStoreAiSettings
import com.programmersbox.kmpuiviews.testing.InMemoryAiSettings
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

    private fun withAi(ai: InMemoryAiSettings) = DailyOverviewSettings(handling, ai)

    @Test
    fun `master off forces numbers only`() = runTest {
        handling.dailyOverviewEngine.set("Provider")
        val prefs = withAi(InMemoryAiSettings(enabled = false)).prefs.first()
        assertEquals(SummaryEngineChoice.Provider, prefs.engine)
        assertEquals(SummaryEngineChoice.Template, prefs.effectiveEngine)
    }

    @Test
    fun `daily toggle off forces numbers only`() = runTest {
        val prefs = withAi(InMemoryAiSettings(disabled = setOf(AiToggle.DailyOverview))).prefs.first()
        assertEquals(SummaryEngineChoice.Template, prefs.effectiveEngine)
    }

    @Test
    fun `automatic follows ai preference`() = runTest {
        val prefs = withAi(InMemoryAiSettings(preference = AiEnginePreference.Gemma)).prefs.first()
        assertEquals(SummaryEngineChoice.Automatic, prefs.engine)
        assertEquals(SummaryEngineChoice.Gemma, prefs.effectiveEngine)
    }

    @Test
    fun `explicit choice kept`() = runTest {
        handling.dailyOverviewEngine.set("Ollama")
        val prefs = withAi(InMemoryAiSettings(preference = AiEnginePreference.Nano)).prefs.first()
        assertEquals(SummaryEngineChoice.Ollama, prefs.effectiveEngine)
    }

    @Test
    fun `chapter recap switch writes the reader key`() = runTest {
        val ai = DataStoreAiSettings(handling)
        ai.setFeatureEnabled(AiToggle.ChapterRecap, false)
        assertEquals(false, handling.novelChapterRecap.get())
        assertEquals(setOf(AiToggle.ChapterRecap), ai.disabled.first())
        ai.setFeatureEnabled(AiToggle.Tldr, false)
        assertEquals(setOf(AiToggle.ChapterRecap, AiToggle.Tldr), ai.disabled.first())
    }
}
