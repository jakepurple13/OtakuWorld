package com.programmersbox.koogintegration.embedding

import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmbeddingServiceTest {

    // Two tight clusters in 3d space plus one outlier:
    // action cluster around (1, 0, 0); romance cluster around (0, 1, 0); outlier (0, 0, 1).
    private val vectors = mapOf(
        "action1" to listOf(1.0, 0.0, 0.0),
        "action2" to listOf(0.98, 0.05, 0.0),
        "action3" to listOf(0.95, 0.1, 0.0),
        "romance1" to listOf(0.0, 1.0, 0.0),
        "romance2" to listOf(0.05, 0.98, 0.0),
        "outlier" to listOf(0.0, 0.0, 1.0),
    )

    private fun cacheData() = EmbeddingCacheData(
        model = "fake-model",
        embeddings = vectors.mapValues { (url, vector) ->
            FavoriteEmbedding(url = url, title = "Title $url", source = "Src-${url.first()}", textHash = 1, vector = vector)
        },
    )

    private fun favorites() = vectors.keys.map { url ->
        DbModel(
            title = "Title $url",
            description = "desc",
            url = url,
            imageUrl = "$url.png",
            source = "Src-${url.first()}",
            numChapters = 10,
            shouldCheckForUpdate = true,
        )
    } + DbModel(
        title = "No Description",
        description = "",
        url = "nodesc",
        imageUrl = "nodesc.png",
        source = "Src-n",
        numChapters = 0,
        shouldCheckForUpdate = true,
    )

    private suspend fun service(): EmbeddingService {
        val storage = FakeEmbeddingStorage()
        val cache = EmbeddingCache(storage)
        cache.save(cacheData())
        return EmbeddingService(cache = cache, favoritesSource = { favorites() })
    }

    @Test
    fun recommendationsForUrlRankSameClusterFirst() = runTest {
        val recs = service().getRecommendations(forUrl = "action1", limit = 2)
        assertEquals(listOf("action2", "action3"), recs.map { it.url })
        assertTrue(recs[0].score > recs[1].score)
        assertTrue(recs.none { it.url == "action1" }) // seed excluded
    }

    @Test
    fun recommendationsWithoutSeedUseTasteCentroid() = runTest {
        val recs = service().getRecommendations(limit = 6)
        assertEquals(6, recs.size)
        // outlier is least similar to overall taste
        assertEquals("outlier", recs.last().url)
    }

    @Test
    fun recommendationsForUnknownUrlAreEmpty() = runTest {
        assertTrue(service().getRecommendations(forUrl = "unknown").isEmpty())
    }

    @Test
    fun curatedListsGroupBySimilarity() = runTest {
        val lists = service().getCuratedLists(similarityThreshold = 0.8, minListSize = 2)
        assertEquals(2, lists.size)
        val byCount = lists.sortedByDescending { it.itemUrls.size }
        assertEquals(setOf("action1", "action2", "action3"), byCount[0].itemUrls.toSet())
        assertEquals(setOf("romance1", "romance2"), byCount[1].itemUrls.toSet())
        assertTrue(lists.all { it.name.isNotBlank() })
    }

    @Test
    fun analysisReportsCountsAndClusters() = runTest {
        val analysis = service().analyzeFavorites()
        assertEquals(7, analysis.totalFavorites)
        assertEquals(6, analysis.embeddedCount)
        assertEquals(1, analysis.skippedNoDescription)
        assertEquals(3, analysis.clusterCount) // action, romance, outlier
        assertTrue(analysis.averagePairwiseSimilarity in 0.0..1.0)
        assertEquals(1.0 - analysis.averagePairwiseSimilarity, analysis.diversityScore, 1e-9)
        assertEquals(3, analysis.sourceDistribution["Src-a"])
        assertEquals("outlier", analysis.leastRepresentative?.url)
    }

    @Test
    fun emptyCacheYieldsEmptyResults() = runTest {
        val svc = EmbeddingService(EmbeddingCache(FakeEmbeddingStorage()), favoritesSource = { emptyList() })
        assertTrue(svc.getRecommendations().isEmpty())
        assertTrue(svc.getCuratedLists().isEmpty())
        assertEquals(0, svc.analyzeFavorites().embeddedCount)
    }
}
