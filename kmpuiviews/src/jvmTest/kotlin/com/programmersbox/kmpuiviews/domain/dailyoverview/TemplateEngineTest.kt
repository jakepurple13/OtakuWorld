package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateEngineTest {
    private val engine = TemplateEngine()

    private fun title(n: Int) = TitleRead("T$n", "u$n", "", "S", "1", "2", 1, false)
    private fun update(n: Int) = UpdateItem("U$n", "u$n", null, "S", "")

    private fun facts(
        unit: UpdateUnit = UpdateUnit.Chapters,
        read: Int = 0,
        titles: Int = 0,
        updates: Int = 0,
        failed: Int = 0,
    ) = DailyFacts(
        date = LocalDate(2026, 11, 10),
        unit = unit,
        readCount = read,
        titles = List(titles, ::title),
        readsByHour = List(24) { 0 },
        updates = List(updates, ::update),
        failedSources = List(failed) { "Src$it" },
    )

    private suspend fun text(f: DailyFacts) = engine.summarize(f).first()

    @Test
    fun `always ready and on device`() = runTest {
        assertEquals(EngineStatus.Ready, engine.status())
        assertTrue(engine.onDevice)
        assertEquals(SummaryEngineId.Template, engine.id)
    }

    @Test
    fun `full day`() = runTest {
        assertEquals(
            "You read 23 chapters across 4 titles. 11 new chapters arrived. 1 source failed its update check.",
            text(facts(read = 23, titles = 4, updates = 11, failed = 1)),
        )
    }

    @Test
    fun `episodes unit`() = runTest {
        assertEquals(
            "You read 23 episodes across 4 titles. 11 new episodes arrived.",
            text(facts(UpdateUnit.Episodes, read = 23, titles = 4, updates = 11)),
        )
    }

    @Test
    fun `quiet day`() = runTest {
        assertEquals("Nothing new yesterday.", text(facts()))
    }

    @Test
    fun `singular forms`() = runTest {
        assertEquals(
            "You read 1 chapter across 1 title. 1 new chapter arrived. 1 source failed its update check.",
            text(facts(read = 1, titles = 1, updates = 1, failed = 1)),
        )
        assertEquals("1 new episode arrived.", text(facts(UpdateUnit.Episodes, updates = 1)))
    }

    @Test
    fun `plural failures`() = runTest {
        assertEquals(
            "1 new chapter arrived. 2 sources failed their update checks.",
            text(facts(updates = 1, failed = 2)),
        )
    }

    @Test
    fun `reads without titles omit the titles clause`() = runTest {
        assertEquals("You read 5 chapters.", text(facts(read = 5)))
    }

    @Test
    fun `only updates`() = runTest {
        assertEquals("3 new chapters arrived.", text(facts(updates = 3)))
    }
}
