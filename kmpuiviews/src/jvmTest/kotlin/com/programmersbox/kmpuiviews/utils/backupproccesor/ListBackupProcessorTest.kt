package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.sharedtools.ProcessorResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Buffer
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class ThrowingOnNameListDao(
    private val delegate: ListDao,
    private val throwForName: String,
) : ListDao by delegate {
    override suspend fun createList(listItem: CustomListItem): Long {
        if (listItem.name == throwForName) throw RuntimeException("boom: ${listItem.name}")
        return delegate.createList(listItem)
    }
}

class ListBackupProcessorTest {

    private lateinit var dbFile: File
    private lateinit var database: ListDatabase

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("list-backup-processor-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ListDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    private fun customList(name: String) = CustomList(
        item = CustomListItem(uuid = name, name = name),
        list = listOf(
            CustomListInfo(
                uuid = name,
                title = "Title-$name",
                description = "Description",
                url = "https://example.com/$name",
                imageUrl = "https://example.com/$name.jpg",
                source = "ExampleService",
            )
        ),
    )

    @Test
    fun `restore skips a failing list and imports the rest`() = runTest {
        val throwingDao = ThrowingOnNameListDao(database.listDao(), throwForName = "bad-list")
        val repository = ListRepository(throwingDao, SystemAlerter(), FakeAuthManager())
        val processor = ListBackupProcessor(repository, database.listDao())

        val json = Json.encodeToString(listOf(customList("good-list"), customList("bad-list")))

        val result = processor.restore(json, Buffer())

        assertEquals(ProcessorResult(successCount = 1, failed = listOf("bad-list")), result)
        val stored = database.listDao().getAllListsSync()
        assertEquals(listOf("good-list"), stored.map { it.item.name })
    }

    @Test
    fun `backup only includes lists whose uuid is in listIdFilter`() = runTest {
        val repository = ListRepository(database.listDao(), SystemAlerter(), FakeAuthManager())
        repository.create("keep-me")
        repository.create("drop-me")
        val keepUuid = database.listDao().getAllListsSync().first { it.item.name == "keep-me" }.item.uuid

        val processor = ListBackupProcessor(repository, database.listDao())
        processor.listIdFilter = setOf(keepUuid)

        val sink = Buffer()
        val result = processor.backup(sink)

        assertEquals(1, result.successCount)
        assertTrue(sink.readUtf8().contains("keep-me"))
    }

    @Test
    fun `backup includes every list when listIdFilter is null`() = runTest {
        val repository = ListRepository(database.listDao(), SystemAlerter(), FakeAuthManager())
        repository.create("list-a")
        repository.create("list-b")

        val processor = ListBackupProcessor(repository, database.listDao())

        val result = processor.backup(Buffer())

        assertEquals(2, result.successCount)
    }

    @Test
    fun `parseLists deserializes a raw lists json blob`() = runTest {
        val repository = ListRepository(database.listDao(), SystemAlerter(), FakeAuthManager())
        val processor = ListBackupProcessor(repository, database.listDao())
        val json = Json.encodeToString(listOf(customList("some-list")))

        val parsed = processor.parseLists(json)

        assertEquals(listOf("some-list"), parsed.map { it.item.name })
    }
}
