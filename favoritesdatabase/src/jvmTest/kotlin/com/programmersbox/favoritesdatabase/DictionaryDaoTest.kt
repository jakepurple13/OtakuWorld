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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DictionaryDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: DictionaryDatabase
    private lateinit var dao: DictionaryDao

    private fun entry(
        term: String,
        definition: String? = null,
        category: String? = null,
        dateAdded: Long = 0L,
    ) = DictionaryEntry(
        term = term,
        definition = definition,
        category = category,
        dateAdded = dateAdded,
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("dictionary-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<DictionaryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.dictionaryDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `insert then getById returns the entry`() = runTest {
        val id = dao.insert(entry("Sensei"))

        val result = dao.getById(id).first()

        assertEquals("Sensei", result?.term)
    }

    @Test
    fun `getById returns null when no row exists`() = runTest {
        assertNull(dao.getById(999L).first())
    }

    @Test
    fun `update modifies an existing entry`() = runTest {
        val id = dao.insert(entry("Sensei", definition = "Teacher"))
        val stored = dao.getById(id).first()!!

        dao.update(stored.copy(definition = "Master"))

        assertEquals("Master", dao.getById(id).first()?.definition)
    }

    @Test
    fun `delete removes the entry`() = runTest {
        val id = dao.insert(entry("Sensei"))
        val stored = dao.getById(id).first()!!

        dao.delete(stored)

        assertNull(dao.getById(id).first())
    }

    @Test
    fun `getAllByTerm orders alphabetically ignoring case`() = runTest {
        dao.insert(entry("zephyr"))
        dao.insert(entry("Apple"))
        dao.insert(entry("banana"))

        val all = dao.getAllByTerm().first()

        assertEquals(listOf("Apple", "banana", "zephyr"), all.map { it.term })
    }

    @Test
    fun `getAllByDateAdded orders newest first`() = runTest {
        dao.insert(entry("First", dateAdded = 100L))
        dao.insert(entry("Second", dateAdded = 300L))
        dao.insert(entry("Third", dateAdded = 200L))

        val all = dao.getAllByDateAdded().first()

        assertEquals(listOf("Second", "Third", "First"), all.map { it.term })
    }

    @Test
    fun `getAllByCategory orders by category then term`() = runTest {
        dao.insert(entry("Zulu", category = "Verbs"))
        dao.insert(entry("Alpha", category = "Nouns"))
        dao.insert(entry("Beta", category = "Nouns"))

        val all = dao.getAllByCategory().first()

        assertEquals(listOf("Alpha", "Beta", "Zulu"), all.map { it.term })
    }

    @Test
    fun `search matches term`() = runTest {
        dao.insert(entry("Sensei"))
        dao.insert(entry("Gakusei"))

        val results = dao.search("Sen").first()

        assertEquals(1, results.size)
        assertEquals("Sensei", results[0].term)
    }

    @Test
    fun `search matches definition`() = runTest {
        dao.insert(entry("Sensei", definition = "A wise teacher"))
        dao.insert(entry("Gakusei", definition = "A student"))

        val results = dao.search("teacher").first()

        assertEquals(1, results.size)
        assertEquals("Sensei", results[0].term)
    }

    @Test
    fun `search matches category`() = runTest {
        dao.insert(entry("Sensei", category = "Honorifics"))
        dao.insert(entry("Gakusei", category = "People"))

        val results = dao.search("Honorific").first()

        assertEquals(1, results.size)
        assertEquals("Sensei", results[0].term)
    }
}
