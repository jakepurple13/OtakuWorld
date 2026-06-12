package com.programmersbox.koogintegration.embedding

import ai.koog.embeddings.base.Embedder
import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import com.programmersbox.koogintegration.KoogDataStore

interface EmbedderProvider {
    val modelId: String

    /** Returns null when no API key is configured yet. */
    suspend fun provide(): Embedder?
}

/**
 * Always embeds with Google's embedding model regardless of the chat provider,
 * reusing the stored Koog API key. If that key is not a Google key, embedding
 * calls will fail and the refresh reports them as failures.
 */
class GoogleEmbedderProvider(
    private val koogDataStore: KoogDataStore,
) : EmbedderProvider {
    override val modelId: String = GoogleModels.Embeddings.GeminiEmbedding001.id

    override suspend fun provide(): Embedder? {
        val apiKey = koogDataStore.getApiKey().takeUnless { it.isEmpty() } ?: return null
        return LLMEmbedder(
            client = GoogleLLMClient(apiKey = apiKey),
            model = GoogleModels.Embeddings.GeminiEmbedding001,
        )
    }
}
