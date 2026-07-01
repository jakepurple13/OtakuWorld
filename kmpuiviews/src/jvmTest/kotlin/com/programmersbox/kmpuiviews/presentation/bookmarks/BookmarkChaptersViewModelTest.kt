package com.programmersbox.kmpuiviews.presentation.bookmarks

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.BookmarkDatabase
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.kmpuiviews.repository.BookmarkRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookmarkChaptersViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: BookmarkDatabase

    // The ViewModel observes BookmarkDao's Room-generated Flow, which emits on Room's own
    // (real, non-test-controlled) dispatcher. A test-dispatcher virtual-clock advance
    // doesn't drive that emission, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun bookmark(
        chapterUrl: String,
        chapterName: String = "Chapter 1",
        parentUrl: String = "https://example.com/series",
        parentTitle: String = "Series Title",
    ) = BookmarkedChapter(
        chapterUrl = chapterUrl,
        chapterName = chapterName,
        parentUrl = parentUrl,
        parentTitle = parentTitle,
        parentImageUrl = "https://example.com/series.jpg",
        source = "ExampleService",
    )

    private fun viewModel(authManager: FakeAuthManager = FakeAuthManager()) = BookmarkChaptersViewModel(
        bookmarkRepository = BookmarkRepository(
            dao = database.bookmarkDao(),
            authManager = authManager,
        ),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("bookmark-chapters-vm-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<BookmarkDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        viewModelStore.clear()
        database.close()
        dbFile.delete()
    }

    @Test fun `starts with no bookmarks`() = runTest {
        val vm = viewModel()
        val __sub = backgroundScope.launch { vm.bookmarks.collect {} }
        assertTrue(vm.bookmarks.value.isEmpty())
    }

    @Test fun `inserted bookmark shows up grouped by parentTitle`() = runTest {
        database.bookmarkDao().insertBookmark(bookmark("https://example.com/series/ch1"))

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.bookmarks.collect {} }
        awaitCondition { vm.bookmarks.value.isNotEmpty() }

        assertEquals(1, vm.bookmarks.value.size)
        assertEquals(listOf("Chapter 1"), vm.bookmarks.value["Series Title"]?.map { it.chapterName })
    }

    @Test fun `searchQuery filters bookmarks via fts`() = runTest {
        val dao = database.bookmarkDao()
        dao.insertBookmark(bookmark("https://example.com/series-a/ch1", chapterName = "Dragon Fight", parentUrl = "https://example.com/series-a", parentTitle = "Amazing Series"))
        dao.insertBookmark(bookmark("https://example.com/series-b/ch1", chapterName = "Calm Morning", parentUrl = "https://example.com/series-b", parentTitle = "Other Story"))

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.bookmarks.collect {} }
        awaitCondition { vm.bookmarks.value.size == 2 }

        vm.searchQuery = "Dragon"
        awaitCondition { vm.bookmarks.value.size == 1 }

        assertTrue("Amazing Series" in vm.bookmarks.value)
    }

    @Test fun `sortOrder TITLE_AZ sorts chapters within a group by name`() = runTest {
        val dao = database.bookmarkDao()
        dao.insertBookmark(bookmark("https://example.com/series/ch2", chapterName = "Zeta"))
        dao.insertBookmark(bookmark("https://example.com/series/ch1", chapterName = "Alpha"))

        val vm = viewModel()

        val __sub = backgroundScope.launch { vm.bookmarks.collect {} }
        awaitCondition { vm.bookmarks.value["Series Title"]?.size == 2 }

        vm.sortOrder = BookmarkSortOrder.TITLE_AZ
        awaitCondition {
            vm.bookmarks.value["Series Title"]?.map { it.chapterName } == listOf("Alpha", "Zeta")
        }

        assertEquals(listOf("Alpha", "Zeta"), vm.bookmarks.value["Series Title"]?.map { it.chapterName })
    }

    @Test fun `removeBookmark hard deletes when not logged in`() = runTest {
        val dao = database.bookmarkDao()
        dao.insertBookmark(bookmark("https://example.com/series/ch1"))

        val vm = viewModel(authManager = FakeAuthManager(loggedIn = false))

        val __sub = backgroundScope.launch { vm.bookmarks.collect {} }
        awaitCondition { vm.bookmarks.value.isNotEmpty() }

        vm.removeBookmark("https://example.com/series/ch1")
        awaitCondition { dao.getAllBookmarksSync().isEmpty() }

        assertTrue(dao.getAllBookmarksSync().isEmpty())
    }
}
