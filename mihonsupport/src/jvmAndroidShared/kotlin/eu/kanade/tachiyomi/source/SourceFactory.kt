// Vendored from mihonapp/mihon@446f6df8 (Apache-2.0). Local edits marked with "OTAKU:".
package eu.kanade.tachiyomi.source

/**
 * A factory for creating sources at runtime.
 */
interface SourceFactory {
    /**
     * Create a new copy of the sources
     * @return The created sources
     */
    fun createSources(): List<Source>
}
