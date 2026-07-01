package com.programmersbox.kmpuiviews.presentation.history

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.RecentModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: HistoryDatabase

    private fun recent(url: String, title: String = "Title", timestamp: Long = 0L) = RecentModel(
        title = title,
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
        timestamp = timestamp,
    )

    private fun viewModel() = HistoryViewModel(dao = database.historyDao()).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("history-viewmodel-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<HistoryDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        database.close()
        dbFile.delete()
    }

    @Test fun `historyCount starts at zero`() = runTest {
        val vm = viewModel()

        assertEquals(0, vm.historyCount.first())
    }

    @Test fun `historyCount reflects inserted recently viewed items`() = runTest {
        val dao = database.historyDao()
        dao.insertRecentlyViewed(recent("https://example.com/1"))
        dao.insertRecentlyViewed(recent("https://example.com/2"))

        val vm = viewModel()

        assertEquals(2, vm.historyCount.first())
    }

    @Test fun `historyCount updates after a recent item is deleted`() = runTest {
        val dao = database.historyDao()
        val item = recent("https://example.com/1")
        dao.insertRecentlyViewed(item)

        val vm = viewModel()
        assertEquals(1, vm.historyCount.first())

        dao.deleteRecent(item)

        assertEquals(0, vm.historyCount.first())
    }
}
