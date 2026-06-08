package com.programmersbox.koogintegration.agentresponse

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("The response type for the agent when it uses the explainList tool.")
data class ListResponse(
    @property:LLMDescription("The text returned by the agent.")
    val text: String,
) : AgentResponse()