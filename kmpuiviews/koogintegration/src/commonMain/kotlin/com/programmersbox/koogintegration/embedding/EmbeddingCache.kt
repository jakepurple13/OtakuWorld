package com.programmersbox.koogintegration.embedding

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class EmbeddingCache(
    private val storage: EmbeddingStorage,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val mutex = Mutex()
    private var memo: EmbeddingCacheData? = null

    suspend fun load(): EmbeddingCacheData = mutex.withLock {
        memo ?: runCatching { storage.read()?.let { json.decodeFromString<EmbeddingCacheData>(it) } }
            .getOrNull()
            .let { it ?: EmbeddingCacheData() }
            .also { memo = it }
    }

    suspend fun save(data: EmbeddingCacheData): Unit = mutex.withLock {
        memo = data
        storage.write(json.encodeToString(EmbeddingCacheData.serializer(), data))
    }
}
