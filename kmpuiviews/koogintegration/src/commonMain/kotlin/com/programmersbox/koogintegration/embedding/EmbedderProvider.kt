package com.programmersbox.koogintegration.embedding

import ai.koog.embeddings.base.Embedder
import ai.koog.embeddings.base.Vector
import ai.koog.prompt.executor.clients.LLMEmbeddingProviderAPI
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLModel
import com.programmersbox.koogintegration.KoogDataStore

interface EmbedderProvider {
    val modelId: String

    /** Returns null when no API key is configured yet. */
    suspend fun provide(): Embedder?
}

/**
 * Koog [Embedder] over any [LLMEmbeddingProviderAPI] client. Koog's own
 * LLMEmbedder requires the LLMEmbeddingProvider abstract class, which the
 * Android variant of GoogleLLMClient does not extend (it only implements
 * LLMEmbeddingProviderAPI), so we adapt the API interface ourselves.
 */
private class ApiEmbedder(
    private val client: LLMEmbeddingProviderAPI,
    private val model: LLModel,
) : Embedder {
    override suspend fun embed(text: String): Vector = Vector(client.embed(text, model))

    override fun diff(embedding1: Vector, embedding2: Vector): Double =
        1.0 - embedding1.cosineSimilarity(embedding2)
}

/**
 * Always embeds with Google's embedding model regardless of the chat provider,
 * reusing the stored Koog API key. If that key is not a Google key, embedding
 * calls will fail and the refresh reports them as failures.
 */
class GoogleEmbedderProvider(
    private val koogDataStore: KoogDataStore,
) : EmbedderProvider {
    override val modelId: String = OllamaModels.Embeddings.ALL_MINI_LM.id

    override suspend fun provide(): Embedder? {
        //val apiKey = koogDataStore.getApiKey().takeUnless { it.isEmpty() } ?: return null
        return ApiEmbedder(
            client = OllamaClient(),//GoogleLLMClient(apiKey = apiKey),
            model = OllamaModels.Embeddings.ALL_MINI_LM//GoogleModels.Embeddings.GeminiEmbedding001,
        )
    }
}
