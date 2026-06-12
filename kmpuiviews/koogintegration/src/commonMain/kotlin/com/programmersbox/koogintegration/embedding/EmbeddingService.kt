package com.programmersbox.koogintegration.embedding

import ai.koog.embeddings.base.Vector

/**
 * All similarity math runs locally on cached vectors — no vector database,
 * no network. Cosine similarity comes from Koog's [Vector].
 */
class EmbeddingService(
    private val cache: EmbeddingCache,
    private val favoritesSource: FavoritesSource,
) {

    /**
     * Favorites most similar to [forUrl], or — when [forUrl] is null — favorites
     * ranked by similarity to the centroid of the user's whole collection
     * (their overall "taste vector").
     */
    suspend fun getRecommendations(forUrl: String? = null, limit: Int = 10): List<RecommendationResult> {
        val embeddings = cache.load().embeddings.values.toList()
        if (embeddings.isEmpty()) return emptyList()

        val (reference, candidates) = if (forUrl != null) {
            val seed = embeddings.find { it.url == forUrl } ?: return emptyList()
            seed.vector to embeddings.filter { it.url != forUrl }
        } else {
            centroid(embeddings.map { it.vector }) to embeddings
        }

        return candidates
            .map { it.toResult(cosine(reference, it.vector)) }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /** Insights over the collection: counts, source spread, similarity structure. */
    suspend fun analyzeFavorites(): FavoritesAnalysis {
        val favorites = favoritesSource.favorites()
        val embeddings = cache.load().embeddings.values.toList()
        val skipped = favorites.count { it.description.isBlank() }

        val averageSimilarity = averagePairwiseSimilarity(embeddings)
        val representativeness = embeddings.map { current ->
            val others = embeddings.filter { it.url != current.url }
            val avg = if (others.isEmpty()) 0.0 else others.map { cosine(current.vector, it.vector) }.average()
            current.toResult(avg)
        }

        return FavoritesAnalysis(
            totalFavorites = favorites.size,
            embeddedCount = embeddings.size,
            skippedNoDescription = skipped,
            sourceDistribution = favorites.groupingBy { it.source }.eachCount(),
            averageChapterCount = if (favorites.isEmpty()) 0.0 else favorites.map { it.numChapters }.average(),
            averagePairwiseSimilarity = averageSimilarity,
            diversityScore = 1.0 - averageSimilarity,
            mostRepresentative = representativeness.maxByOrNull { it.score },
            leastRepresentative = representativeness.minByOrNull { it.score },
            clusterCount = cluster(embeddings, DEFAULT_SIMILARITY_THRESHOLD).size,
        )
    }

    /**
     * Greedy centroid clustering of favorites into themed lists. Each cluster
     * is named after its most central member. Singleton clusters are dropped
     * by default ([minListSize]).
     */
    suspend fun getCuratedLists(
        similarityThreshold: Double = DEFAULT_SIMILARITY_THRESHOLD,
        minListSize: Int = 2,
    ): List<CuratedList> {
        val embeddings = cache.load().embeddings.values.toList()
        return cluster(embeddings, similarityThreshold)
            .filter { it.size >= minListSize }
            .map { members ->
                val center = centroid(members.map { it.vector })
                val anchor = members.maxByOrNull { cosine(center, it.vector) } ?: members.first()
                CuratedList(
                    name = "Similar to ${anchor.title}",
                    description = "${members.size} favorites grouped by embedding similarity, anchored by \"${anchor.title}\".",
                    itemUrls = members.map { it.url },
                    itemTitles = members.map { it.title },
                )
            }
    }

    private fun FavoriteEmbedding.toResult(score: Double) =
        RecommendationResult(url = url, title = title, source = source, score = score)

    private fun cosine(a: List<Double>, b: List<Double>): Double {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0.0
        val va = Vector(a)
        val vb = Vector(b)
        if (va.magnitude() == 0.0 || vb.magnitude() == 0.0) return 0.0
        return va.cosineSimilarity(vb)
    }

    private fun centroid(vectors: List<List<Double>>): List<Double> {
        if (vectors.isEmpty()) return emptyList()
        val size = vectors.first().size
        return List(size) { i -> vectors.sumOf { it.getOrElse(i) { 0.0 } } / vectors.size }
    }

    private fun averagePairwiseSimilarity(embeddings: List<FavoriteEmbedding>): Double {
        if (embeddings.size < 2) return 0.0
        var sum = 0.0
        var count = 0
        for (i in embeddings.indices) {
            for (j in i + 1 until embeddings.size) {
                sum += cosine(embeddings[i].vector, embeddings[j].vector)
                count++
            }
        }
        return sum / count
    }

    private fun cluster(
        embeddings: List<FavoriteEmbedding>,
        threshold: Double,
    ): List<List<FavoriteEmbedding>> {
        val clusters = mutableListOf<MutableList<FavoriteEmbedding>>()
        for (embedding in embeddings) {
            val best = clusters.maxByOrNull { cosine(centroid(it.map { m -> m.vector }), embedding.vector) }
            val bestScore = best?.let { cosine(centroid(it.map { m -> m.vector }), embedding.vector) } ?: 0.0
            if (best != null && bestScore >= threshold) best.add(embedding) else clusters.add(mutableListOf(embedding))
        }
        return clusters
    }

    companion object {
        const val DEFAULT_SIMILARITY_THRESHOLD = 0.8
    }
}
