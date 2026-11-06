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

class SkippedUpdateSourceDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: ItemDatabase
    private lateinit var dao: ItemDao

    private fun favorite(url: String, source: String, notify: Boolean = true, deleted: Boolean = false) = DbModel(
        title = url,
        description = "",
        url = url,
        imageUrl = "",
        source = source,
        shouldCheckForUpdate = notify,
        isDeleted = deleted,
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("skipped-source-test", ".db").also { it.deleteOnExit() }
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

    @Test fun `favorites from skipped sources are not checked`() = runTest {
        dao.insertFavorites(favorite("a1", "A"), favorite("b1", "B"), favorite("b2", "B"))
        dao.insertSkippedUpdateSources(listOf(SkippedUpdateSource("B")))

        assertEquals(listOf("a1"), dao.getFavoritesToCheckSync(checkAll = false).map { it.url })
        assertEquals(listOf("a1"), dao.getFavoritesToCheckSync(checkAll = true).map { it.url })
    }

    @Test fun `checkAll includes favorites with updates turned off`() = runTest {
        dao.insertFavorites(favorite("a1", "A"), favorite("a2", "A", notify = false))

        assertEquals(listOf("a1"), dao.getFavoritesToCheckSync(checkAll = false).map { it.url })
        assertEquals(setOf("a1", "a2"), dao.getFavoritesToCheckSync(checkAll = true).map { it.url }.toSet())
    }

    @Test fun `deleting a skipped source checks it again`() = runTest {
        dao.insertFavorites(favorite("a1", "A"))
        dao.insertSkippedUpdateSources(listOf(SkippedUpdateSource("A")))
        dao.deleteSkippedUpdateSources(listOf("A"))

        assertEquals(emptyList(), dao.getSkippedUpdateSources().first())
        assertEquals(listOf("a1"), dao.getFavoritesToCheckSync(checkAll = false).map { it.url })
    }

    @Test fun `favorite counts group by source and ignore deleted favorites`() = runTest {
        dao.insertFavorites(
            favorite("a1", "A"),
            favorite("b1", "B"),
            favorite("b2", "B"),
            favorite("b3", "B", deleted = true),
        )

        val counts = dao.getFavoriteCountsBySource().first().associate { it.source to it.count }

        assertEquals(mapOf("A" to 1, "B" to 2), counts)
    }
}
