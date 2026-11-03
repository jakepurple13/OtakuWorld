package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoritesFtsTest {

    private lateinit var dbFile: File
    private lateinit var database: ItemDatabase
    private lateinit var dao: ItemDao

    private fun favorite(url: String, title: String, description: String = "") = DbModel(
        title = title,
        description = description,
        url = url,
        imageUrl = "",
        source = "Source",
    )

    private suspend fun search(raw: String) = dao.searchFavorites(ftsMatch(raw)!!).first()

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("favorites-fts-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ItemDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.itemDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `title prefix match`() = runTest {
        dao.insertFavorite(favorite("u1", "Blue Lock"))

        val hits = search("blu lo")

        assertEquals(1, hits.size)
        assertEquals("u1", hits[0].url)
        assertEquals("\u0002Blue\u0003 \u0002Lock\u0003", hits[0].titleHighlight)
    }

    @Test fun `description match has snippet and no title marker`() = runTest {
        dao.insertFavorite(favorite("u1", "Blue Lock", "Three hundred young strikers compete"))

        val hit = search("strikers").single()

        assertTrue('\u0002' !in hit.titleHighlight)
        assertTrue("\u0002strikers\u0003" in hit.descriptionSnippet)
    }

    @Test fun `title outranks description`() = runTest {
        dao.insertFavorite(favorite("b", "Frieren", "An elf mage retraces a journey"))
        dao.insertFavorite(favorite("a", "Mage Academy", "School story"))

        assertEquals(listOf("a", "b"), search("mage").map { it.url })
    }

    @Test fun `soft deleted excluded`() = runTest {
        dao.insertFavorite(favorite("u1", "Blue Lock"))
        dao.softDeleteFavorite("u1", 1)

        assertTrue(search("lock").isEmpty())
    }

    @Test fun `update reindexes`() = runTest {
        val item = favorite("u1", "Blue Lock")
        dao.insertFavorite(item)
        dao.updateFavoriteItem(item.copy(title = "Kaiju"))

        assertTrue(search("lock").isEmpty())
        assertEquals("u1", search("kai").single().url)
    }

    @Test fun `hard delete removes`() = runTest {
        val item = favorite("u1", "Blue Lock")
        dao.insertFavorite(item)
        dao.deleteFavorite(item)

        assertTrue(search("lock").isEmpty())
    }

    @Test fun `accents folded`() = runTest {
        dao.insertFavorite(favorite("u1", "Café Terrace"))

        assertEquals("u1", search("cafe").single().url)
    }

    @Test fun `no substring match`() = runTest {
        dao.insertFavorite(favorite("u1", "Blue Lock"))

        assertTrue(search("ock").isEmpty())
    }
}
