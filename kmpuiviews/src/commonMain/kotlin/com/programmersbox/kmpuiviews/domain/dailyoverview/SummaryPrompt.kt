package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import kotlinx.serialization.json.Json

object SummaryPrompt {
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

    fun user(facts: DailyFacts): String = Json.encodeToString(DailyFacts.serializer(), facts)
}
