package com.programmersbox.kmpuiviews.presentation.quickactions

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickActionsFilterTest {
    private val texts = mapOf(
        QuickAction.CheckForUpdates to "Check for updates Never checked",
        QuickAction.ScanQrCode to "Scan QR code Import a title or list",
        QuickAction.DailyOverview to "Daily overview Yesterday: 23 chapters",
        QuickAction.GlobalSearch to "Global search Search every installed source",
        QuickAction.Lists to "Lists Your custom lists",
        QuickAction.SavedNotifications to "Saved notifications Saved update notifications",
        QuickAction.BackgroundTasks to "Background tasks See what's scheduled",
        QuickAction.UpdateErrors to "Update check errors See what failed in the last check",
    )

    @Test
    fun blankQueryReturnsAllInOrder() {
        assertEquals(QuickAction.entries, filterQuickActions("", texts))
        assertEquals(QuickAction.entries, filterQuickActions("   ", texts))
    }

    @Test
    fun filterTrimsAndIgnoresCase() {
        assertEquals(listOf(QuickAction.ScanQrCode), filterQuickActions("  QR ", texts))
    }

    @Test
    fun filterMatchesSupportingText() {
        assertEquals(listOf(QuickAction.UpdateErrors), filterQuickActions("failed", texts))
    }

    @Test
    fun filterMatchesDailyOverviewCount() {
        assertEquals(listOf(QuickAction.DailyOverview), filterQuickActions("23 chapters", texts))
    }

    @Test
    fun filterOnlySearchesGivenActions() {
        val visible = QuickAction.entries - QuickAction.DailyOverview
        assertEquals(visible, filterQuickActions("", texts, visible))
        assertEquals(emptyList(), filterQuickActions("yesterday", texts, visible))
    }

    @Test
    fun noMatchIsEmpty() {
        assertEquals(emptyList(), filterQuickActions("zzz", texts))
    }

    @Test
    fun moveSelectionStopsAtEnds() {
        assertEquals(0, moveSelection(0, -1, 3))
        assertEquals(2, moveSelection(2, 1, 3))
        assertEquals(1, moveSelection(0, 1, 3))
    }

    @Test
    fun selectionClampsWhenListShrinks() {
        assertEquals(1, moveSelection(5, 0, 2))
        assertEquals(0, moveSelection(3, 0, 0))
    }
}
