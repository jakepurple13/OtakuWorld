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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ListDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: ListDatabase
    private lateinit var dao: ListDao

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("list-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ListDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.listDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `create then getAllLists shows the new list with an empty item list`() = runTest {
        dao.create("My List")

        val lists = dao.getAllLists().first()

        assertEquals(1, lists.size)
        assertEquals("My List", lists[0].item.name)
        assertTrue(lists[0].list.isEmpty())
    }

    @Test fun `addToList adds an item and returns true`() = runTest {
        dao.create("My List")
        val uuid = dao.getAllLists().first()[0].item.uuid

        val added = dao.addToList(
            uuid = uuid,
            title = "Title",
            description = "Description",
            url = "https://example.com/1",
            imageUrl = "https://example.com/1.jpg",
            source = "ExampleService",
        )

        assertTrue(added)
        val list = dao.getAllLists().first()[0]
        assertEquals(1, list.list.size)
        assertEquals("https://example.com/1", list.list[0].url)
    }

    @Test fun `addToList with the same uuid and url returns false and does not duplicate`() = runTest {
        dao.create("My List")
        val uuid = dao.getAllLists().first()[0].item.uuid
        dao.addToList(
            uuid = uuid,
            title = "Title",
            description = "Description",
            url = "https://example.com/1",
            imageUrl = "https://example.com/1.jpg",
            source = "ExampleService",
        )

        val addedAgain = dao.addToList(
            uuid = uuid,
            title = "Title",
            description = "Description",
            url = "https://example.com/1",
            imageUrl = "https://example.com/1.jpg",
            source = "ExampleService",
        )

        assertFalse(addedAgain)
        val list = dao.getAllLists().first()[0]
        assertEquals(1, list.list.size)
    }

    @Test fun `updateBiometric flips the flag`() = runTest {
        dao.create("My List")
        val uuid = dao.getAllLists().first()[0].item.uuid

        dao.updateBiometric(uuid, true)

        val item = dao.getCustomListItemByUuid(uuid)
        assertNotNull(item)
        assertTrue(item.useBiometric)
    }

    @Test fun `removeList removes the list row`() = runTest {
        dao.create("My List")
        val item = dao.getAllLists().first()[0].item

        dao.removeList(item)

        assertTrue(dao.getAllLists().first().isEmpty())
    }

    @Test fun `getCustomListItemFlow emits null once the list is gone`() = runTest {
        dao.create("My List")
        val list = dao.getAllLists().first()[0]

        dao.removeList(list)

        assertEquals(null, dao.getCustomListItemFlow(list.item.uuid).first())
        assertEquals(null, dao.getCustomListItem(list.item.uuid))
    }

    @Test fun `addToList on a missing list returns false`() = runTest {
        assertFalse(dao.addToList("missing", "T", "D", "https://example.com/1", "", "Src"))
    }

    @Test fun `removeList with items removes the items too`() = runTest {
        dao.create("My List")
        val uuid = dao.getAllLists().first()[0].item.uuid
        dao.addToList(uuid, "T", "D", "https://example.com/1", "", "Src")

        dao.removeList(dao.getCustomListItem(uuid)!!)

        assertTrue(dao.getAllCustomListInfoSync().isEmpty())
        assertTrue(dao.getAllCustomListItemsSync().isEmpty())
    }

    @Test fun `list edits mark the list dirty with a newer timestamp`() = runTest {
        dao.createList(CustomListItem(uuid = "uuid", name = "name", updatedAt = 100L, isDirty = false))

        dao.updateFullList(dao.getCustomListItemByUuid("uuid")!!.copy(name = "Renamed"))
        val renamed = dao.getCustomListItemByUuid("uuid")!!
        assertTrue(renamed.isDirty)
        assertTrue(renamed.updatedAt > 100L)

        dao.markCustomListItemSynced("uuid", timestamp = renamed.updatedAt, pushedUpdatedAt = renamed.updatedAt)
        dao.updateBiometric("uuid", true, timestamp = renamed.updatedAt + 1)
        val locked = dao.getCustomListItemByUuid("uuid")!!
        assertTrue(locked.isDirty)
        assertEquals(renamed.updatedAt + 1, locked.updatedAt)
    }
}
