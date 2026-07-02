package com.programmersbox.kmpuiviews.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ListRepositoryTest {

    private lateinit var dbFile: File
    private lateinit var database: ListDatabase

    private fun repository(loggedIn: Boolean = false) = ListRepository(
        listDao = database.listDao(),
        systemAlerter = SystemAlerter(),
        authManager = FakeAuthManager(loggedIn = loggedIn),
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("list-repository-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ListDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `create makes a list retrievable via getAllLists`() = runTest {
        repository().create("My List")

        val lists = repository().getAllLists().first()

        assertEquals(1, lists.size)
        assertEquals("My List", lists[0].item.name)
    }

    @Test fun `addList makes a list retrievable via getAllLists`() = runTest {
        repository().addList("Another List")

        val lists = repository().getAllLists().first()

        assertEquals(1, lists.size)
        assertEquals("Another List", lists[0].item.name)
    }

    @Test fun `addToList adds an item to the list`() = runTest {
        val repo = repository()
        repo.create("My List")
        val uuid = repo.getAllLists().first()[0].item.uuid

        val added = repo.addToList(
            uuid = uuid,
            title = "Title",
            description = "Description",
            url = "https://example.com/1",
            imageUrl = "https://example.com/1.jpg",
            source = "ExampleService",
        )

        assertTrue(added)
        val list = repo.getAllLists().first()[0]
        assertEquals(1, list.list.size)
        assertEquals("https://example.com/1", list.list[0].url)
    }

    @Test fun `removeList hard-deletes when logged out`() = runTest {
        val repo = repository(loggedIn = false)
        repo.create("My List")
        val item = repo.getAllLists().first()[0]

        repo.removeList(item)

        assertTrue(repo.getAllLists().first().isEmpty())
    }

    @Test fun `removeList soft-deletes when logged in`() = runTest {
        val repo = repository(loggedIn = true)
        repo.create("My List")
        val list = repo.getAllLists().first()[0]
        val uuid = list.item.uuid
        repo.addToList(
            uuid = uuid,
            title = "Title",
            description = "Description",
            url = "https://example.com/1",
            imageUrl = "https://example.com/1.jpg",
            source = "ExampleService",
        )
        val item = repo.getAllLists().first()[0]

        repo.removeList(item)

        // soft-deleted row still exists but is not returned by the is_deleted = 0 filter
        assertTrue(repo.getAllLists().first().isEmpty())

        val itemRow = database.listDao().getCustomListItemByUuid(uuid)
        assertNotNull(itemRow)
        assertTrue(itemRow.isDeleted)

        val infoUniqueId = item.list[0].uniqueId
        val infoRow = database.listDao().getCustomListInfoByUniqueId(infoUniqueId)
        assertNotNull(infoRow)
        assertTrue(infoRow.isDeleted)
    }

    @Test fun `updateBiometric flips the flag`() = runTest {
        val repo = repository()
        repo.create("My List")
        val uuid = repo.getAllLists().first()[0].item.uuid

        repo.updateBiometric(uuid, true)

        val item = database.listDao().getCustomListItemByUuid(uuid)
        assertNotNull(item)
        assertTrue(item.useBiometric)
    }

    @Test fun `updateBiometric can flip the flag back off`() = runTest {
        val repo = repository()
        repo.create("My List")
        val uuid = repo.getAllLists().first()[0].item.uuid
        repo.updateBiometric(uuid, true)

        repo.updateBiometric(uuid, false)

        val item = database.listDao().getCustomListItemByUuid(uuid)
        assertNotNull(item)
        assertFalse(item.useBiometric)
    }
}
