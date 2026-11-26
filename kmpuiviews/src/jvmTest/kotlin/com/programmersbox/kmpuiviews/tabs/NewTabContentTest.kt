package com.programmersbox.kmpuiviews.tabs

import kotlin.test.Test
import kotlin.test.assertEquals

class NewTabContentTest {

    private val defaults = parseSections(null)

    @Test
    fun pageShowsTheOnSectionsInDefaultOrder() {
        assertEquals(
            listOf(NewTabSection.Search, NewTabSection.Continue, NewTabSection.Updates, NewTabSection.Closed),
            newTabContent(NewTabShows.Page, defaults),
        )
    }

    @Test
    fun pageFollowsTheSavedOrder() {
        val sections = parseSections("Links:1,Closed:1,Search:1,Updates:1,Continue:1")
        assertEquals(
            listOf(
                NewTabSection.Links,
                NewTabSection.Closed,
                NewTabSection.Search,
                NewTabSection.Updates,
                NewTabSection.Continue,
            ),
            newTabContent(NewTabShows.Page, sections),
        )
    }

    @Test
    fun pageLeavesOutOffSections() {
        val sections = parseSections("Search:0,Continue:1,Updates:0,Closed:1,Links:1")
        assertEquals(
            listOf(NewTabSection.Continue, NewTabSection.Closed, NewTabSection.Links),
            newTabContent(NewTabShows.Page, sections),
        )
        assertEquals(emptyList<NewTabSection>(), newTabContent(NewTabShows.Page, sections.map { it.copy(on = false) }))
    }

    @Test
    fun continueShowsOnlyContinueReadingWhateverTheSections() {
        assertEquals(listOf(NewTabSection.Continue), newTabContent(NewTabShows.Continue, defaults))
        assertEquals(
            listOf(NewTabSection.Continue),
            newTabContent(NewTabShows.Continue, defaults.map { it.copy(on = false) }),
        )
    }

    @Test
    fun recentAndBlankShowNoSections() {
        assertEquals(emptyList<NewTabSection>(), newTabContent(NewTabShows.Recent, defaults))
        assertEquals(emptyList<NewTabSection>(), newTabContent(NewTabShows.Blank, defaults))
    }
}
