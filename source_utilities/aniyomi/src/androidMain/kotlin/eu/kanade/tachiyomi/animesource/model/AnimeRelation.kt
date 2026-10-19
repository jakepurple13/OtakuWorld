// Copied from aniyomiorg/aniyomi 97414446b8a95994c72dd33c41c971a89d4d25b8, Apache-2.0. Local edits marked with "OTAKU:".
package eu.kanade.tachiyomi.animesource.model

/**
 * A named group of anime, that are related to a specific anime entry
 *
 * @since extensions-lib 17
 */
class AnimeRelation(
    val name: String,
    val animes: List<SAnime>,
)
