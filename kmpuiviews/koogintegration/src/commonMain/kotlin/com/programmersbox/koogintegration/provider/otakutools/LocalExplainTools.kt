package com.programmersbox.koogintegration.provider.otakutools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.programmersbox.koogintegration.integrator.DateRange
import com.programmersbox.koogintegration.integrator.KoogIntegrator

class LocalExplainTools(
    private val favoritesAnalyzer: KoogIntegrator<String>,
    private val heatmapAnalyzer: KoogIntegrator<DateRange?>,
    private val bookmarksAnalyzer: KoogIntegrator<String>,
    private val listAnalyzer: KoogIntegrator<String>,
) : ToolSet {

    @Tool
    @LLMDescription("Analyze favorites to see what the user is interested in")
    suspend fun explain() = """
        Analyze the user's favorites to see what they are interested in.
        
        ${favoritesAnalyzer.map("")}
        
        ## Rules
        - Show a list of genres the user is interested in
        - Show the number of favorites for each genre
        - Show the top 3 genres with the most favorites
    """.trimIndent()

    @Tool
    @LLMDescription("Analyze how often the user reads and on what days")
    suspend fun explainHeatmap(
        @LLMDescription("The date range to analyze. If the user does not specify a date range, the default is `null`.")
        dateRange: DateRange? = null,
    ) = """
        Analyze how often the user reads and on what days.
        
        ${heatmapAnalyzer.map(dateRange)}
        
        ## Rules
        - Show the number of times the user reads each day
        - Show the number of days the user reads
    """.trimIndent()

    @Tool
    @LLMDescription("Analyze the user's bookmarks")
    suspend fun explainBookmarks() = """
        Analyze the user's bookmarks.
        
        ${bookmarksAnalyzer.map("")}
        
        ## Rules
        - Show the number of bookmarks for each genre
        - Show the top 3 genres with the most bookmarks
        - Show the number of total bookmarks
        - Show the item with the most bookmarks
    """.trimIndent()

    @Tool
    @LLMDescription("Analyze the user's list")
    suspend fun explainList() = """
        Analyze the user's list.
        
        ${listAnalyzer.map("")}
        
        ## Rules
        - Show the number of items in the list
        - Show last updated time in a human readable format
        - Include anything else that could be useful to the user
    """.trimIndent()
}