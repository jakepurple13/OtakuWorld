package com.programmersbox.koogintegration

import ai.koog.http.client.ktor.KtorKoogHttpClient
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
import ai.koog.prompt.llm.AnthropicLLMProvider
import ai.koog.prompt.llm.DeepSeekLLMProvider
import ai.koog.prompt.llm.GoogleLLMProvider
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.MistralAILLMProvider
import ai.koog.prompt.llm.OllamaLLMProvider
import ai.koog.prompt.llm.OpenAILLMProvider
import ai.koog.prompt.llm.OpenRouterLLMProvider

class AgentMaker(
    private val koogDataStore: KoogDataStore,
    private val platformAgents: PlatformAgents,
) {
    suspend fun getAgentInfo(): AgentInfo? {
        val apiKey = koogDataStore
            .getApiKey()
            .takeUnless { it.isEmpty() }
        //?: error("API key not set")

        val modelCompany = koogDataStore
            .getModelCompany()
            .takeUnless { it.isEmpty() }
            ?: error("Model company not set")

        val modelName = koogDataStore
            .getModelName()
            .takeUnless { it.isEmpty() }
            ?: error("Model name not set")

        val httpClient = KtorKoogHttpClient.Factory()

        val client = when (modelCompany) {
            NoLLMProvider.display -> return null
            GoogleLLMProvider.display -> GoogleLLMClient(apiKey = apiKey ?: error("API key not set"), httpClientFactory = httpClient)
            AnthropicLLMProvider.display -> AnthropicLLMClient(apiKey = apiKey ?: error("API key not set"), httpClientFactory = httpClient)
            OpenAILLMProvider.display -> OpenAILLMClient(apiKey = apiKey ?: error("API key not set"), httpClientFactory = httpClient)
            DeepSeekLLMProvider.display -> DeepSeekLLMClient(apiKey = apiKey ?: error("API key not set"), httpClientFactory = httpClient)
            OllamaLLMProvider.display -> OllamaClient(httpClientFactory = httpClient)
            MistralAILLMProvider.display -> MistralAILLMClient(apiKey = apiKey ?: error("API key not set"), httpClientFactory = httpClient)
            OpenRouterLLMProvider.display -> OpenRouterLLMClient(apiKey = apiKey ?: error("API key not set"), httpClientFactory = httpClient)
            else -> platformAgents.isPlatformProvider(modelCompany, apiKey) ?: return null
        }

        val model = when (modelCompany) {
            AnthropicLLMProvider.display -> AnthropicModels.models.find { it.id == modelName }
            OpenAILLMProvider.display -> OpenAIModels.models.find { it.id == modelName }
            GoogleLLMProvider.display -> GoogleModels.models.find { it.id == modelName }
            DeepSeekLLMProvider.display -> DeepSeekModels.models.find { it.id == modelName }
            OpenRouterLLMProvider.display -> OpenRouterModels.models.find { it.id == modelName }
            MistralAILLMProvider.display -> MistralAIModels.models.find { it.id == modelName }
            OllamaLLMProvider.display -> OllamaModels.models.find { it.id == modelName }
            NoLLMProvider.display -> return null
            else -> platformAgents.isPlatformModel(modelCompany, modelName) ?: return null
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

    fun mapStringToProvider(modelCompany: String): LLMProvider = when (modelCompany) {
        NoLLMProvider.display -> NoLLMProvider
        GoogleLLMProvider.display -> GoogleLLMProvider
        AnthropicLLMProvider.display -> AnthropicLLMProvider
        OpenAILLMProvider.display -> OpenAILLMProvider
        DeepSeekLLMProvider.display -> DeepSeekLLMProvider
        OllamaLLMProvider.display -> OllamaLLMProvider
        MistralAILLMProvider.display -> MistralAILLMProvider
        OpenRouterLLMProvider.display -> OpenRouterLLMProvider
        else -> platformAgents.platformLLMProviders().find { it.display == modelCompany } ?: NoLLMProvider
    }

    val providerList by lazy {
        listOf(
            GoogleLLMProvider,
            AnthropicLLMProvider,
            OpenAILLMProvider,
            DeepSeekLLMProvider,
            OllamaLLMProvider,
            MistralAILLMProvider,
            OpenRouterLLMProvider,
            NoLLMProvider
        ) + platformAgents.platformLLMProviders()
    }
}

expect class PlatformAgents {
    fun isPlatformProvider(modelCompany: String, apiKey: String?): LLMClient?
    fun platformLLMProviders(): List<LLMProvider>
    fun isPlatformModel(modelCompany: String, modelName: String): LLModel?
}

expect fun platformModels(modelCompany: String): List<LLModel>
expect fun getModelLinkToDownload(name: String): String?
expect fun canDownloadModel(name: String): Boolean

object NoLLMProvider : LLMProvider("none", "None")

data class AgentInfo(
    val llmClient: LLMClient,
    val model: LLModel,
)