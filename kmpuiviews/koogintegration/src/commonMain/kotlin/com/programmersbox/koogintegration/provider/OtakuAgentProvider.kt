package com.programmersbox.koogintegration.provider

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.structuredOutputWithToolsStrategy
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.json.JsonStructure
import ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator
import com.programmersbox.koogintegration.AgentMaker
import com.programmersbox.koogintegration.MathTools
import com.programmersbox.koogintegration.SystemPrompts
import com.programmersbox.koogintegration.agentresponse.AgentRecommendations
import com.programmersbox.koogintegration.agentresponse.AgentResponse
import com.programmersbox.koogintegration.agentresponse.Recommendation
import com.programmersbox.koogintegration.generateAiConfig
import com.programmersbox.koogintegration.provider.otakutools.LocalExplainTools
import com.programmersbox.koogintegration.provider.otakutools.RecommendationTools
import com.programmersbox.koogintegration.trackEvents

class OtakuAgentProvider(
    private val agentMaker: AgentMaker,
    private val mathTools: MathTools,
    private val recommendationTools: RecommendationTools,
    private val explainTools: LocalExplainTools,
) : ChatAgentProvider {
    override val title: String
        get() = "Otaku Agent"
    override val description: String
        get() = "I am a master of all that is weeb!"

    override suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (toolName: String, args: Map<String, String>) -> Unit,
        onLLMCallEvent: suspend (messages: List<Message>, tools: List<ToolDescriptor>) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
        onLLMCallCompleted: suspend (ResponseMetaInfo) -> Unit,
    ): AIAgent<String, AgentResponse> {
        val agentMakerInfo = generateAiConfig(
            agentMaker = agentMaker,
            promptId = "otaku",
            prompt = SystemPrompts.otakuPrompt,
            maxAgentIterations = 200
        )

        val genericStructure = JsonStructure.create<AgentResponse>(
            schemaGenerator = StandardJsonSchemaGenerator,
            examples = listOf(
                AgentResponse.Text("This is some sample text"),
                AgentRecommendations(
                    text = "This is some sample recommendations",
                    recommendations = listOf(
                        Recommendation(
                            title = "Recommendation 1",
                            description = "Description 1",
                            reason = "https://example.com/1",
                            genre = listOf("fantasy")
                        ),
                        Recommendation(
                            title = "Recommendation 2",
                            description = "Description 2",
                            reason = "https://example.com/2",
                            genre = listOf("fantasy", "isekai")
                        )
                    )
                )
            )
        )

        return AIAgent(
            promptExecutor = agentMakerInfo.executor,
            agentConfig = agentMakerInfo.agentConfig,
            strategy = structuredOutputWithToolsStrategy<AgentResponse>(
                config = StructuredRequestConfig(
                    default = StructuredRequest.Manual(genericStructure)
                ),
            ),
            toolRegistry = ToolRegistry {
                tools(recommendationTools.asTools())
                tools(mathTools.asTools())
                tools(explainTools.asTools())
            }
        ) {
            install(EventHandler) {
                trackEvents(
                    onToolCallEvent = onToolCallEvent,
                    onErrorEvent = onErrorEvent,
                    onLLMCallEvent = onLLMCallEvent,
                    onExecutionTraceEvent = onExecutionTraceEvent,
                    onLLMCallCompleted = onLLMCallCompleted
                )
            }

            install(ChatMemory) {
                chatHistoryProvider = historyProvider
                windowSize(50)
            }

            install(Persistence) {
                storage = InMemoryPersistenceStorageProvider()
                enableAutomaticPersistence = true
            }
        }
    }
}