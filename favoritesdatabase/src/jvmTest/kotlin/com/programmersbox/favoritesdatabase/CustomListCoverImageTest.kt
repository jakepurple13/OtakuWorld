package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomListCoverImageTest {

    private fun createTestListDatabase(): ListDatabase {
        val dbFile = File.createTempFile("list-cover-test", ".db").also { it.deleteOnExit() }
        return Room.databaseBuilder<ListDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    private fun customListInfo(uuid: String, title: String, imageUrl: String) = CustomListInfo(
        uuid = uuid,
        title = title,
        description = "",
        url = "url/$title",
        imageUrl = imageUrl,
        source = "source",
    )

    @Test
    fun `resolvedCoverImageUrl falls back to first item when coverImageUrl is null`() {
        val customList = CustomList(
            item = CustomListItem(uuid = "uuid", name = "name", coverImageUrl = null),
            list = listOf(
                customListInfo("uuid", "first", "first.jpg"),
                customListInfo("uuid", "second", "second.jpg"),
            ),
        )

        assertEquals("first.jpg", customList.resolvedCoverImageUrl())
    }

    @Test
    fun `resolvedCoverImageUrl falls back to first item when coverImageUrl is empty`() {
        val customList = CustomList(
            item = CustomListItem(uuid = "uuid", name = "name", coverImageUrl = ""),
            list = listOf(customListInfo("uuid", "first", "first.jpg")),
        )

        assertEquals("first.jpg", customList.resolvedCoverImageUrl())
    }

    @Test
    fun `resolvedCoverImageUrl uses custom cover when set`() {
        val customList = CustomList(
            item = CustomListItem(uuid = "uuid", name = "name", coverImageUrl = "custom.jpg"),
            list = listOf(customListInfo("uuid", "first", "first.jpg")),
        )

        assertEquals("custom.jpg", customList.resolvedCoverImageUrl())
    }

    @Test
    fun `resolvedCoverImageUrl returns empty string when list is empty and no custom cover`() {
        val customList = CustomList(
            item = CustomListItem(uuid = "uuid", name = "name", coverImageUrl = null),
            list = emptyList(),
        )

        assertEquals("", customList.resolvedCoverImageUrl())
    }

    @Test
    fun `updateCoverImageUrl sets cover, marks dirty, and updates timestamp`() = runTest {
        val db = createTestListDatabase()
        val dao = db.listDao()

        dao.createList(CustomListItem(uuid = "uuid", name = "name", isDirty = false))
        dao.markCustomListItemSynced("uuid", timestamp = 100L)

        dao.updateCoverImageUrl(uuid = "uuid", coverImageUrl = "cover.jpg", timestamp = 200L)

        val updated = dao.getCustomListItemByUuid("uuid")
        assertEquals("cover.jpg", updated?.coverImageUrl)
        assertTrue(updated?.isDirty == true)
        assertEquals(200L, updated?.updatedAt)
    }

    @Test
    fun `updateCoverImageUrl only touches the targeted list`() = runTest {
        val db = createTestListDatabase()
        val dao = db.listDao()

        dao.createList(CustomListItem(uuid = "uuid-a", name = "a"))
        dao.createList(CustomListItem(uuid = "uuid-b", name = "b"))

        dao.updateCoverImageUrl(uuid = "uuid-a", coverImageUrl = "cover.jpg", timestamp = 100L)

        assertEquals("cover.jpg", dao.getCustomListItemByUuid("uuid-a")?.coverImageUrl)
        assertFalse(dao.getCustomListItemByUuid("uuid-b")?.coverImageUrl == "cover.jpg")
    }
}
