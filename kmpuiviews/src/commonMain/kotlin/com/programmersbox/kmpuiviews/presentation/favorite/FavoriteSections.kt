package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.compose.runtime.Immutable
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpuiviews.repository.FavoriteSearchHit
import com.programmersbox.kmpuiviews.repository.MatchField
import kotlin.time.Duration.Companion.days

enum class QuickFilter { RECENT, MULTI_SOURCE, UPDATES_OFF }

enum class FavoriteSort { TITLE, CHAPTERS, ADDED }

/** Every favorite saved under the same title, one entry per source. */
@Immutable
data class FavoriteGroup(
    val title: String,
    val items: List<DbModel>,
    val hit: FavoriteSearchHit? = null,
) {
    val maxChapters: Int get() = items.maxOf { it.numChapters }
    val latestCreatedAt: Long get() = items.maxOf { it.createdAt }
    val updatesOff: Boolean get() = items.none { it.shouldCheckForUpdate }
    val sources: List<String> get() = items.map { it.source }.distinct()
}

sealed interface SectionHeader {
    data class Letter(val char: Char) : SectionHeader
    enum class Added : SectionHeader { THIS_WEEK, THIS_MONTH, EARLIER }
    enum class Chapters : SectionHeader { OVER_500, FROM_100, UNDER_100 }
    data class Match(val field: MatchField) : SectionHeader
}

@Immutable
data class FavoriteSection(
    val header: SectionHeader,
    val groups: List<FavoriteGroup>,
)

private val RECENT_WINDOW = 30.days.inWholeMilliseconds
private val WEEK = 7.days.inWholeMilliseconds

fun groupFavorites(items: List<DbModel>): List<FavoriteGroup> =
    items.groupBy { it.title }.map { (title, list) -> FavoriteGroup(title, list) }

fun QuickFilter.matches(group: FavoriteGroup, now: Long): Boolean = when (this) {
    QuickFilter.RECENT -> group.latestCreatedAt > 0 && now - group.latestCreatedAt <= RECENT_WINDOW
    QuickFilter.MULTI_SOURCE -> group.items.size > 1
    QuickFilter.UPDATES_OFF -> group.updatesOff
}

/** Empty [sources] means every source. Quick filters combine with AND. */
fun filterGroups(
    groups: List<FavoriteGroup>,
    quick: Set<QuickFilter>,
    sources: Set<String>,
    now: Long,
): List<FavoriteGroup> = groups.filter { group ->
    quick.all { it.matches(group, now) } &&
        (sources.isEmpty() || group.items.any { it.source in sources })
}

fun buildSections(groups: List<FavoriteGroup>, sort: FavoriteSort, now: Long): List<FavoriteSection> {
    val sorted = when (sort) {
        FavoriteSort.TITLE -> groups.sortedWith(
            compareBy<FavoriteGroup> { it.title.firstOrNull()?.isLetter() == true }
                .thenBy { it.title.lowercase() }
        )
        FavoriteSort.CHAPTERS -> groups.sortedByDescending { it.maxChapters }
        FavoriteSort.ADDED -> groups.sortedByDescending { it.latestCreatedAt }
    }
    return sorted
        .groupBy { group -> header(group, sort, now) }
        .map { (header, list) -> FavoriteSection(header, list) }
}

private fun header(group: FavoriteGroup, sort: FavoriteSort, now: Long): SectionHeader = when (sort) {
    FavoriteSort.TITLE -> SectionHeader.Letter(
        group.title.firstOrNull()?.takeIf { it.isLetter() }?.uppercaseChar() ?: '#'
    )
    FavoriteSort.CHAPTERS -> when {
        group.maxChapters >= 500 -> SectionHeader.Chapters.OVER_500
        group.maxChapters >= 100 -> SectionHeader.Chapters.FROM_100
        else -> SectionHeader.Chapters.UNDER_100
    }
    FavoriteSort.ADDED -> {
        val created = group.latestCreatedAt
        when {
            created <= 0 -> SectionHeader.Added.EARLIER
            now - created <= WEEK -> SectionHeader.Added.THIS_WEEK
            now - created <= RECENT_WINDOW -> SectionHeader.Added.THIS_MONTH
            else -> SectionHeader.Added.EARLIER
        }
    }
}

/**
 * Attaches each group's best hit (strongest match field, then best score) and sections the
 * results by match field. Groups without a hit, and hits without a favorite, are dropped.
 */
fun buildSearchSections(groups: List<FavoriteGroup>, hits: List<FavoriteSearchHit>): List<FavoriteSection> {
    val hitByUrl = hits.associateBy { it.url }
    val bestFirst = compareBy<FavoriteSearchHit> { it.group.ordinal }.thenBy { it.score }
    val matched = groups.mapNotNull { group ->
        group.items
            .mapNotNull { hitByUrl[it.url] }
            .minWithOrNull(bestFirst)
            ?.let { group.copy(hit = it) }
    }
    return MatchField.entries.mapNotNull { field ->
        matched
            .filter { it.hit!!.group == field }
            .sortedBy { it.hit!!.score }
            .takeIf { it.isNotEmpty() }
            ?.let { FavoriteSection(SectionHeader.Match(field), it) }
    }
}
