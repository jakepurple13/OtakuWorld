package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SummaryPrompt {
    /** Update titles named in the prompt; [PromptFacts.updateCount] keeps the full count. */
    internal const val MAX_UPDATES = 10

    fun system(unit: UpdateUnit): String {
        val noun = when (unit) {
            UpdateUnit.Chapters -> "chapters"
            UpdateUnit.Episodes -> "episodes"
        }
        return "You write a short recap of someone's reading yesterday. " +
                "Write 2-3 sentences in the second person, in plain words. " +
                "Use only numbers that appear in the JSON facts you are given, and never invent or calculate new ones. " +
                "Call each unit \"$noun\". " +
                "Do not mention anything that is not in the facts."
    }

    /** Only what the recap needs: no URLs, image URLs, source ids or timestamps leave the device. */
    fun user(facts: DailyFacts): String = Json.encodeToString(PromptFacts.serializer(), facts.toPrompt())

    @Serializable
    private data class PromptTitle(
        val title: String,
        val firstChapter: String,
        val lastChapter: String,
        val count: Int,
        val caughtUp: Boolean,
    )

    @Serializable
    private data class PromptFacts(
        val date: LocalDate,
        val unit: UpdateUnit,
        val readCount: Int,
        val titles: List<PromptTitle>,
        val updates: List<String>,
        val updateCount: Int,
        val failedSourceCount: Int,
    )

    private fun DailyFacts.toPrompt() = PromptFacts(
        date = date,
        unit = unit,
        readCount = readCount,
        titles = titles.map { PromptTitle(it.title, it.firstChapter, it.lastChapter, it.count, it.caughtUp) },
        updates = updates.take(MAX_UPDATES).map { it.title },
        updateCount = updates.size,
        failedSourceCount = failedSources.size,
    )
}
