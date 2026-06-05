package com.programmersbox.koogintegration

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.mistralai.MistralAILLMClient
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLModel

class AgentMaker(
    private val koogDataStore: KoogDataStore,
) {
    suspend fun getAgentInfo(): AgentInfo? {
        val apiKey = koogDataStore
            .getApiKey()
            .takeUnless { it.isEmpty() }
            ?: error("API key not set")

        val modelCompany = koogDataStore
            .getModelCompany()
            .takeUnless { it.isEmpty() }
            ?.let { runCatching { AIProvider.valueOf(it) }.getOrNull() }
            ?: error("Model company not set")

        val modelName = koogDataStore
            .getModelName()
            .takeUnless { it.isEmpty() }
            ?: error("Model name not set")

        val client = when (modelCompany) {
            AIProvider.ANTHROPIC -> AnthropicLLMClient(apiKey = apiKey)
            AIProvider.OPEN_AI -> OpenAILLMClient(apiKey = apiKey)
            AIProvider.GOOGLE -> GoogleLLMClient(apiKey = apiKey)
            AIProvider.DEEP_SEEK -> DeepSeekLLMClient(apiKey = apiKey)
            AIProvider.OPEN_ROUTER -> OpenRouterLLMClient(apiKey = apiKey)
            AIProvider.MISTRAL -> MistralAILLMClient(apiKey = apiKey)
            AIProvider.OLLAMA -> OllamaClient()
            AIProvider.NONE -> return null
        }

        val model = when (modelCompany) {
            AIProvider.ANTHROPIC -> AnthropicModels.models.find { it.id == modelName }
            AIProvider.OPEN_AI -> OpenAIModels.models.find { it.id == modelName }
            AIProvider.GOOGLE -> GoogleModels.models.find { it.id == modelName }
            AIProvider.DEEP_SEEK -> DeepSeekModels.models.find { it.id == modelName }
            AIProvider.OPEN_ROUTER -> OpenRouterModels.models.find { it.id == modelName }
            AIProvider.MISTRAL -> MistralAIModels.models.find { it.id == modelName }
            AIProvider.OLLAMA -> OllamaModels.models.find { it.id == modelName }
            AIProvider.NONE -> return null
        } ?: return null

        return AgentInfo(
            llmClient = client,
            model = model
        )
    }

    suspend fun needsOnboarding(): Boolean {
        val apiKey = koogDataStore.getApiKey().takeUnless { it.isEmpty() }
        val modelCompany = koogDataStore.getModelCompany().takeUnless { it.isEmpty() }
        val modelName = koogDataStore.getModelName().takeUnless { it.isEmpty() }

        return apiKey == null || modelCompany == null || modelName == null
    }
}

data class AgentInfo(
    val llmClient: LLMClient,
    val model: LLModel,
)

enum class AIProvider(val displayName: String) {
    OPEN_AI("OpenAI"),
    ANTHROPIC("Anthropic"),
    GOOGLE("Google"),
    DEEP_SEEK("DeepSeek"),
    OPEN_ROUTER("OpenRouter"),
    MISTRAL("Mistral"),
    OLLAMA("Ollama"),
    NONE("None")
}