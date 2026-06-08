package com.programmersbox.koogintegration.agentresponse

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
data class AgentRecommendations(
    @property:LLMDescription("Text explaining the recommendations")
    val text: String,
    @property:LLMDescription("Recommendations for the user based on their query")
    val recommendations: List<Recommendation>,
) : AgentResponse()

@Serializable
@LLMDescription("A recommendation for the user based on their query")
data class Recommendation(
    @property:LLMDescription("The title of the recommendation")
    val title: String,
    @property:LLMDescription("A description of the recommendation")
    val description: String,
    @property:LLMDescription("A reason for the recommendation")
    val reason: String,
    @property:LLMDescription("A list of genres associated with the recommendation")
    val genre: List<String>,
)