package com.programmersbox.koogintegration.embedding

import kotlin.time.measureTime

/**
 * Builds and maintains the on-disk embedding cache for the user's favorites.
 * Skips favorites without a description, reuses cached vectors when the
 * embedded text is unchanged, prunes unfavorited items, and re-embeds
 * everything if the embedding model changes.
 */
class FavoritesEmbeddingRepository(
    private val favoritesSource: FavoritesSource,
    private val embedderProvider: EmbedderProvider,
    private val cache: EmbeddingCache,
) {
    suspend fun refreshEmbeddings(): EmbeddingRefreshResult {
        val embedder = embedderProvider.provide()
            ?: return EmbeddingRefreshResult(missingApiKey = true)

        val favorites = favoritesSource
            .favorites()
            .filter { it.source != "MANGANELO" && it.source != "MANGA_4_LIFE" }
            .filterNot { it.description.isBlank() }
        val cached = cache.load()
        val reusable = if (cached.model == embedderProvider.modelId) cached.embeddings else emptyMap()

        val updated = mutableMapOf<String, FavoriteEmbedding>()
        var embedded = 0
        var reused = 0
        var skipped = 0
        var failed = 0

        println("Refreshing embeddings for ${favorites.size} favorites...")
        val fullDuration = measureTime {
            for (favorite in favorites) {
                val duration = measureTime {
                    val text = favorite.toEmbeddingText()
                    if (text == null) {
                        skipped++
                        continue
                    }
                    val textHash = text.hashCode()
                    val existing = reusable[favorite.url]
                    if (existing != null && existing.textHash == textHash) {
                        updated[favorite.url] = existing
                        reused++
                    } else {
                        runCatching { embedder.embed(text) }
                            .onSuccess { vector ->
                                updated[favorite.url] = FavoriteEmbedding(
                                    url = favorite.url,
                                    title = favorite.title,
                                    source = favorite.source,
                                    textHash = textHash,
                                    vector = vector.values,
                                )
                                embedded++
                            }
                            .onFailure {
                                failed++
                                it.printStackTrace()
                            }
                    }
                }
                println("Embedding ${favorite.title} took $duration")
            }
        }

        println("Full embedding took $fullDuration")

        val removed = (reusable.keys - updated.keys).size
        cache.save(EmbeddingCacheData(model = embedderProvider.modelId, embeddings = updated))

        return EmbeddingRefreshResult(
            embedded = embedded,
            reused = reused,
            skippedNoDescription = skipped,
            removed = removed,
            failed = failed,
        )
    }
}
