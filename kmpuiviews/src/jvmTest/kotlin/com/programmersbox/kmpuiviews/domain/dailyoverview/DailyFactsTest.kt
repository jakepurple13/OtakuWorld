package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyFactsTest {
    private fun facts(
        readCount: Int = 0,
        titles: List<TitleRead> = emptyList(),
        updates: List<UpdateItem> = emptyList(),
    ) = DailyFacts(
        date = LocalDate(2026, 11, 10),
        unit = UpdateUnit.Chapters,
        readCount = readCount,
        titles = titles,
        readsByHour = List(24) { 0 },
        updates = updates,
        failedSources = emptyList(),
    )

    @Test
    fun nothingReadOrUpdatedIsQuiet() {
        assertTrue(facts().isQuiet)
    }

    @Test
    fun titlesReadWithZeroHeatMapCountIsNotQuiet() {
        val title = TitleRead("T", "u", "", "S", "1", "2", 2, false)
        assertFalse(facts(readCount = 0, titles = listOf(title)).isQuiet)
    }

    @Test
    fun updatesAloneIsNotQuiet() {
        assertFalse(facts(updates = listOf(UpdateItem("U", "u", null, "S", ""))).isQuiet)
    }

    @Test
    fun lastReadIsTheTitleWithTheLatestReadNotTheLowestCount() {
        val most = TitleRead("Most", "m", "", "S", "1", "13", 13, false, lastReadAt = 2_000)
        val late = TitleRead("Late", "l", "", "S", "1", "7", 7, false, lastReadAt = 3_000)
        val least = TitleRead("Least", "x", "", "S", "1", "2", 2, false, lastReadAt = 1_000)
        assertEquals("Late", facts(titles = listOf(most, late, least)).lastRead?.title)
    }
}
