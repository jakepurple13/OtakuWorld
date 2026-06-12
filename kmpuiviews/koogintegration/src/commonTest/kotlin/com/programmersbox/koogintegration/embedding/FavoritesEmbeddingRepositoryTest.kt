package com.programmersbox.koogintegration.embedding

import ai.koog.embeddings.base.Embedder
import ai.koog.embeddings.base.Vector
import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeEmbedder : Embedder {
    var embedCalls = 0
    var failOn: String? = null
    override suspend fun embed(text: String): Vector {
        embedCalls++
        if (failOn != null && failOn!! in text) error("embed failure")
        // deterministic pseudo-vector derived from the text
        return Vector(listOf(text.length.toDouble(), text.hashCode().toDouble() % 100, 1.0))
    }

    override fun diff(embedding1: Vector, embedding2: Vector): Double =
        1.0 - embedding1.cosineSimilarity(embedding2)
}

private class FakeEmbedderProvider(
    var embedder: Embedder? = FakeEmbedder(),
) : EmbedderProvider {
    override val modelId: String = "fake-model"
    override suspend fun provide(): Embedder? = embedder
}

private fun fav(url: String, description: String = "desc $url", title: String = "title $url") = DbModel(
    title = title,
    description = description,
    url = url,
    imageUrl = "$url.png",
    source = "Src",
    numChapters = 1,
    shouldCheckForUpdate = true,
)

class FavoritesEmbeddingRepositoryTest {

    private fun repository(
        favorites: List<DbModel>,
        provider: EmbedderProvider = FakeEmbedderProvider(),
        storage: EmbeddingStorage = FakeEmbeddingStorage(),
    ) = FavoritesEmbeddingRepository(
        favoritesSource = { favorites },
        embedderProvider = provider,
        cache = EmbeddingCache(storage),
    )

    @Test
    fun embedsFavoritesWithDescriptions() = runTest {
        val repo = repository(listOf(fav("a"), fav("b")))
        val result = repo.refreshEmbeddings()
        assertEquals(2, result.embedded)
        assertEquals(0, result.skippedNoDescription)
    }

    @Test
    fun skipsEmptyDescriptions() = runTest {
        val repo = repository(listOf(fav("a"), fav("b", description = "")))
        val result = repo.refreshEmbeddings()
        assertEquals(1, result.embedded)
        assertEquals(1, result.skippedNoDescription)
    }

    @Test
    fun reusesUnchangedEmbeddingsAcrossRefreshes() = runTest {
        val embedder = FakeEmbedder()
        val provider = FakeEmbedderProvider(embedder)
        val storage = FakeEmbeddingStorage()
        repository(listOf(fav("a")), provider, storage).refreshEmbeddings()
        assertEquals(1, embedder.embedCalls)
        val second = repository(listOf(fav("a")), provider, storage).refreshEmbeddings()
        assertEquals(1, embedder.embedCalls) // cached, no new API call
        assertEquals(1, second.reused)
        assertEquals(0, second.embedded)
    }

    @Test
    fun reEmbedsWhenTextChanges() = runTest {
        val embedder = FakeEmbedder()
        val provider = FakeEmbedderProvider(embedder)
        val storage = FakeEmbeddingStorage()
        repository(listOf(fav("a", description = "old")), provider, storage).refreshEmbeddings()
        val second = repository(listOf(fav("a", description = "new longer description")), provider, storage).refreshEmbeddings()
        assertEquals(1, second.embedded)
        assertEquals(2, embedder.embedCalls)
    }

    @Test
    fun prunesRemovedFavorites() = runTest {
        val provider = FakeEmbedderProvider()
        val storage = FakeEmbeddingStorage()
        repository(listOf(fav("a"), fav("b")), provider, storage).refreshEmbeddings()
        val second = repository(listOf(fav("a")), provider, storage).refreshEmbeddings()
        assertEquals(1, second.removed)
        val cached = EmbeddingCache(storage).load()
        assertEquals(setOf("a"), cached.embeddings.keys)
    }

    @Test
    fun reportsMissingApiKey() = runTest {
        val result = repository(listOf(fav("a")), FakeEmbedderProvider(embedder = null)).refreshEmbeddings()
        assertTrue(result.missingApiKey)
        assertEquals(0, result.embedded)
    }

    @Test
    fun countsPerItemFailuresAndKeepsGoing() = runTest {
        val embedder = FakeEmbedder().apply { failOn = "title b" }
        val result = repository(listOf(fav("a"), fav("b"), fav("c")), FakeEmbedderProvider(embedder)).refreshEmbeddings()
        assertEquals(2, result.embedded)
        assertEquals(1, result.failed)
    }

    @Test
    fun modelChangeInvalidatesCache() = runTest {
        val storage = FakeEmbeddingStorage()
        val embedderA = FakeEmbedder()
        repository(listOf(fav("a")), FakeEmbedderProvider(embedderA), storage).refreshEmbeddings()
        val providerB = object : EmbedderProvider {
            override val modelId: String = "different-model"
            override suspend fun provide(): Embedder? = embedderA
        }
        val second = repository(listOf(fav("a")), providerB, storage).refreshEmbeddings()
        assertEquals(1, second.embedded)
        assertEquals(0, second.reused)
    }
}
