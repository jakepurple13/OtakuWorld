package com.programmersbox.koogintegration.provider.otakutools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.programmersbox.koogintegration.integrator.KoogIntegrator

class LocalExplainTools(
    private val favoritesAnalyzer: KoogIntegrator,
) : ToolSet {

    @Tool
    @LLMDescription("Analyze favorites to see what the user is interested in")
    suspend fun explain() = """
        Analyze the user's favorites to see what they are interested in.
        
        ${favoritesAnalyzer.map()}
        
        ## Rules
        - Show a list of genres the user is interested in
        - Show the number of favorites for each genre
        - Show the top 3 genres with the most favorites
    """.trimIndent()

}