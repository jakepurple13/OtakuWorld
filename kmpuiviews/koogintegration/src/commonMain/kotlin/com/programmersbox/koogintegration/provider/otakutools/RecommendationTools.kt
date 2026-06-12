package com.programmersbox.koogintegration.provider.otakutools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

class RecommendationTools : ToolSet {
    @Tool
    @LLMDescription("Get recommendations for the user based on their query")
    fun getRecommendations(
        @LLMDescription("The user's query")
        query: String,
        @LLMDescription("Some genres the user might be interested in. Default is empty list.")
        genres: List<String> = emptyList(),
    ): String = """
        Get some recommendations for the user based on their query: $query
        
        The genres the user might be interested in are: ${genres.joinToString()}
        
        ## Rules
        - Recommendations should be based on the user's query
        - Show a minimum of 3 recommendations and a maximum of 5
    """.trimIndent()
}