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
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.Recommendation
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.koogintegration.agentresponse.AgentResponse
import com.programmersbox.koogintegration.provider.AgentExecutionTraceEvent
import com.programmersbox.koogintegration.provider.AgentProvider
import com.programmersbox.koogintegration.provider.ChatAgentProvider
import com.programmersbox.koogintegration.provider.SingleTaskAgentProvider
import com.programmersbox.koogintegration.provider.TaskAgentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.uuid.Uuid

class ChatViewModel(
    private val agentProvider: AgentProvider,
    private val listDao: ListDao,
    private val recommendationDao: RecommendationDao,
) : ViewModel() {
    val snackbarHostState = SnackbarHostState()

    val uiState: StateFlow<ChatUiState>
        field = MutableStateFlow(
            ChatUiState(
                title = agentProvider.title,
                chatMessages = listOf(ChatMessage.SystemMessage(agentProvider.description))
            )
        )

    val savedRecommendations = recommendationDao.getAllRecommendations()

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
                is ChatUiEvents.SaveRecommendation -> saveRecommendation(event)
                is ChatUiEvents.DeleteRecommendation -> deleteRecommendation(event.recommendation)
            }
        }
    }

    private fun updateInputText(text: String) {
        uiState.update { it.copy(inputText = text) }
    }

    private fun toggleDebugEnabled() {
        uiState.update {
            it.copy(debugView = it.debugView.copy(enabled = !it.debugView.enabled))
        }
    }

    private fun toggleDebugOption(option: DebugOption) {
        uiState.update {
            val current = it.debugView
            val newOptions =
                if (option in current.options) current.options - option else current.options + option
            it.copy(debugView = current.copy(options = newOptions))
        }
    }

    private fun sendMessage() {
        val userInput = uiState.value.inputText.trim()
        if (userInput.isEmpty()) return

        // If the agent is waiting for a response to a question
        if (uiState.value.userResponseRequested) {
            uiState.update {
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
            uiState.update {
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

                uiState.update {
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
                uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage.ErrorMessage("Error: ${e.message}"),
                        isInputEnabled = !uiState.value.isChatEnded,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private suspend fun createAgent(): AIAgent<String, AgentResponse> {
        val onToolCallEvent: suspend (String, Map<String, String>) -> Unit = { toolName, args ->
            uiState.update {
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
            uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.ErrorMessage(errorMessage),
                    isInputEnabled = true,
                    isLoading = false,
                )
            }
        }
        val onLLMCallEvent: suspend (List<Message>, List<ToolDescriptor>) -> Unit =
            { messages, tools ->
                uiState.update {
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
            uiState.update {
                it.copy(chatMessages = it.chatMessages + ChatMessage.ExecutionTraceMessage(item))
            }
        }

        val onLLMCallCompleted: suspend (ResponseMetaInfo) -> Unit = { metaInfo ->
            uiState.update {
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
                    uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages + ChatMessage.AgentMessage(message),
                            isInputEnabled = true,
                            isLoading = false,
                            userResponseRequested = true,
                        )
                    }

                    val userResponse = uiState
                        .first { it.currentUserResponse != null }
                        .currentUserResponse
                        ?: throw IllegalStateException("User response is null")

                    uiState.update {
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
                    uiState.update { it.copy(mermaidGraphString = s) }
                }
        }
    }

    private fun showMermaidGraph() {
        viewModelScope.launch(Dispatchers.IO) {
            agent ?: createAgent().also { agent = it }
            uiState.update {
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

    private fun saveRecommendation(chatUiEvents: ChatUiEvents.SaveRecommendation) {
        viewModelScope.launch(Dispatchers.IO) {
            recommendationDao.insertRecommendation(
                Recommendation(
                    title = chatUiEvents.recommendation.title,
                    description = chatUiEvents.recommendation.description,
                    reason = chatUiEvents.recommendation.reason,
                    genre = chatUiEvents.recommendation.genre,
                )
            )
            snackbarHostState.showSnackbar("${chatUiEvents.recommendation.title} saved")
        }
    }

    private fun deleteRecommendation(recommendation: Recommendation) {
        viewModelScope.launch(Dispatchers.IO) {
            recommendationDao.deleteRecommendation(recommendation)
            snackbarHostState.showSnackbar("${recommendation.title} deleted")
        }
    }

    private fun restartChat() {
        chatHistoryProvider = InMemoryChatHistoryProvider()
        sessionId = UUID.randomUUID().toString()
        agent = null
        uiState.update {
            ChatUiState(
                title = agentProvider.title,
                chatMessages = listOf(ChatMessage.SystemMessage(agentProvider.description))
            )
        }
    }
}
