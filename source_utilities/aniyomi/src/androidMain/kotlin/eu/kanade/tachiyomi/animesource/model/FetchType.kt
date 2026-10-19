// Copied from aniyomiorg/aniyomi 97414446b8a95994c72dd33c41c971a89d4d25b8, Apache-2.0. Local edits marked with "OTAKU:".
package eu.kanade.tachiyomi.animesource.model

/**
 * Define what type of content the anime should fetch.
 * The fetch type for a [SAnime] will not update after it's been initialized
 * to either Seasons or Episodes
 *
 * @since extensions-lib 16
 */
enum class FetchType {
    /**
     * [SAnime] will only call `getSeasonList`.
     */
    Seasons,

    /**
     * [SAnime] will only call `getEpisodeList`.
     */
    Episodes,
}
