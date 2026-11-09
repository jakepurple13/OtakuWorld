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
        dao.insertFavorites(favorite("a1", "A"), favorite("b1", "B"), favorite("c1", "C"))

        assertEquals(listOf("a1"), dao.getFavoritesToCheckSync(checkAll = false, listOf("B", "C")).map { it.url })
        assertEquals(listOf("a1"), dao.getFavoritesToCheckSync(checkAll = true, listOf("B", "C")).map { it.url })
    }

    @Test fun `checkAll includes favorites with updates turned off`() = runTest {
        dao.insertFavorites(favorite("a1", "A"), favorite("a2", "A", notify = false))

        assertEquals(listOf("a1"), dao.getFavoritesToCheckSync(checkAll = false, emptyList()).map { it.url })
        assertEquals(setOf("a1", "a2"), dao.getFavoritesToCheckSync(checkAll = true, emptyList()).map { it.url }.toSet())
    }

    @Test fun `deleting a skipped extension removes it`() = runTest {
        dao.insertSkippedUpdateSources(listOf(SkippedUpdateSource("pkg.a"), SkippedUpdateSource("pkg.b")))
        dao.deleteSkippedUpdateSources(listOf("pkg.a"))

        assertEquals(listOf("pkg.b"), dao.getSkippedUpdateSources().first())
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
