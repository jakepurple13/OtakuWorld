package com.programmersbox.koogintegration.embedding

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeEmbeddingStorage(var content: String? = null) : EmbeddingStorage {
    var writeCount = 0
    var readCount = 0
    override suspend fun read(): String? {
        readCount++
        return content
    }

    override suspend fun write(content: String) {
        writeCount++
        this.content = content
    }
}

class EmbeddingCacheTest {

    private val sample = EmbeddingCacheData(
        model = "gemini-embedding-001",
        embeddings = mapOf(
            "url1" to FavoriteEmbedding(
                url = "url1",
                title = "Naruto",
                source = "Src",
                textHash = 42,
                vector = listOf(0.1, 0.2, 0.3),
            )
        ),
    )

    @Test
    fun roundTripsThroughStorage() = runTest {
        val storage = FakeEmbeddingStorage()
        EmbeddingCache(storage).save(sample)
        // fresh cache instance forces a real read of persisted content
        assertEquals(sample, EmbeddingCache(storage).load())
    }

    @Test
    fun emptyStorageLoadsEmptyCache() = runTest {
        assertEquals(EmbeddingCacheData(), EmbeddingCache(FakeEmbeddingStorage()).load())
    }

    @Test
    fun corruptStorageLoadsEmptyCache() = runTest {
        assertEquals(EmbeddingCacheData(), EmbeddingCache(FakeEmbeddingStorage("not json {{{")).load())
    }

    @Test
    fun loadIsMemoizedUntilSave() = runTest {
        val storage = FakeEmbeddingStorage()
        val cache = EmbeddingCache(storage)
        cache.load()
        cache.load()
        assertEquals(1, storage.readCount)
        cache.save(sample)
        assertEquals(sample, cache.load())
        assertEquals(1, storage.readCount)
    }
}
