package com.programmersbox.kmpuiviews.presentation.favorite

import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpuiviews.repository.FavoriteSearchHit
import com.programmersbox.kmpuiviews.repository.MatchField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val DAY = 24L * 60 * 60 * 1000
private const val NOW = 1_000 * DAY

class FavoriteSectionsTest {

    private fun db(
        title: String,
        url: String = title,
        source: String = "A",
        chapters: Int = 0,
        createdAt: Long = 0,
        checkUpdates: Boolean = true,
    ) = DbModel(title, "", url, "", source, chapters, checkUpdates, createdAt = createdAt)

    private fun group(vararg items: DbModel) = FavoriteGroup(items.first().title, items.toList())

    private fun hit(url: String, group: MatchField, score: Double = -1.0) =
        FavoriteSearchHit(url, group, null, null, null, setOf(group), score)

    @Test fun `groupFavorites groups by title`() {
        val groups = groupFavorites(listOf(db("One Piece", "u1", "A"), db("One Piece", "u2", "B"), db("Dandadan")))

        assertEquals(setOf("One Piece", "Dandadan"), groups.map { it.title }.toSet())
        assertEquals(listOf("A", "B"), groups.first { it.title == "One Piece" }.sources)
    }

    @Test fun `title sections are letters with non letters first`() {
        val groups = groupFavorites(listOf(db("blue lock"), db("Akane"), db("86"), db("Boruto")))

        val sections = buildSections(groups, FavoriteSort.TITLE, NOW)

        assertEquals(
            listOf(SectionHeader.Letter('#'), SectionHeader.Letter('A'), SectionHeader.Letter('B')),
            sections.map { it.header },
        )
        assertEquals(listOf("blue lock", "Boruto"), sections[2].groups.map { it.title })
    }

    @Test fun `chapter buckets split at 500 and 100`() {
        val groups = groupFavorites(listOf(db("a", chapters = 500), db("b", chapters = 499), db("c", chapters = 99), db("d", chapters = 100)))

        val sections = buildSections(groups, FavoriteSort.CHAPTERS, NOW)

        assertEquals(
            listOf(SectionHeader.Chapters.OVER_500, SectionHeader.Chapters.FROM_100, SectionHeader.Chapters.UNDER_100),
            sections.map { it.header },
        )
        assertEquals(listOf("b", "d"), sections[1].groups.map { it.title })
    }

    @Test fun `added buckets split at 7 and 30 days with unknown dates last`() {
        val groups = groupFavorites(
            listOf(
                db("unknown", createdAt = 0),
                db("week", createdAt = NOW - 7 * DAY),
                db("month", createdAt = NOW - 30 * DAY),
                db("old", createdAt = NOW - 31 * DAY),
            )
        )

        val sections = buildSections(groups, FavoriteSort.ADDED, NOW)

        assertEquals(
            listOf(SectionHeader.Added.THIS_WEEK, SectionHeader.Added.THIS_MONTH, SectionHeader.Added.EARLIER),
            sections.map { it.header },
        )
        assertEquals(listOf("old", "unknown"), sections[2].groups.map { it.title })
    }

    @Test fun `recent excludes unknown createdAt`() {
        assertFalse(QuickFilter.RECENT.matches(group(db("a", createdAt = 0)), NOW))
        assertTrue(QuickFilter.RECENT.matches(group(db("a", createdAt = NOW - 30 * DAY)), NOW))
        assertFalse(QuickFilter.RECENT.matches(group(db("a", createdAt = NOW - 31 * DAY)), NOW))
    }

    @Test fun `updates off needs every source off`() {
        assertTrue(QuickFilter.UPDATES_OFF.matches(group(db("a", "1", checkUpdates = false), db("a", "2", checkUpdates = false)), NOW))
        assertFalse(QuickFilter.UPDATES_OFF.matches(group(db("a", "1", checkUpdates = false), db("a", "2")), NOW))
    }

    @Test fun `source filter matches a multi source group through any source`() {
        val groups = listOf(group(db("a", "1", "A"), db("a", "2", "B")), group(db("b", source = "C")))

        assertEquals(listOf("a"), filterGroups(groups, emptySet(), setOf("B"), NOW).map { it.title })
        assertEquals(2, filterGroups(groups, emptySet(), emptySet(), NOW).size)
    }

    @Test fun `quick filters combine with AND`() {
        val groups = listOf(
            group(db("multi off", "1", checkUpdates = false), db("multi off", "2", "B", checkUpdates = false)),
            group(db("multi on", "3"), db("multi on", "4", "B")),
            group(db("single off", checkUpdates = false)),
        )

        val result = filterGroups(groups, setOf(QuickFilter.MULTI_SOURCE, QuickFilter.UPDATES_OFF), emptySet(), NOW)

        assertEquals(listOf("multi off"), result.map { it.title })
    }

    @Test fun `search sections ordered title description note and non favorites dropped`() {
        val groups = groupFavorites(listOf(db("Note one", "n"), db("Desc one", "d"), db("Title one", "t")))
        val hits = listOf(hit("n", MatchField.NOTE), hit("d", MatchField.DESCRIPTION), hit("t", MatchField.TITLE), hit("orphan", MatchField.NOTE))

        val sections = buildSearchSections(groups, hits)

        assertEquals(
            listOf(MatchField.TITLE, MatchField.DESCRIPTION, MatchField.NOTE).map(SectionHeader::Match),
            sections.map { it.header },
        )
        assertEquals(listOf("n"), sections[2].groups.flatMap { g -> g.items.map { it.url } })
        assertEquals(MatchField.NOTE, sections[2].groups.single().hit?.group)
    }

    @Test fun `multi source group takes its best hit`() {
        val groups = groupFavorites(listOf(db("One Piece", "u1"), db("One Piece", "u2", "B")))
        val hits = listOf(hit("u1", MatchField.NOTE), hit("u2", MatchField.TITLE))

        val sections = buildSearchSections(groups, hits)

        assertEquals(SectionHeader.Match(MatchField.TITLE), sections.single().header)
    }
}
