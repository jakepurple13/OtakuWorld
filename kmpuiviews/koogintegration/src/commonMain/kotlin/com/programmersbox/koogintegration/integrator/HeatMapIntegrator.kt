package com.programmersbox.koogintegration.integrator

import ai.koog.agents.core.tools.annotations.LLMDescription
import com.programmersbox.favoritesdatabase.HeatMapDao
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

class HeatMapIntegrator(
    private val heatMapDao: HeatMapDao,
) : KoogIntegrator<DateRange?>() {
    override suspend fun map(input: DateRange?): String {
        return buildString {
            appendLine("Day to number read or watched:")
            heatMapDao
                .getAllHeatMapsSync()
                .let { map ->
                    input
                        ?.let { range -> map.filter { it.time in range.start..range.end } }
                        ?: map
                }
                .forEach {
                    appendLine("${it.time} | ${it.count}")
                }
        }
    }
}

@Serializable
@LLMDescription("A range of dates")
data class DateRange(
    @property:LLMDescription("The start date of the range")
    val start: LocalDate,
    @property:LLMDescription("The end date of the range")
    val end: LocalDate,
)