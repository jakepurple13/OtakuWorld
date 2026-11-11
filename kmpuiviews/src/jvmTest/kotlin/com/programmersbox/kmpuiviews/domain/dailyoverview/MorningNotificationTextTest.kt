package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MorningNotificationTextTest {
    private fun update(n: Int) = UpdateItem("U$n", "u$n", null, "S", "")

    private fun facts(unit: UpdateUnit, read: Int, updates: Int) = DailyFacts(
        date = LocalDate(2026, 11, 10),
        unit = unit,
        readCount = read,
        titles = emptyList(),
        readsByHour = List(24) { 0 },
        updates = List(updates, ::update),
        failedSources = emptyList(),
    )

    @Test
    fun `title uses the unit noun and the update count`() = runTest {
        assertEquals("Yesterday: 23 chapters, 11 updates", morningNotificationTitle(facts(UpdateUnit.Chapters, 23, 11)))
        assertEquals("Yesterday: 1 episode, 1 update", morningNotificationTitle(facts(UpdateUnit.Episodes, 1, 1)))
    }

    @Test
    fun `first sentence stops at the first sentence end`() {
        assertEquals(
            "You finished Frieren and you're caught up.",
            firstSentence("You finished Frieren and you're caught up. A new One Piece chapter is out."),
        )
        assertEquals("Wow!", firstSentence("Wow! "))
        assertEquals("Read chapter 12.5 of Dandadan?", firstSentence("Read chapter 12.5 of Dandadan? Yes."))
    }

    @Test
    fun `first sentence without an end mark is the whole text`() {
        assertEquals("No full stop here", firstSentence("  No full stop here  "))
    }
}
