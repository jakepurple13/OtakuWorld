package com.programmersbox.koogintegration.provider

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import com.programmersbox.koogintegration.agentresponse.AgentResponse

sealed interface AgentProvider {
    val title: String
    val description: String
}

sealed interface AgentExecutionTraceEvent {
    val name: String

    data class Node(override val name: String) : AgentExecutionTraceEvent
    data class SubgraphStarted(override val name: String) : AgentExecutionTraceEvent
    data class SubgraphCompleted(override val name: String, val result: String? = null) : AgentExecutionTraceEvent
}

interface ChatAgentProvider : AgentProvider {
    suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
        onLLMCallCompleted: suspend (ResponseMetaInfo) -> Unit,
    ): AIAgent<String, AgentResponse>
}

interface TaskAgentProvider : AgentProvider {
    suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
        onAssistantMessage: suspend (String) -> String,
        onLLMCallCompleted: suspend (ResponseMetaInfo) -> Unit,
    ): AIAgent<String, AgentResponse>
}

interface SingleTaskAgentProvider : TaskAgentProvider