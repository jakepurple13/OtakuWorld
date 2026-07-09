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

private class FakeTranslationService(
    private val result: TranslationResult = TranslationResult(
        translatedTerm = "fake",
        translatedDefinition = "fake definition",
        reading = "fake reading",
    ),
) : TranslationService {
    var lastCall: Triple<String, String, String>? = null

    override suspend fun translateTerm(
        term: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): TranslationResult {
        lastCall = Triple(term, sourceLanguage, targetLanguage)
        return result
    }
}

class DictionaryRepositoryTest {

    private lateinit var dbFile: File
    private lateinit var database: DictionaryDatabase
    private lateinit var translationService: FakeTranslationService
    private lateinit var repository: DictionaryRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("dictionary-repository-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<DictionaryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        translationService = FakeTranslationService()
        repository = DictionaryRepository(database.dictionaryDao(), translationService)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `save with id 0 inserts a new entry`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))

        val stored = repository.getById(id).first()
        assertEquals("Sensei", stored?.term)
    }

    @Test
    fun `save with a non-zero id updates the existing entry`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))
        val stored = repository.getById(id).first()!!

        repository.save(stored.copy(term = "Sensei-updated"))

        assertEquals("Sensei-updated", repository.getById(id).first()?.term)
    }

    @Test
    fun `delete removes the entry`() = runTest {
        val id = repository.save(DictionaryEntry(term = "Sensei"))
        val stored = repository.getById(id).first()!!

        repository.delete(stored)

        assertNull(repository.getById(id).first())
    }

    @Test
    fun `getAll with Term sort delegates to the term-ordered query`() = runTest {
        repository.save(DictionaryEntry(term = "Zulu"))
        repository.save(DictionaryEntry(term = "Alpha"))

        val all = repository.getAll(DictionarySort.Term).first()

        assertEquals(listOf("Alpha", "Zulu"), all.map { it.term })
    }

    @Test
    fun `getAll with DateAdded sort delegates to the date-ordered query`() = runTest {
        repository.save(DictionaryEntry(term = "First", dateAdded = 100L))
        repository.save(DictionaryEntry(term = "Second", dateAdded = 200L))

        val all = repository.getAll(DictionarySort.DateAdded).first()

        assertEquals(listOf("Second", "First"), all.map { it.term })
    }

    @Test
    fun `search delegates to the dao search query`() = runTest {
        repository.save(DictionaryEntry(term = "Sensei"))
        repository.save(DictionaryEntry(term = "Gakusei"))

        val results = repository.search("Sen").first()

        assertEquals(1, results.size)
        assertEquals("Sensei", results[0].term)
    }

    @Test
    fun `translateTerm delegates to the translation service with the given args`() = runTest {
        val result = repository.translateTerm("Sensei", "ja", "en")

        assertEquals("fake", result.translatedTerm)
        assertEquals(Triple("Sensei", "ja", "en"), translationService.lastCall)
    }
}
