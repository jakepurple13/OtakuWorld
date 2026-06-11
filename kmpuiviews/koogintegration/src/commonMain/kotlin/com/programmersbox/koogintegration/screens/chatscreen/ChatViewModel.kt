package com.programmersbox.koogintegration.screens.chatscreen

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgentBase
import ai.koog.agents.core.agent.MermaidDiagramGenerator
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.koogintegration.agentresponse.AgentResponse
import com.programmersbox.koogintegration.provider.AgentExecutionTraceEvent
import com.programmersbox.koogintegration.provider.AgentProvider
import com.programmersbox.koogintegration.provider.ChatAgentProvider
import com.programmersbox.koogintegration.provider.SingleTaskAgentProvider
import com.programmersbox.koogintegration.provider.TaskAgentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.uuid.Uuid

class ChatViewModel(
    private val agentProvider: AgentProvider,
    private val listDao: ListDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ChatUiState(
            title = agentProvider.title,
            chatMessages = listOf(ChatMessage.SystemMessage(agentProvider.description))
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var chatHistoryProvider: ChatHistoryProvider = InMemoryChatHistoryProvider()
    private var sessionId: String = UUID.randomUUID().toString()
    private var agent: AIAgent<String, AgentResponse>? = null

    fun onEvent(event: ChatUiEvents) {
        viewModelScope.launch {
            when (event) {
                is ChatUiEvents.UpdateInputText -> updateInputText(event.text)
                is ChatUiEvents.ToggleDebugEnabled -> toggleDebugEnabled()
                is ChatUiEvents.ToggleDebugOption -> toggleDebugOption(event.option)
                ChatUiEvents.SendMessage -> sendMessage()
                ChatUiEvents.RestartChat -> restartChat()
                ChatUiEvents.ShowMermaidGraph -> showMermaidGraph()
                is ChatUiEvents.SaveGeneratedList -> saveList(event)
            }
        }
    }

    private fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    private fun toggleDebugEnabled() {
        _uiState.update {
            it.copy(debugView = it.debugView.copy(enabled = !it.debugView.enabled))
        }
    }

    private fun toggleDebugOption(option: DebugOption) {
        _uiState.update {
            val current = it.debugView
            val newOptions =
                if (option in current.options) current.options - option else current.options + option
            it.copy(debugView = current.copy(options = newOptions))
        }
    }

    private fun sendMessage() {
        val userInput = _uiState.value.inputText.trim()
        if (userInput.isEmpty()) return

        // If the agent is waiting for a response to a question
        if (_uiState.value.userResponseRequested) {
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.UserMessage(userInput),
                    inputText = "",
                    isInputEnabled = false,
                    isLoading = true,
                    userResponseRequested = false,
                    currentUserResponse = userInput,
                    hideEmptyState = true
                )
            }
        } else {
            // Initial message flow - add user message and start the agent
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.UserMessage(userInput),
                    inputText = "",
                    isInputEnabled = false,
                    isLoading = true,
                    hideEmptyState = true
                )
            }

            viewModelScope.launch {
                runAgent(userInput)
            }
        }
    }

    private suspend fun runAgent(userInput: String) {
        withContext(Dispatchers.Default) {
            try {
                val currentAgent = agent ?: createAgent().also { agent = it }
                val result = currentAgent.run(userInput, sessionId)

                _uiState.update {
                    when (agentProvider) {
                        is SingleTaskAgentProvider -> it.copy(
                            chatMessages = it.chatMessages +
                                    ChatMessage.ResultMessage(result) +
                                    ChatMessage.SystemMessage("The agent has stopped."),
                            isInputEnabled = false,
                            isLoading = false,
                            isChatEnded = true,
                        )

                        is TaskAgentProvider -> it.copy(
                            chatMessages = it.chatMessages + ChatMessage.ResultMessage(result),
                            isInputEnabled = true,
                            isLoading = false,
                            isChatEnded = false,
                        )

                        is ChatAgentProvider -> it.copy(
                            chatMessages = it.chatMessages + ChatMessage.AgentMessage(result),
                            isInputEnabled = true,
                            isLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage.ErrorMessage("Error: ${e.message}"),
                        isInputEnabled = !_uiState.value.isChatEnded,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private suspend fun createAgent(): AIAgent<String, AgentResponse> {
        val onToolCallEvent: suspend (String, Map<String, String>) -> Unit = { toolName, args ->
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.ToolCallMessage(
                        toolName,
                        args
                    )
                )
            }
        }
        val onErrorEvent: suspend (String) -> Unit = { errorMessage ->
            println(errorMessage)
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.ErrorMessage(errorMessage),
                    isInputEnabled = true,
                    isLoading = false,
                )
            }
        }
        val onLLMCallEvent: suspend (List<Message>, List<ToolDescriptor>) -> Unit =
            { messages, tools ->
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage.LLMCallMessage(
                            LlmCallData(
                                messageHistory = messages.toHistoryItems(),
                                availableTools = tools.toToolData()
                            )
                        ),
                    )
                }
            }
        val onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit = { event ->
            val item = when (event) {
                is AgentExecutionTraceEvent.Node -> ExecutionTraceItem.Node(event.name)
                is AgentExecutionTraceEvent.SubgraphStarted -> ExecutionTraceItem.SubgraphStarted(
                    event.name
                )

                is AgentExecutionTraceEvent.SubgraphCompleted -> ExecutionTraceItem.SubgraphCompleted(
                    event.name,
                    event.result
                )
            }
            _uiState.update {
                it.copy(chatMessages = it.chatMessages + ChatMessage.ExecutionTraceMessage(item))
            }
        }

        val onLLMCallCompleted: suspend (ResponseMetaInfo) -> Unit = { metaInfo ->
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.LLMTokenUsageMessage(
                        inputTokens = metaInfo.inputTokensCount ?: 0,
                        outputTokens = metaInfo.outputTokensCount ?: 0,
                        totalTokens = metaInfo.totalTokensCount ?: 0
                    )
                )
            }
        }

        return when (val provider = agentProvider) {
            is ChatAgentProvider -> provider.provideAgent(
                historyProvider = chatHistoryProvider,
                onToolCallEvent = onToolCallEvent,
                onLLMCallEvent = onLLMCallEvent,
                onErrorEvent = onErrorEvent,
                onExecutionTraceEvent = onExecutionTraceEvent,
                onLLMCallCompleted = onLLMCallCompleted,
            )

            else -> error("Agent provider is not supported")

            /*is TaskAgentProvider -> provider.provideAgent(
                historyProvider = chatHistoryProvider,
                onToolCallEvent = onToolCallEvent,
                onLLMCallEvent = onLLMCallEvent,
                onErrorEvent = onErrorEvent,
                onExecutionTraceEvent = onExecutionTraceEvent,
                onLLMCallCompleted = onLLMCallCompleted,
                onAssistantMessage = { message ->
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages + ChatMessage.AgentMessage(message),
                            isInputEnabled = true,
                            isLoading = false,
                            userResponseRequested = true,
                        )
                    }

                    val userResponse = _uiState
                        .first { it.currentUserResponse != null }
                        .currentUserResponse
                        ?: throw IllegalStateException("User response is null")

                    _uiState.update {
                        it.copy(currentUserResponse = null)
                    }

                    userResponse
                },
            )*/
        }.also { agent ->
            ((agent as? AIAgentBase<*, *, *>)
                ?.strategy as? AIAgentGraphStrategy<*, *>)
                ?.let { graph -> MermaidDiagramGenerator.generate(graph) }
                ?.let { s ->
                    println(s)
                    _uiState.update { it.copy(mermaidGraphString = s) }
                }
        }
    }

    private fun showMermaidGraph() {
        viewModelScope.launch(Dispatchers.IO) {
            agent ?: createAgent().also { agent = it }
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.MermaidGraphMessage(
                        it.mermaidGraphString ?: "Graph has not been created yet"
                    ),
                )
            }
        }
    }

    private fun saveList(
        event: ChatUiEvents.SaveGeneratedList,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val parentUuid = Uuid.random().toString()
            listDao.createList(
                CustomListItem(
                    uuid = parentUuid,
                    name = event.chosenName,
                    useBiometric = event.useBiometrics,
                    description = event.generatedCustomListResponse.listDescription
                )
            )

            event.itemsToSave.forEach {
                listDao.addItem(
                    it.copy(uuid = parentUuid)
                )
            }
        }
    }

    private fun restartChat() {
        chatHistoryProvider = InMemoryChatHistoryProvider()
        sessionId = UUID.randomUUID().toString()
        agent = null
        _uiState.update {
            ChatUiState(
                title = agentProvider.title,
                chatMessages = listOf(ChatMessage.SystemMessage(agentProvider.description))
            )
        }
    }
}
