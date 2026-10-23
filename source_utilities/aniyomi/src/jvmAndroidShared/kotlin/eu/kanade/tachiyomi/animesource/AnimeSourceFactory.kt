// Copied from aniyomiorg/aniyomi 97414446b8a95994c72dd33c41c971a89d4d25b8, Apache-2.0. Local edits marked with "OTAKU:".
package eu.kanade.tachiyomi.animesource

/**
 * A factory for creating sources at runtime.
 */
interface AnimeSourceFactory {
    /**
     * Create a new copy of the sources
     * @return The created sources
     */
    fun createSources(): List<AnimeSource>
}
