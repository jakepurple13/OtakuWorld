package com.programmersbox.koogintegration.embedding

/** Platform persistence for the embedding cache. Android: filesDir; JVM: ~/.otakuworld. */
interface EmbeddingStorage {
    suspend fun read(): String?
    suspend fun write(content: String)
}
