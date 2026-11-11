package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SummaryPromptTest {
    private fun facts() = DailyFacts(
        date = LocalDate(2026, 11, 10),
        unit = UpdateUnit.Chapters,
        readCount = 3,
        titles = listOf(TitleRead("A", "u", "", "S", "1", "3", 3, false)),
        readsByHour = List(24) { if (it == 9) 3 else 0 },
        updates = emptyList(),
        failedSources = emptyList(),
    )

    @Test
    fun `user prompt is the facts JSON`() {
        val f = facts()
        assertEquals(Json.encodeToString(DailyFacts.serializer(), f), SummaryPrompt.user(f))
    }

    @Test
    fun `system prompt names the unit and forbids numbers not in the facts`() {
        val chapters = SummaryPrompt.system(UpdateUnit.Chapters)
        val episodes = SummaryPrompt.system(UpdateUnit.Episodes)
        assertTrue("chapters" in chapters)
        assertTrue("episodes" in episodes)
        assertTrue("only numbers" in chapters.lowercase())
        assertTrue("2-3 sentences" in chapters)
        assertTrue("second person" in chapters)
    }
}
