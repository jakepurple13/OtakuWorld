package com.programmersbox.koogintegration.screens.chatscreen

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import androidx.compose.runtime.Stable
import com.programmersbox.koogintegration.agentresponse.AgentResponse

data class ChatUiState(
    val title: String = "Agent Demo",
    val chatMessages: List<ChatMessage> = listOf(ChatMessage.SystemMessage("Hi, I'm an agent that can help you")),
    val debugView: DebugView = DebugView(),
    val inputText: String = "",
    val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isChatEnded: Boolean = false,
    val userResponseRequested: Boolean = false,
    val currentUserResponse: String? = null,
    val mermaidGraphString: String? = null,
    val hideEmptyState: Boolean = false,
)

enum class DebugOption(val title: String) {
    Tools("Tools"),
    LlmCalls("LLM Calls"),
    Nodes("Nodes"),
    Tasks("Tasks"),
    TokenUsage("Token Usage"),
}

data class DebugView(
    val enabled: Boolean = false,
    val options: Set<DebugOption> = DebugOption.entries.toSet() - DebugOption.Nodes,
) {
    fun shows(message: ChatMessage): Boolean = shows(message.type)

    fun shows(type: ChatMessageType): Boolean {
        if (type in alwaysVisible) return true
        if (!enabled) return false
        return when (type) {
            ChatMessageType.Error -> true
            ChatMessageType.ToolCall -> DebugOption.Tools in options
            ChatMessageType.LlmCall -> DebugOption.LlmCalls in options
            ChatMessageType.Node -> DebugOption.Nodes in options
            ChatMessageType.Task -> DebugOption.Tasks in options
            ChatMessageType.TokenUsage -> DebugOption.TokenUsage in options
            else -> true
        }
    }

    companion object {
        private val alwaysVisible = setOf(
            ChatMessageType.User,
            ChatMessageType.Agent,
            ChatMessageType.System,
            ChatMessageType.Result,
        )
    }
}

enum class ChatMessageType {
    User,
    Agent,
    System,
    Error,
    Result,
    ToolCall,
    LlmCall,
    Node,
    Task,
    TokenUsage,
}

// Define message types for the chat
sealed class ChatMessage {
    data class UserMessage(val text: String) : ChatMessage()
    data class AgentMessage(val response: AgentResponse) : ChatMessage()
    data class SystemMessage(val text: String) : ChatMessage()
    data class ErrorMessage(val text: String) : ChatMessage()
    data class ResultMessage(val response: AgentResponse) : ChatMessage()
    data class ToolCallMessage(val toolName: String, val args: Map<String, String>) : ChatMessage()
    data class LLMCallMessage(val data: LlmCallData) : ChatMessage()

    data class LLMTokenUsageMessage(
        val inputTokens: Int,
        val outputTokens: Int,
        val totalTokens: Int,
    ) : ChatMessage()

    data class ExecutionTraceMessage(val item: ExecutionTraceItem) : ChatMessage()
    data class MermaidGraphMessage(val mermaidGraphString: String) : ChatMessage()
}

val ChatMessage.type: ChatMessageType
    get() =
        when (this) {
            is ChatMessage.UserMessage -> ChatMessageType.User
            is ChatMessage.AgentMessage -> ChatMessageType.Agent
            is ChatMessage.SystemMessage -> ChatMessageType.System
            is ChatMessage.ErrorMessage -> ChatMessageType.Error
            is ChatMessage.ResultMessage -> ChatMessageType.Result
            is ChatMessage.ToolCallMessage -> ChatMessageType.ToolCall
            is ChatMessage.LLMCallMessage -> ChatMessageType.LlmCall
            is ChatMessage.ExecutionTraceMessage -> when (item) {
                is ExecutionTraceItem.Node -> ChatMessageType.Node
                is ExecutionTraceItem.SubgraphStarted -> ChatMessageType.Task
                is ExecutionTraceItem.SubgraphCompleted -> ChatMessageType.Task
            }

            is ChatMessage.MermaidGraphMessage -> ChatMessageType.System
            is ChatMessage.LLMTokenUsageMessage -> ChatMessageType.TokenUsage
        }

sealed interface ExecutionTraceItem {
    val name: String

    data class Node(override val name: String) : ExecutionTraceItem
    data class SubgraphStarted(override val name: String) : ExecutionTraceItem
    data class SubgraphCompleted(
        override val name: String,
        val result: String? = null,
    ) : ExecutionTraceItem
}

data class LlmCallData(
    val messageHistory: List<LlmCallHistoryItem>,
    val availableTools: List<LlmCallToolData>,
)

sealed interface LlmCallHistoryItem {
    val text: String

    data class System(override val text: String) : LlmCallHistoryItem
    data class User(override val text: String) : LlmCallHistoryItem
    data class Assistant(override val text: String) : LlmCallHistoryItem
    data class Reasoning(override val text: String) : LlmCallHistoryItem
    data class ToolCall(val toolName: String, override val text: String) : LlmCallHistoryItem
    data class ToolResult(val toolName: String, override val text: String) : LlmCallHistoryItem
}

data class LlmCallToolData(
    val name: String,
    val requiredParameters: List<String>,
    val optionalParameters: List<String>,
)

fun List<Message>.toHistoryItems(): List<LlmCallHistoryItem> = flatMap { message ->
    when (message) {
        is Message.System -> message.parts.map { LlmCallHistoryItem.System(it.text) }
        is Message.User -> message.parts.mapNotNull { part ->
            when (part) {
                is MessagePart.Text -> LlmCallHistoryItem.User(part.text)
                is MessagePart.Tool.Result -> LlmCallHistoryItem.ToolResult(part.tool, part.output)
                is MessagePart.Attachment -> null
            }
        }

        is Message.Assistant -> message.parts.mapNotNull { part ->
            when (part) {
                is MessagePart.Text -> LlmCallHistoryItem.Assistant(part.text)
                is MessagePart.Reasoning -> LlmCallHistoryItem.Reasoning(part.content.joinToString(""))
                is MessagePart.Tool.Call -> LlmCallHistoryItem.ToolCall(part.tool, part.args)
                else -> null
            }
        }
    }
}

fun List<ToolDescriptor>.toToolData(): List<LlmCallToolData> =
    map { tool ->
        LlmCallToolData(
            name = tool.name,
            requiredParameters = tool.requiredParameters.map { it.name },
            optionalParameters = tool.optionalParameters.map { it.name }
        )
    }

@Stable
data class EmptyStateItem(
    val title: String,
    val action: () -> Unit,
)