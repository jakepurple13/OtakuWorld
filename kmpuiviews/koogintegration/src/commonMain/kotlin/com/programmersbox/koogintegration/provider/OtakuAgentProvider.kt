package com.programmersbox.koogintegration.provider

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.structuredOutputWithToolsStrategy
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.json.JsonStructure
import ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.kmpuiviews.koogintegration.BuildKonfig
import com.programmersbox.koogintegration.AgentMaker
import com.programmersbox.koogintegration.MathTools
import com.programmersbox.koogintegration.SystemPrompts
import com.programmersbox.koogintegration.agentresponse.AgentRecommendations
import com.programmersbox.koogintegration.agentresponse.AgentResponse
import com.programmersbox.koogintegration.agentresponse.GeneratedCustomListResponse
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
            examples = generateExampleResponses()
        )

        return AIAgent(
            promptExecutor = agentMakerInfo.executor,
            agentConfig = agentMakerInfo.agentConfig,
            strategy = structuredOutputWithToolsStrategy<AgentResponse>(
                config = StructuredRequestConfig(
                    default = StructuredRequest.Manual(genericStructure)
                    //default = StructuredRequest.Native(genericStructure)
                ),
                fixingParser = StructureFixingParser(
                    model = agentMakerInfo.agentConfig.model,
                    retries = 3
                )
            ),
            toolRegistry = ToolRegistry {
                tools(recommendationTools)
                tools(mathTools)
                tools(explainTools)
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

            val langfuseSecretKey = BuildKonfig.LANG_FUSE_SECRET_KEY
            val langfusePublicKey = BuildKonfig.LANG_FUSE_PUBLIC_KEY
            val langfuseUrl = BuildKonfig.LANG_FUSE_URL

            if (langfuseUrl != null && langfuseSecretKey != null && langfusePublicKey != null) {
                install(OpenTelemetry) {
                    addLangfuseExporter(
                        langfuseSecretKey = langfuseSecretKey,
                        langfusePublicKey = langfusePublicKey,
                        langfuseUrl = langfuseUrl,
                    )
                }
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

    private fun generateExampleResponses() = listOf(
        AgentResponse.Text("This is some sample text"),
        AgentRecommendations(
            text = "This is some sample recommendations",
            recommendations = listOf(
                Recommendation(
                    title = "Recommendation 1",
                    description = "Description 1",
                    reason = "This is an example reason",
                    genre = listOf("fantasy")
                ),
                Recommendation(
                    title = "Recommendation 2",
                    description = "Description 2",
                    reason = "This is an example reason",
                    genre = listOf("fantasy", "isekai")
                )
            )
        ),
        GeneratedCustomListResponse(
            response = "This is your new generated list!",
            listName = "Awesome List!",
            listDescription = "This is filled with awesome items!",
            items = listOf(
                CustomListInfo(
                    uuid = "EXAMPLE-UUID",
                    title = "Example",
                    description = "This is just an example",
                    url = "https://example.com/",
                    imageUrl = "https://example.com/2",
                    source = "Example"
                )
            )
        )
    )
}