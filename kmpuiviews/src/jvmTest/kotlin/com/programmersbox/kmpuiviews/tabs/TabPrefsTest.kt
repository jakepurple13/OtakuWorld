package com.programmersbox.kmpuiviews.tabs

import kotlin.test.Test
import kotlin.test.assertEquals

class TabPrefsTest {
    @Test
    fun unknownEnumNameFallsBackToDefault() {
        assertEquals(TabPosition.Top, TabPosition.fromName("garbage"))
        assertEquals(ClickOpens.SameTab, ClickOpens.fromName("garbage"))
    }

    @Test
    fun knownEnumNamesMap() {
        assertEquals(TabPosition.Side, TabPosition.fromName("Side"))
        assertEquals(ClickOpens.NewTab, ClickOpens.fromName("NewTab"))
    }

    @Test
    fun newEnumPrefsFallBackToTheirDefaults() {
        listOf(null, "garbage").forEach { raw ->
            assertEquals(ModClick.Background, ModClick.fromName(raw))
            assertEquals(NewTabPlace.NextToCurrent, NewTabPlace.fromName(raw))
            assertEquals(AfterClose.Right, AfterClose.fromName(raw))
            assertEquals(TabMotionMode.System, TabMotionMode.fromName(raw))
            assertEquals(NewTabShows.Page, NewTabShows.fromName(raw))
            assertEquals(NewTabBackground.Plain, NewTabBackground.fromName(raw))
            assertEquals(OnLaunch.Restore, OnLaunch.fromName(raw))
            assertEquals(SleepAfter.Never, SleepAfter.fromName(raw))
            assertEquals(MiniPlayerCorner.BottomRight, MiniPlayerCorner.fromName(raw))
        }
    }

    @Test
    fun newEnumPrefsMapKnownNames() {
        assertEquals(ModClick.Foreground, ModClick.fromName("Foreground"))
        assertEquals(NewTabPlace.AtEnd, NewTabPlace.fromName("AtEnd"))
        assertEquals(AfterClose.LastUsed, AfterClose.fromName("LastUsed"))
        assertEquals(TabMotionMode.Off, TabMotionMode.fromName("Off"))
        assertEquals(NewTabShows.Blank, NewTabShows.fromName("Blank"))
        assertEquals(NewTabBackground.LastCover, NewTabBackground.fromName("LastCover"))
        assertEquals(OnLaunch.NewTab, OnLaunch.fromName("NewTab"))
        assertEquals(SleepAfter.H4, SleepAfter.fromName("H4"))
        assertEquals(MiniPlayerCorner.TopLeft, MiniPlayerCorner.fromName("TopLeft"))
    }

    @Test
    fun countPrefsFallBackToTheirDefaults() {
        assertEquals(4, continueCountOf(null))
        assertEquals(4, continueCountOf(5))
        assertEquals(12, continueCountOf(12))
        assertEquals(25, closedKeptOf(null))
        assertEquals(25, closedKeptOf(0))
        assertEquals(50, closedKeptOf(50))
    }

    @Test
    fun migrateOnLaunchMapsOldBoolean() {
        assertEquals(OnLaunch.Restore, migrateOnLaunch(true))
        assertEquals(OnLaunch.Restore, migrateOnLaunch(null))
        assertEquals(OnLaunch.Home, migrateOnLaunch(false))
    }

    @Test
    fun sectionsPrefRoundTrips() {
        val default = listOf(
            SectionSetting(NewTabSection.Search, true),
            SectionSetting(NewTabSection.Continue, true),
            SectionSetting(NewTabSection.Updates, true),
            SectionSetting(NewTabSection.Closed, true),
            SectionSetting(NewTabSection.Links, false),
        )
        assertEquals(default, parseSections(null))
        assertEquals(default, parseSections(""))
        assertEquals("Search:1,Continue:1,Updates:1,Closed:1,Links:0", formatSections(default))
        assertEquals(default, parseSections(formatSections(default)))

        val custom = listOf(
            SectionSetting(NewTabSection.Links, true),
            SectionSetting(NewTabSection.Closed, false),
            SectionSetting(NewTabSection.Search, true),
            SectionSetting(NewTabSection.Continue, false),
            SectionSetting(NewTabSection.Updates, true),
        )
        assertEquals(custom, parseSections(formatSections(custom)))

        // Unknown and malformed entries are dropped, duplicates keep the first, missing ones are appended with defaults.
        assertEquals(
            listOf(
                SectionSetting(NewTabSection.Closed, false),
                SectionSetting(NewTabSection.Search, true),
                SectionSetting(NewTabSection.Continue, true),
                SectionSetting(NewTabSection.Updates, true),
                SectionSetting(NewTabSection.Links, false),
            ),
            parseSections("Closed:0,Bogus:1,Search,Closed:1, Search:1 ,Updates:x"),
        )
    }
}
