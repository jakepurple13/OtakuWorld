package com.programmersbox.manga.shared.reader

import androidx.compose.ui.input.key.Key
import com.programmersbox.datastore.mangasettings.ReaderType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReaderKeysTest {

    private fun action(
        key: Key,
        type: ReaderType = ReaderType.List,
        shift: Boolean = false,
        appModifier: Boolean = false,
        question: Boolean = false,
    ) = readerKeyAction(key, shift = shift, appModifier = appModifier, question = question, readerType = type)

    @Test
    fun `down, right and space go to the next page`() {
        assertEquals(ReaderKeyAction.NextPage, action(Key.DirectionDown))
        assertEquals(ReaderKeyAction.NextPage, action(Key.DirectionRight))
        assertEquals(ReaderKeyAction.NextPage, action(Key.Spacebar))
    }

    @Test
    fun `up, left and shift space go to the previous page`() {
        assertEquals(ReaderKeyAction.PreviousPage, action(Key.DirectionUp))
        assertEquals(ReaderKeyAction.PreviousPage, action(Key.DirectionLeft))
        assertEquals(ReaderKeyAction.PreviousPage, action(Key.Spacebar, shift = true))
    }

    @Test
    fun `left and right are reversed in flip and curl modes`() {
        listOf(ReaderType.FlipPager, ReaderType.CurlPager).forEach { type ->
            assertEquals(ReaderKeyAction.NextPage, action(Key.DirectionLeft, type))
            assertEquals(ReaderKeyAction.PreviousPage, action(Key.DirectionRight, type))
            assertEquals(ReaderKeyAction.NextPage, action(Key.DirectionDown, type))
            assertEquals(ReaderKeyAction.PreviousPage, action(Key.DirectionUp, type))
        }
        assertEquals(ReaderKeyAction.NextPage, action(Key.DirectionRight, ReaderType.Pager))
    }

    @Test
    fun `keys with an app modifier are left for app shortcuts`() {
        assertNull(action(Key.DirectionDown, appModifier = true))
        assertNull(action(Key.F, appModifier = true))
        assertNull(action(Key.Slash, appModifier = true, question = true))
    }

    @Test
    fun `question mark opens the shortcuts list`() {
        assertEquals(ReaderKeyAction.ShortcutsList, action(Key.Slash, shift = true, question = true))
    }

    @Test
    fun `other keys map to their actions`() {
        assertEquals(ReaderKeyAction.ScreenDown, action(Key.PageDown))
        assertEquals(ReaderKeyAction.ScreenUp, action(Key.PageUp))
        assertEquals(ReaderKeyAction.FirstPage, action(Key.MoveHome))
        assertEquals(ReaderKeyAction.LastPage, action(Key.MoveEnd))
        assertEquals(ReaderKeyAction.NextChapter, action(Key.RightBracket))
        assertEquals(ReaderKeyAction.PreviousChapter, action(Key.LeftBracket))
        assertEquals(ReaderKeyAction.ChapterList, action(Key.C))
        assertEquals(ReaderKeyAction.PageGrid, action(Key.G))
        assertEquals(ReaderKeyAction.Bookmark, action(Key.B))
        assertEquals(ReaderKeyAction.SwitchMode, action(Key.M))
        assertEquals(ReaderKeyAction.ToggleBars, action(Key.H))
        assertEquals(ReaderKeyAction.Fullscreen, action(Key.F))
        assertEquals(ReaderKeyAction.Settings, action(Key.Comma))
        assertEquals(ReaderKeyAction.Escape, action(Key.Escape))
        assertNull(action(Key.Q))
    }

    @Test
    fun `switch mode cycles through every reader type`() {
        assertEquals(ReaderType.Pager, ReaderType.List.nextReaderType())
        assertEquals(ReaderType.FlipPager, ReaderType.Pager.nextReaderType())
        assertEquals(ReaderType.CurlPager, ReaderType.FlipPager.nextReaderType())
        assertEquals(ReaderType.List, ReaderType.CurlPager.nextReaderType())
    }
}
