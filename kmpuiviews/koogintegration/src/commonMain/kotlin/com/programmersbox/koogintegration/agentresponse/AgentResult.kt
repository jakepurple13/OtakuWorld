package com.programmersbox.koogintegration.agentresponse

import androidx.compose.runtime.Stable
import com.mikepenz.markdown.model.State

sealed interface AgentResult {
    @Stable
    data class Text(val state: State) : AgentResult

    @Stable
    data class Recommendation(
        val state: State,
        val recommendations: List<com.programmersbox.koogintegration.agentresponse.Recommendation>,
    ) : AgentResult

    @Stable
    data class GeneratedList(val list: GeneratedCustomListResponse) : AgentResult

    @Stable
    data class CustomList(val state: State) : AgentResult
}
