package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDatabase
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import com.programmersbox.kmpuiviews.testing.createTestNotesDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoritesSearchRepositoryTest {

    private lateinit var items: ItemDatabase
    private lateinit var notes: NotesDatabase
    private lateinit var repository: FavoritesSearchRepository

    private suspend fun favorite(url: String, title: String, description: String = "") =
        items.itemDao().insertFavorite(DbModel(title, description, url, "", "Source"))

    private suspend fun note(url: String, content: String) =
        notes.notesDao().upsertNote(NoteItem(url, "Title", content))

    private suspend fun search(raw: String) = repository.search(raw).first()

    @BeforeTest
    fun setUp() {
        items = createTestItemDatabase()
        notes = createTestNotesDatabase()
        repository = FavoritesSearchRepository(items.itemDao(), notes.notesDao())
    }

    @AfterTest
    fun tearDown() {
        items.close()
        notes.close()
    }

    @Test fun `title hit groups as TITLE`() = runTest {
        favorite("u1", "Blue Lock")

        val hit = search("lock").single()

        assertEquals("u1", hit.url)
        assertEquals(MatchField.TITLE, hit.group)
        assertEquals("Blue \u0002Lock\u0003", hit.titleHighlight)
    }

    @Test fun `description hit groups as DESCRIPTION with description snippet`() = runTest {
        favorite("u1", "Blue Lock", "Three hundred young strikers compete")

        val hit = search("strikers").single()

        assertEquals(MatchField.DESCRIPTION, hit.group)
        assertEquals(MatchField.DESCRIPTION, hit.snippetField)
        assertTrue("\u0002strikers\u0003" in hit.snippet!!)
    }

    @Test fun `description and note both match prefers note snippet`() = runTest {
        favorite("u1", "Frieren", "An elf mage retraces a journey")
        note("u1", "Favorite part is the first-class mage exam")

        val hit = search("mage").single()

        assertEquals(MatchField.DESCRIPTION, hit.group)
        assertEquals(MatchField.NOTE, hit.snippetField)
        assertEquals(setOf(MatchField.DESCRIPTION, MatchField.NOTE), hit.matchedFields)
    }

    @Test fun `note only hit groups as NOTE`() = runTest {
        favorite("u1", "Kagurabachi")
        note("u1", "Wait for the tournament arc to finish")

        val hit = search("tournament arc").single()

        assertEquals(MatchField.NOTE, hit.group)
        assertEquals(null, hit.titleHighlight)
    }

    @Test fun `words are not combined across indexes`() = runTest {
        favorite("u1", "Blue Lock")
        note("u1", "Wait for the tournament")

        assertTrue(search("blue tournament").isEmpty())
    }

    @Test fun `fts syntax only gives empty list`() = runTest {
        favorite("u1", "Blue Lock")

        assertTrue(search("\"*").isEmpty())
    }

    @Test fun `note for a non favorite is still emitted`() = runTest {
        note("orphan", "tournament arc")

        assertEquals("orphan", search("tournament").single().url)
    }

    @Test fun `groups are ordered title then description then note`() = runTest {
        favorite("n", "Kagurabachi")
        note("n", "mage notes")
        favorite("d", "Frieren", "An elf mage")
        favorite("t", "Mage Academy")

        assertEquals(listOf("t", "d", "n"), search("mage").map { it.url })
    }
}
