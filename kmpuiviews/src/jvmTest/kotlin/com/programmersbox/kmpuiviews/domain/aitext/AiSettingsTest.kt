package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import kotlin.test.Test
import kotlin.test.assertEquals

class AiSettingsTest {
    private val android = listOf(SummaryEngineId.Nano, SummaryEngineId.Gemma)
    private val desktop = listOf(SummaryEngineId.Ollama)

    @Test
    fun `preference picks the order`() {
        assertEquals(android, AiEnginePreference.Automatic.order(android))
        assertEquals(listOf(SummaryEngineId.Nano), AiEnginePreference.Nano.order(android))
        assertEquals(listOf(SummaryEngineId.Gemma), AiEnginePreference.Gemma.order(android))
    }

    @Test
    fun `preference outside platform is ignored`() {
        assertEquals(emptyList(), AiEnginePreference.Nano.order(desktop))
        assertEquals(desktop, AiEnginePreference.Automatic.order(desktop))
    }

    @Test
    fun `chapter recap toggle uses the reader key`() {
        assertEquals(setOf(AiToggle.ChapterRecap), storedDisabledToggles(emptySet(), chapterRecap = false))
        assertEquals(emptySet(), storedDisabledToggles(emptySet(), chapterRecap = true))
        // A stray ChapterRecap in the stored set is ignored: the reader key wins
        assertEquals(emptySet(), storedDisabledToggles(setOf("ChapterRecap"), chapterRecap = true))
    }

    @Test
    fun `stored names map to toggles and unknown names are dropped`() {
        assertEquals(
            setOf(AiToggle.Tldr, AiToggle.DailyOverview),
            storedDisabledToggles(setOf("Tldr", "DailyOverview", "NoLongerExists"), chapterRecap = true),
        )
    }

    @Test
    fun `features map to their toggles`() {
        assertEquals(AiToggle.Tldr, AiFeature.Tldr.toggle)
        // The reader gates the recap bar with its own switch (the same setting), so the engine stays available
        kotlin.test.assertNull(AiFeature.Recap.toggle as AiToggle?)
        assertEquals(AiToggle.WritingTools, AiFeature.Proofread.toggle)
        assertEquals(AiToggle.WritingTools, AiFeature.Rewrite(RewriteStyle.Emojify).toggle)
        assertEquals(AiToggle.ListSuggestions, AiFeature.ListNames.toggle)
        assertEquals(AiToggle.ListSuggestions, AiFeature.ListDescription.toggle)
        assertEquals(AiToggle.ErrorExplanations, AiFeature.ExplainError.toggle)
        assertEquals(AiToggle.UpdateSummary, AiFeature.UpdateSummary.toggle)
        assertEquals(AiToggle.DescribeSearch, AiFeature.SearchKeywords.toggle)
        assertEquals(null, AiFeature.TrySample.toggle)
    }
}
