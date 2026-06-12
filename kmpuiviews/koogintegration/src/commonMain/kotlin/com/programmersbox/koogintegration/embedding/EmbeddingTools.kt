package com.programmersbox.koogintegration.embedding

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

/**
 * Embedding-backed tools for the agent. These run entirely on locally cached
 * vectors, so the LLM gets small, pre-ranked summaries instead of the full
 * favorites dump — fewer tokens per call.
 */
class EmbeddingTools(
    private val embeddingService: EmbeddingService,
    private val embeddingRepository: FavoritesEmbeddingRepository,
) : ToolSet {

    @Tool
    @LLMDescription(
        "Find favorited manga most similar to a given favorite using local embedding " +
            "cosine similarity. Pass the exact url of a favorite if known, otherwise a title to match."
    )
    suspend fun recommendSimilarManga(
        @LLMDescription("The url (preferred) or title of the favorite to find similar items for.")
        urlOrTitle: String,
        @LLMDescription("Maximum number of recommendations to return. Default 5.")
        limit: Int = 5,
    ): String {
        val seedUrl = findSeedUrl(urlOrTitle)
            ?: return "No embedded favorite matches \"$urlOrTitle\". Embeddings may still be generating, or the item has no description."
        val recommendations = embeddingService.getRecommendations(forUrl = seedUrl, limit = limit)
        if (recommendations.isEmpty()) return "No similar favorites found for \"$urlOrTitle\"."
        return buildString {
            appendLine("Favorites most similar to $seedUrl (cosine similarity, computed locally):")
            recommendations.forEach {
                appendLine("- ${it.title} | source: ${it.source} | url: ${it.url} | similarity: ${it.score.formatScore()}")
            }
        }
    }

    @Tool
    @LLMDescription(
        "Analyze the user's favorites collection using embeddings: counts, source distribution, " +
            "similarity/diversity, most and least representative favorites, and theme cluster count."
    )
    suspend fun analyzeFavoritesWithEmbeddings(): String {
        val analysis = embeddingService.analyzeFavorites()
        return buildString {
            appendLine("Favorites embedding analysis (all computed locally):")
            appendLine("- Total favorites: ${analysis.totalFavorites}")
            appendLine("- Embedded (has description): ${analysis.embeddedCount}")
            appendLine("- Skipped (no description): ${analysis.skippedNoDescription}")
            appendLine("- Average chapter count: ${analysis.averageChapterCount.formatScore()}")
            appendLine("- Sources: ${analysis.sourceDistribution.entries.joinToString { "${it.key}=${it.value}" }}")
            appendLine("- Average pairwise similarity: ${analysis.averagePairwiseSimilarity.formatScore()}")
            appendLine("- Diversity score (1 - similarity): ${analysis.diversityScore.formatScore()}")
            appendLine("- Theme clusters: ${analysis.clusterCount}")
            analysis.mostRepresentative?.let { appendLine("- Most representative favorite: ${it.title} (${it.url})") }
            analysis.leastRepresentative?.let { appendLine("- Most unique favorite: ${it.title} (${it.url})") }
        }
    }

    @Tool
    @LLMDescription(
        "Group the user's favorites into themed lists by embedding similarity clusters. " +
            "Returns list names with member titles and urls. The urls can be passed to " +
            "saveCuratedList to persist a list the user likes."
    )
    suspend fun getCuratedEmbeddingLists(
        @LLMDescription("Minimum number of items required for a list. Default 2.")
        minListSize: Int = 2,
    ): String {
        val lists = embeddingService.getCuratedLists(minListSize = minListSize)
        if (lists.isEmpty()) return "No curated lists could be generated. Embeddings may still be generating."
        return buildString {
            appendLine("Curated lists from embedding similarity clusters:")
            lists.forEach { list ->
                appendLine("## ${list.name}")
                appendLine(list.description)
                list.itemTitles.zip(list.itemUrls).forEach { (title, url) ->
                    appendLine("- $title | url: $url")
                }
            }
        }
    }

    @Tool
    @LLMDescription(
        "Regenerate the favorites embedding cache now. Use when embeddings seem missing or stale. " +
            "Requires a configured API key; calls Google's embedding model only for new or changed favorites."
    )
    suspend fun refreshFavoriteEmbeddings(): String {
        val result = embeddingRepository.refreshEmbeddings()
        if (result.missingApiKey) return "Cannot refresh embeddings: no API key configured."
        return "Embedding refresh complete: ${result.embedded} embedded, ${result.reused} reused from cache, " +
            "${result.skippedNoDescription} skipped (no description), ${result.removed} removed, ${result.failed} failed."
    }

    private suspend fun findSeedUrl(urlOrTitle: String): String? {
        val embeddings = embeddingService.getRecommendations(limit = Int.MAX_VALUE)
        return embeddings.firstOrNull { it.url == urlOrTitle }?.url
            ?: embeddings.firstOrNull { it.title.contains(urlOrTitle, ignoreCase = true) }?.url
    }

    private fun Double.formatScore(): String {
        val rounded = (this * 1000).toInt() / 1000.0
        return rounded.toString()
    }
}
