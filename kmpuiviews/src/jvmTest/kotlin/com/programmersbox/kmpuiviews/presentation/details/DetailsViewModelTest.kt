package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.BlurHashDatabase
import com.programmersbox.favoritesdatabase.BookmarkDatabase
import com.programmersbox.favoritesdatabase.ExceptionDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.repository.BookmarkRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.FakeKmpGenericInfo
import com.programmersbox.kmpuiviews.testing.FakeTranslationHandler
import com.programmersbox.kmpuiviews.utils.Cached
import com.programmersbox.kmpuiviews.utils.ImageModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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

class DetailsViewModelTest {

    private val viewModelStore = ViewModelStore()

    private lateinit var itemDbFile: File
    private lateinit var itemDatabase: ItemDatabase

    private lateinit var bookmarkDbFile: File
    private lateinit var bookmarkDatabase: BookmarkDatabase

    private lateinit var blurHashDbFile: File
    private lateinit var blurHashDatabase: BlurHashDatabase

    private lateinit var exceptionDbFile: File
    private lateinit var exceptionDatabase: ExceptionDatabase

    private val sourceRepository = SourceRepository()

    // The ViewModel observes several Room-generated Flows, which emit on Room's own
    // (real, non-test-controlled) dispatcher. A test-dispatcher virtual-clock advance
    // doesn't drive those emissions, so wait for them with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun details(source: String = "ExampleService") = Screen.DetailsScreen.Details(
        title = "Example",
        description = "Example",
        url = "https://example.com/",
        imageUrl = "https://picsum.photos/200/300",
        source = source,
    )

    private fun viewModel(details: Screen.DetailsScreen.Details? = details()) = DetailsViewModel(
        details = details,
        genericInfo = FakeKmpGenericInfo(),
        blurHashDao = blurHashDatabase.blurDao(),
        sourceRepository = sourceRepository,
        favoritesRepository = FavoritesRepository(
            dao = itemDatabase.itemDao(),
            systemAlerter = SystemAlerter(),
            authManager = FakeAuthManager(),
        ),
        translationHandler = FakeTranslationHandler(),
        exceptionDao = exceptionDatabase.exceptionDao(),
        imageModifier = ImageModifier(),
        bookmarkRepository = BookmarkRepository(
            dao = bookmarkDatabase.bookmarkDao(),
            authManager = FakeAuthManager(),
        ),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)

        sourceRepository.setSources(listOf(ExampleService.getSourceInformation()))

        itemDbFile = File.createTempFile("details-vm-item-test", ".db").also { it.deleteOnExit() }
        itemDatabase = Room.databaseBuilder<ItemDatabase>(name = itemDbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()

        bookmarkDbFile = File.createTempFile("details-vm-bookmark-test", ".db").also { it.deleteOnExit() }
        bookmarkDatabase = Room.databaseBuilder<BookmarkDatabase>(name = bookmarkDbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()

        blurHashDbFile = File.createTempFile("details-vm-blurhash-test", ".db").also { it.deleteOnExit() }
        blurHashDatabase = Room.databaseBuilder<BlurHashDatabase>(name = blurHashDbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()

        exceptionDbFile = File.createTempFile("details-vm-exception-test", ".db").also { it.deleteOnExit() }
        exceptionDatabase = Room.databaseBuilder<ExceptionDatabase>(name = exceptionDbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        runBlocking { Cached.cache.cleanUp() }
        itemDatabase.close()
        itemDbFile.delete()
        bookmarkDatabase.close()
        bookmarkDbFile.delete()
        blurHashDatabase.close()
        blurHashDbFile.delete()
        exceptionDatabase.close()
        exceptionDbFile.delete()
    }

    @Test
    fun `uses cached info instead of refetching from source`() = runTest {
        val service = ExampleService()
        val cachedInfo = service.itemInfo(
            KmpItemModel(
                title = "Cached Title",
                description = "Cached Description",
                url = "https://example.com/",
                imageUrl = "https://picsum.photos/200/300",
                source = service,
            )
        ).copy(title = "Cached Title")
        Cached.cache["https://example.com/"] = cachedInfo

        val vm = viewModel()

        awaitCondition { vm.currentState is DetailState.Success }

        val state = vm.currentState as DetailState.Success
        assertEquals("Cached Title", state.info.title)
    }

    @Test
    fun `favoriteAction Add persists favorite and flips currentState to Remove`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val addAction = (vm.currentState as DetailState.Success).action as DetailFavoriteAction.Add
        vm.favoriteAction(addAction)

        awaitCondition { vm.favoriteListener }

        assertTrue(itemDatabase.itemDao().getDbModelSync("https://example.com/") != null)
        assertTrue((vm.currentState as DetailState.Success).action is DetailFavoriteAction.Remove)
    }

    @Test
    fun `favoriteAction Remove deletes favorite and flips currentState back to Add`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val addAction = (vm.currentState as DetailState.Success).action as DetailFavoriteAction.Add
        vm.favoriteAction(addAction)
        awaitCondition { vm.favoriteListener }

        val removeAction = (vm.currentState as DetailState.Success).action as DetailFavoriteAction.Remove
        vm.favoriteAction(removeAction)
        awaitCondition { !vm.favoriteListener }

        assertTrue(itemDatabase.itemDao().getDbModelSync("https://example.com/") == null)
        assertTrue((vm.currentState as DetailState.Success).action is DetailFavoriteAction.Add)
    }

    @Test
    fun `toggleBookmark adds then removes a bookmark for a chapter`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val chapter = (vm.currentState as DetailState.Success).info.chapters.first()

        vm.toggleBookmark(chapter)
        awaitCondition { vm.bookmarkedChapterUrls.contains(chapter.url) }
        assertTrue(chapter.url in vm.bookmarkedChapterUrls)

        vm.toggleBookmark(chapter)
        awaitCondition { !vm.bookmarkedChapterUrls.contains(chapter.url) }
        assertTrue(chapter.url !in vm.bookmarkedChapterUrls)
    }

    @Test
    fun `markAs true adds a watched chapter and reread clears it`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val chapter = (vm.currentState as DetailState.Success).info.chapters.first()

        vm.markAs(chapter, true)
        awaitCondition { vm.chapters.any { it.url == chapter.url } }
        assertTrue(vm.chapters.any { it.url == chapter.url })

        vm.reread()
        awaitCondition { vm.chapters.isEmpty() }
        assertTrue(vm.chapters.isEmpty())
    }

    @Test
    fun `translateDescription updates description via TranslationHandler`() = runTest {
        val vm = viewModel()
        awaitCondition { vm.currentState is DetailState.Success }

        val progress = mutableStateOf(false)
        vm.translateDescription(progress)

        awaitCondition { !progress.value }
        assertEquals("Example", vm.description)
    }

    @Test
    fun `null details produces null itemModel and Loading never resolves`() = runTest {
        val vm = viewModel(details = null)

        assertEquals(null, vm.itemModel)
        assertEquals(DetailState.Loading, vm.currentState)
    }
}
