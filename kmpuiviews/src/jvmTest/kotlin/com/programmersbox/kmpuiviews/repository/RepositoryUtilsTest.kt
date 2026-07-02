package com.programmersbox.kmpuiviews.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryUtilsTest {

    private lateinit var dbFile: File
    private lateinit var database: ItemDatabase
    private lateinit var dao: ItemDao

    private fun sourceInfo(
        name: String,
        packageName: String = name,
        apiService: KmpApiService = ExampleService(),
    ) = KmpSourceInformation(
        apiService = apiService,
        name = name,
        icon = null,
        packageName = packageName,
    )

    private class NotWorkingService : KmpApiService {
        override val baseUrl: String = "https://example.com/"
        override val notWorking: Boolean = true
    }

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("repository-utils-test", ".db").also { it.deleteOnExit() }
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

    @Test fun `notWorking sources are filtered out`() = runTest {
        val sourceRepository = SourceRepository()
        sourceRepository.setSources(
            listOf(
                sourceInfo("Working", packageName = "com.working"),
                sourceInfo("Broken", packageName = "com.broken", apiService = NotWorkingService()),
            )
        )

        combineSources(sourceRepository, dao).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("com.working", list[0].packageName)
        }
    }

    @Test fun `sources sort by persisted order`() = runTest {
        val sourceRepository = SourceRepository()
        sourceRepository.setSources(
            listOf(
                sourceInfo("A", packageName = "com.a"),
                sourceInfo("B", packageName = "com.b"),
            )
        )
        dao.insertSourceOrder(SourceOrder(source = "com.a", name = "A", order = 2))
        dao.insertSourceOrder(SourceOrder(source = "com.b", name = "B", order = 1))

        combineSources(sourceRepository, dao).test {
            val list = awaitItem()
            assertEquals(listOf("com.b", "com.a"), list.map { it.packageName })
        }
    }

    @Test fun `source with no matching order entry falls back to position 0`() = runTest {
        val sourceRepository = SourceRepository()
        sourceRepository.setSources(
            listOf(
                sourceInfo("A", packageName = "com.a"),
                sourceInfo("B", packageName = "com.b"),
                sourceInfo("C", packageName = "com.c"),
            )
        )
        // Only "com.b" has a persisted order; "com.a" and "com.c" fall back to 0 and keep
        // their relative order from the source list (sortedBy is stable).
        dao.insertSourceOrder(SourceOrder(source = "com.b", name = "B", order = -1))

        combineSources(sourceRepository, dao).test {
            val list = awaitItem()
            assertEquals(listOf("com.b", "com.a", "com.c"), list.map { it.packageName })
        }
    }
}
