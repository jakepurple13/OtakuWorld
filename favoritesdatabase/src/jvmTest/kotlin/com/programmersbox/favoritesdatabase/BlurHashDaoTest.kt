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

class BlurHashDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: BlurHashDatabase
    private lateinit var dao: BlurHashDao

    private fun hash(url: String, blurHash: String = "hash") = BlurHashItem(
        url = url,
        blurHash = blurHash,
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("blur-hash-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<BlurHashDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.blurDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `insertHash then getAllHashes and getHash return it`() = runTest {
        dao.insertHash(hash("https://example.com/1", "abc123"))

        val all = dao.getAllHashes().first()
        val single = dao.getHash("https://example.com/1").first()

        assertEquals(1, all.size)
        assertEquals("https://example.com/1", all[0].url)
        assertEquals("abc123", single?.blurHash)
    }

    @Test fun `getAllHashesCount reflects inserts`() = runTest {
        assertEquals(0, dao.getAllHashesCount().first())

        dao.insertHash(hash("https://example.com/1"))
        dao.insertHash(hash("https://example.com/2"))

        assertEquals(2, dao.getAllHashesCount().first())
    }

    @Test fun `insertHash with same primary key is ignored on conflict`() = runTest {
        dao.insertHash(hash("https://example.com/1", "original"))
        dao.insertHash(hash("https://example.com/1", "replacement"))

        val result = dao.getHash("https://example.com/1").first()

        assertEquals("original", result?.blurHash)
        assertEquals(1, dao.getAllHashesCount().first())
    }

    @Test fun `deleteHash removes the row`() = runTest {
        val item = hash("https://example.com/1")
        dao.insertHash(item)

        dao.deleteHash(item)

        assertNull(dao.getHash("https://example.com/1").first())
        assertEquals(0, dao.getAllHashesCount().first())
    }
}
