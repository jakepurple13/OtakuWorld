package com.programmersbox.kmpuiviews.tabs

import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotesPanelTest {
    private val details = Screen.DetailsScreen.Details("t", "d", "https%3A%2F%2Fx.com%2Fa%3Fb%3D1", "i", "src")

    private fun tab(start: NavKey = details) =
        TabState(TabId(1), TabNavigation3Actions(start), pinned = false)

    @Test
    fun toggleFlipsNotesOpenPerTab() {
        val a = tab()
        val b = tab()
        assertFalse(a.notesOpen.value)
        a.toggleNotes()
        assertTrue(a.notesOpen.value)
        assertFalse(b.notesOpen.value)
        a.toggleNotes()
        assertFalse(a.notesOpen.value)
    }

    @Test
    fun visibleOnlyWhenOpenAndOnDetails() {
        assertTrue(notesPanelVisible(true, details))
        assertFalse(notesPanelVisible(false, details))
        assertFalse(notesPanelVisible(true, Screen.RecentScreen))
    }

    @Test
    fun itemUrlIsTheDecodedKeyUrl() {
        assertEquals("https://x.com/a?b=1", notesItemUrl(details))
        assertNull(notesItemUrl(Screen.RecentScreen))
    }

    @Test
    fun panelSurvivesLeavingAndReturningToDetails() {
        val t = tab()
        t.toggleNotes()
        t.nav.noteDetail("x")
        assertFalse(notesPanelVisible(t.notesOpen.value, t.current))
        t.nav.popBackStack()
        assertTrue(notesPanelVisible(t.notesOpen.value, t.current))
    }
}
