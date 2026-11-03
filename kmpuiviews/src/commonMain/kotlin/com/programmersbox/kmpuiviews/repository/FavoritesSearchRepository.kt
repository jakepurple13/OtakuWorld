package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.FavoriteFtsHit
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NoteFtsHit
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.favoritesdatabase.ftsMatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

enum class MatchField { TITLE, DESCRIPTION, NOTE }

/**
 * A search result for one favorite url. [titleHighlight] and [snippet] mark matched words
 * with `\u0002`/`\u0003` (see parseHighlight).
 */
data class FavoriteSearchHit(
    val url: String,
    val group: MatchField,
    val titleHighlight: String?,
    val snippet: String?,
    val snippetField: MatchField?,
    val matchedFields: Set<MatchField>,
    val score: Double,
)

private const val MARK = '\u0002'

/**
 * Searches favorites (title + description) and notes (content) with FTS5 and merges the two
 * by url. Each index has to match every word on its own; words are never combined across them.
 */
class FavoritesSearchRepository(
    private val itemDao: ItemDao,
    private val notesDao: NotesDao,
) {
    fun search(raw: String): Flow<List<FavoriteSearchHit>> {
        val favoritesMatch = ftsMatch(raw) ?: return flowOf(emptyList())
        val notesMatch = ftsMatch(raw, column = "content") ?: return flowOf(emptyList())
        return combine(
            itemDao.searchFavorites(favoritesMatch),
            notesDao.searchNoteHits(notesMatch),
            ::merge,
        )
    }

    private fun merge(favorites: List<FavoriteFtsHit>, notes: List<NoteFtsHit>): List<FavoriteSearchHit> {
        val favoriteByUrl = favorites.associateBy { it.url }
        val noteByUrl = notes.associateBy { it.itemUrl }
        return (favoriteByUrl.keys + noteByUrl.keys)
            .map { url -> toHit(url, favoriteByUrl[url], noteByUrl[url]) }
            .sortedWith(compareBy<FavoriteSearchHit> { it.group.ordinal }.thenBy { it.score })
    }

    private fun toHit(url: String, favorite: FavoriteFtsHit?, note: NoteFtsHit?): FavoriteSearchHit {
        val titleMatched = favorite != null && MARK in favorite.titleHighlight
        val descriptionMatched = favorite != null && MARK in favorite.descriptionSnippet
        val matched = buildSet {
            if (titleMatched) add(MatchField.TITLE)
            if (descriptionMatched) add(MatchField.DESCRIPTION)
            if (note != null) add(MatchField.NOTE)
        }
        val group = when {
            titleMatched -> MatchField.TITLE
            favorite != null -> MatchField.DESCRIPTION
            else -> MatchField.NOTE
        }
        val (snippet, snippetField) = when {
            note != null -> note.contentSnippet to MatchField.NOTE
            descriptionMatched -> favorite.descriptionSnippet to MatchField.DESCRIPTION
            else -> null to null
        }
        return FavoriteSearchHit(
            url = url,
            group = group,
            titleHighlight = favorite?.titleHighlight,
            snippet = snippet,
            snippetField = snippetField,
            matchedFields = matched,
            score = favorite?.score ?: note!!.score,
        )
    }
}
