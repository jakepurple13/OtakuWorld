package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SummaryPromptTest {
    private fun facts(updates: Int = 0) = DailyFacts(
        date = LocalDate(2026, 11, 10),
        unit = UpdateUnit.Chapters,
        readCount = 3,
        titles = listOf(
            TitleRead("A", "https://site/a", "https://img/a.png", "src-a", "1", "3", 3, false, lastReadAt = 1_700_000_000_000),
        ),
        readsByHour = List(24) { if (it == 9) 3 else 0 },
        updates = List(updates) { UpdateItem("U$it", "https://site/u$it", "https://img/u$it", "src-b", "Ch $it") },
        failedSources = listOf("src-c", "src-d"),
    )

    private fun prompt(f: DailyFacts) = Json.parseToJsonElement(SummaryPrompt.user(f)).jsonObject

    @Test
    fun `user prompt carries only the compact facts`() {
        val json = prompt(facts(updates = 2))
        assertEquals(
            setOf("date", "unit", "readCount", "titles", "updates", "updateCount", "failedSourceCount"),
            json.keys,
        )
        assertEquals("2026-11-10", json["date"]!!.jsonPrimitive.content)
        assertEquals(3, json["readCount"]!!.jsonPrimitive.int)
        assertEquals(2, json["failedSourceCount"]!!.jsonPrimitive.int)
        val title = json["titles"]!!.jsonArray.single().jsonObject
        assertEquals(setOf("title", "firstChapter", "lastChapter", "count", "caughtUp"), title.keys)
        assertEquals(listOf("U0", "U1"), json["updates"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun `user prompt has no urls, source ids or timestamps`() {
        val text = SummaryPrompt.user(facts(updates = 3))
        listOf("http", "src-", "1700000000000", "readsByHour").forEach {
            assertFalse(it in text, "prompt leaks $it: $text")
        }
    }

    @Test
    fun `updates are capped at ten titles with the total kept`() {
        val json = prompt(facts(updates = 25))
        assertEquals(10, json["updates"]!!.jsonArray.size)
        assertEquals(25, json["updateCount"]!!.jsonPrimitive.int)
    }

    @Test
    fun `system prompt names the unit`() {
        assertTrue("chapters" in SummaryPrompt.system(UpdateUnit.Chapters))
        assertTrue("episodes" in SummaryPrompt.system(UpdateUnit.Episodes))
    }
}
