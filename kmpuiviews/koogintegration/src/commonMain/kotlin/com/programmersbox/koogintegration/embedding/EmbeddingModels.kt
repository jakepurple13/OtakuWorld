package com.programmersbox.koogintegration.embedding

import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.serialization.Serializable

/** Abstraction over ItemDao so common code and tests don't need Room. */
fun interface FavoritesSource {
    suspend fun favorites(): List<DbModel>
}

/** One cached embedding for a single favorite, keyed by [url]. */
@Serializable
data class FavoriteEmbedding(
    val url: String,
    val title: String,
    val source: String,
    val textHash: Int,
    val vector: List<Double>,
)

/** Envelope persisted to disk. [model] invalidates everything when the embedding model changes. */
@Serializable
data class EmbeddingCacheData(
    val version: Int = 1,
    val model: String = "",
    val embeddings: Map<String, FavoriteEmbedding> = emptyMap(),
)

data class EmbeddingRefreshResult(
    val embedded: Int = 0,
    val reused: Int = 0,
    val skippedNoDescription: Int = 0,
    val removed: Int = 0,
    val failed: Int = 0,
    val missingApiKey: Boolean = false,
)

@Serializable
data class RecommendationResult(
    val url: String,
    val title: String,
    val source: String,
    val score: Double,
)

@Serializable
data class CuratedList(
    val name: String,
    val description: String,
    val itemUrls: List<String>,
    val itemTitles: List<String>,
)

@Serializable
data class FavoritesAnalysis(
    val totalFavorites: Int,
    val embeddedCount: Int,
    val skippedNoDescription: Int,
    val sourceDistribution: Map<String, Int>,
    val averageChapterCount: Double,
    val averagePairwiseSimilarity: Double,
    val diversityScore: Double,
    val mostRepresentative: RecommendationResult?,
    val leastRepresentative: RecommendationResult?,
    val clusterCount: Int,
)
