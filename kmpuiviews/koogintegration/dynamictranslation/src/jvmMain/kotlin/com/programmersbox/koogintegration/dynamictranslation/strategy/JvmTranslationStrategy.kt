package com.programmersbox.koogintegration.dynamictranslation.strategy

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.asUserMessage
import ai.koog.agents.core.dsl.extension.nodeLLMSendMessage
import ai.koog.agents.core.dsl.extension.onTextMessage
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLModel
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.OcrResult
import com.programmersbox.koogintegration.dynamictranslation.model.TranslatedBlock
import com.programmersbox.koogintegration.dynamictranslation.model.TranslationResult

class JvmTranslationStrategy(
    private val client: OllamaClient,
    private val model: LLModel,
) : TranslationStrategy {

    private val executor = MultiLLMPromptExecutor(client)

    private val translationAgent: AIAgent<String, String> by lazy {
        AIAgent(
            promptExecutor = executor,
            agentConfig = AIAgentConfig(
                prompt = prompt("dt-translation") {
                    system(
                        "You are a translation engine. Translate the given text exactly and accurately. " +
                            "Return ONLY the translated text. No explanations, no extra words."
                    )
                },
                model = model,
                maxAgentIterations = 3,
            ),
            strategy = strategy<String, String>("dt-single-llm-call") {
                val nodeCallLLM by nodeLLMSendMessage()
                edge(nodeStart forwardTo nodeCallLLM asUserMessage { it })
                edge(nodeCallLLM forwardTo nodeFinish onTextMessage { true } transformed { it.trim() })
            },
        )
    }

    override suspend fun translate(ocr: OcrResult, config: DynamicTranslationConfig): TranslationResult {
        val blocks = ocr.blocks.mapIndexed { idx, block ->
            val prompt = "Translate from ${config.sourceLanguage} to ${config.targetLanguage}:\n${block.text}"
            val translated = translationAgent.run(prompt, "dt-translate-$idx-${block.text.hashCode()}")
            TranslatedBlock(
                original = block.text,
                translated = translated,
                bounds = block.bounds,
            )
        }
        return TranslationResult(blocks)
    }

    override fun close() {}
}
