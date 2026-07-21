package com.programmersbox.koogintegration.agentresponse

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("The generic response type for the agent.")
sealed class AgentResponse {

    @Serializable
    @LLMDescription("The response type for the agent when it returns a string. This is the default response type.")
    data class Text(
        @property:LLMDescription("The text returned by the agent.")
        val text: String,
    ) : AgentResponse()

}