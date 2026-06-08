package com.programmersbox.koogintegration

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.dsl.extension.ReceivedToolResults
import ai.koog.agents.core.dsl.extension.ToolCalls
import ai.koog.agents.core.dsl.extension.asToolResultMessage
import ai.koog.agents.core.dsl.extension.asUserMessage
import ai.koog.agents.core.dsl.extension.nodeExecuteTools
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.dsl.extension.nodeLLMSendMessage
import ai.koog.agents.core.dsl.extension.onTextMessage
import ai.koog.agents.core.dsl.extension.onToolCalls
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.features.eventHandler.feature.EventHandlerConfig
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import com.programmersbox.koogintegration.provider.AgentExecutionTraceEvent

fun EventHandlerConfig.trackEvents(
    onToolCallEvent: suspend (String, Map<String, String>) -> Unit,
    onErrorEvent: suspend (String) -> Unit,
    onLLMCallEvent: suspend (List<Message>, List<ToolDescriptor>) -> Unit,
    onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
    onLLMCallCompleted: suspend (ResponseMetaInfo) -> Unit,
) {
    onNodeExecutionStarting { ctx ->
        onExecutionTraceEvent(AgentExecutionTraceEvent.Node(ctx.node.name))
    }

    onSubgraphExecutionStarting { ctx ->
        onExecutionTraceEvent(AgentExecutionTraceEvent.SubgraphStarted(ctx.subgraph.name))
    }

    onSubgraphExecutionCompleted { ctx ->
        onExecutionTraceEvent(
            AgentExecutionTraceEvent.SubgraphCompleted(
                ctx.subgraph.name,
                result = ctx.output?.toString()
            )
        )
    }

    onToolCallStarting { ctx ->
        onToolCallEvent(ctx.toolName, ctx.toolArgs.entries.mapValues { it.value.toString() })
    }

    onAgentExecutionFailed { ctx ->
        ctx.error.printStackTrace()
        onErrorEvent("${ctx.error.message}")
    }

    onLLMCallStarting { ctx ->
        onLLMCallEvent(ctx.prompt.messages, ctx.tools)
    }

    onLLMCallCompleted { ctx ->
        ctx.response?.metaInfo?.let { onLLMCallCompleted(it) }
    }
}

inline fun <reified Output> defaultCompressionStrategy(
    crossinline transformer: (String) -> Output,
) = strategy<String, Output>(
    "default-compression-strategy"
) {
    // 1. Define the history compression node
    val nodeCompressHistory by nodeLLMCompressHistory<Message.User>(
        // Compresses history in chunks of 10 messages
        strategy = HistoryCompressionStrategy.Chunked(10),
        preserveMemory = true // Optional: Keeps retrieved facts intact if you're using memory
    )

    val nodeCallLLM: AIAgentNodeBase<Message.User, Message.Assistant> by nodeLLMSendMessage()
    val nodeExecuteTools: AIAgentNodeBase<ToolCalls, ReceivedToolResults> by nodeExecuteTools()

    // 2. Route the user's input through the compression node first
    edge(nodeStart forwardTo nodeCompressHistory asUserMessage { it })

    // 3. Forward the flow from the compression node to the LLM node
    edge(nodeCompressHistory forwardTo nodeCallLLM)

    // 4. The tool execution and finishing logic remains exactly the same
    edge(nodeCallLLM forwardTo nodeExecuteTools onToolCalls { true })
    edge(nodeExecuteTools forwardTo nodeCallLLM asToolResultMessage { true })
    edge(
        nodeCallLLM forwardTo nodeFinish
                onTextMessage { true }
                transformed { transformer(it) }
    )
}

/**
 * Generates an AI configuration by combining a given prompt, model, and execution logic.
 *
 * @param agentMaker The `AgentMaker` instance used to retrieve agent information such as LLM client and model.
 * @param prompt The main system prompt to guide the AI agent's behavior.
 * @param promptId The unique identifier for the prompt, used to associate specific configurations.
 * @param maxAgentIterations The maximum number of iterations allowed for the AI agent, with a default value of 50.
 * @return An instance of `AgentMakerInfo`, containing the AI agent configuration and the associated executor.
 * @throws IllegalStateException If the agent information retrieved from `AgentMaker` is `null`.
 */
internal suspend fun generateAiConfig(
    agentMaker: AgentMaker,
    prompt: String,
    promptId: String,
    maxAgentIterations: Int = 50,
): AgentMakerInfo {
    val agentInfo = agentMaker.getAgentInfo() ?: error("Agent info is null")
    val executor = MultiLLMPromptExecutor(agentInfo.llmClient)

    val agentConfig = AIAgentConfig(
        prompt = prompt(promptId) {
            system(prompt)
        },
        model = agentInfo.model,
        maxAgentIterations = maxAgentIterations
    )

    return AgentMakerInfo(agentConfig, executor)
}

data class AgentMakerInfo(
    val agentConfig: AIAgentConfig,
    val executor: PromptExecutor,
)

class MathTools : ToolSet {

    @Tool
    @LLMDescription("Returns the result of the addition of two numbers.")
    fun add(
        @LLMDescription("The first number to add.")
        num1: Int,
        @LLMDescription("The second number to add.")
        num2: Int,
    ): Int {
        return num1 + num2
    }

    @Tool
    @LLMDescription("Returns the result of the subtraction of two numbers.")
    fun subtract(
        @LLMDescription("The number to subtract from.")
        num1: Int,
        @LLMDescription("The number to subtract.")
        num2: Int,
    ): Int {
        return num1 - num2
    }

    @Tool
    @LLMDescription("Returns the result of the multiplication of two numbers.")
    fun multiply(
        @LLMDescription("The first number to multiply.")
        num1: Int,
        @LLMDescription("The second number to multiply.")
        num2: Int,
    ): Int {
        return num1 * num2
    }

    @Tool
    @LLMDescription("Returns the result of the division of two numbers.")
    fun divide(
        @LLMDescription("The number to divide.")
        num1: Int,
        @LLMDescription("The number to divide by.")
        num2: Int,
    ): Int {
        return num1 / num2
    }

}