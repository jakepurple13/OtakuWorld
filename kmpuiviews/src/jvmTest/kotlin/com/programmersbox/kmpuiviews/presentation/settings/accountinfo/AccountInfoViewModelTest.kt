package com.programmersbox.kmpuiviews.presentation.settings.accountinfo

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.BlurHashDatabase
import com.programmersbox.favoritesdatabase.BookmarkDatabase
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ExceptionDatabase
import com.programmersbox.favoritesdatabase.HeatMapDatabase
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.favoritesdatabase.NoteItem
import com.programmersbox.favoritesdatabase.NotesDatabase
import com.programmersbox.favoritesdatabase.RecommendationDatabase
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.FakeKmpFirebaseListener
import com.programmersbox.kmpuiviews.testing.FakeTranslationModelHandler
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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

class AccountInfoViewModelTest {

    private val viewModelStore = ViewModelStore()

    private lateinit var itemDatabase: ItemDatabase
    private lateinit var listDatabase: ListDatabase
    private lateinit var historyDatabase: HistoryDatabase
    private lateinit var blurHashDatabase: BlurHashDatabase
    private lateinit var heatMapDatabase: HeatMapDatabase
    private lateinit var recommendationDatabase: RecommendationDatabase
    private lateinit var exceptionDatabase: ExceptionDatabase
    private lateinit var bookmarkDatabase: BookmarkDatabase
    private lateinit var notesDatabase: NotesDatabase

    private val showsFlow = MutableStateFlow<List<DbModel>>(emptyList())
    private val firebaseListener = FakeKmpFirebaseListener(showsFlow)

    // The ViewModel observes several Room-generated Flows, which emit on Room's own
    // (real, non-test-controlled) dispatcher. A test-dispatcher virtual-clock advance
    // doesn't drive that emission, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel(
        sourceRepository: SourceRepository = SourceRepository(),
    ) = AccountInfoViewModel(
        itemDao = itemDatabase.itemDao(),
        listDao = listDatabase.listDao(),
        historyDao = historyDatabase.historyDao(),
        blurHashDao = blurHashDatabase.blurDao(),
        heatMapDao = heatMapDatabase.heatMapDao(),
        translationModelHandler = FakeTranslationModelHandler(),
        sourceRepository = sourceRepository,
        firebaseConnection = firebaseListener,
        dataStoreHandling = createTestDataStoreHandling(),
        recommendationDao = recommendationDatabase.recommendationDao(),
        exceptionDao = exceptionDatabase.exceptionDao(),
        bookmarksDao = bookmarkDatabase.bookmarkDao(),
        notesDao = notesDatabase.notesDao(),
        authManager = FakeAuthManager(),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    private inline fun <reified T : Any> tempDatabase(name: String, build: (String) -> T): T {
        val dbFile = File.createTempFile(name, ".db").also { it.deleteOnExit() }
        return build(dbFile.absolutePath)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        itemDatabase = tempDatabase("account-info-item-test") {
            Room.databaseBuilder<ItemDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
        listDatabase = tempDatabase("account-info-list-test") {
            Room.databaseBuilder<ListDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
        historyDatabase = tempDatabase("account-info-history-test") {
            Room.databaseBuilder<HistoryDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
        blurHashDatabase = tempDatabase("account-info-blurhash-test") {
            Room.databaseBuilder<BlurHashDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
        heatMapDatabase = tempDatabase("account-info-heatmap-test") {
            Room.databaseBuilder<HeatMapDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
        recommendationDatabase = tempDatabase("account-info-recommendation-test") {
            Room.databaseBuilder<RecommendationDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
        exceptionDatabase = tempDatabase("account-info-exception-test") {
            Room.databaseBuilder<ExceptionDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
        bookmarkDatabase = tempDatabase("account-info-bookmark-test") {
            Room.databaseBuilder<BookmarkDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
        notesDatabase = tempDatabase("account-info-notes-test") {
            Room.databaseBuilder<NotesDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
        itemDatabase.close()
        listDatabase.close()
        historyDatabase.close()
        blurHashDatabase.close()
        heatMapDatabase.close()
        recommendationDatabase.close()
        exceptionDatabase.close()
        bookmarkDatabase.close()
        notesDatabase.close()
    }

    @Test fun `starts with empty account info`() = runTest {
        val vm = viewModel()

        assertEquals(AccountInfoCount.Empty.totalFavorites, vm.accountInfo.totalFavorites)
        assertEquals(0, vm.accountInfo.notesCount)
    }

    @Test fun `favorite inserted before collection starts is reflected in localFavorites`() = runTest {
        itemDatabase.itemDao().insertFavorite(
            DbModel(
                title = "Title",
                description = "Description",
                url = "https://example.com/1",
                imageUrl = "https://example.com/1.jpg",
                source = "ExampleService",
            )
        )

        val vm = viewModel()
        awaitCondition { vm.accountInfo.localFavorites == 1 }

        assertEquals(1, vm.accountInfo.localFavorites)
        assertEquals(1, vm.accountInfo.totalFavorites)
    }

    @Test fun `cloud favorites from firebase listener are counted`() = runTest {
        showsFlow.value = listOf(
            DbModel(
                title = "Title",
                description = "Description",
                url = "https://example.com/1",
                imageUrl = "https://example.com/1.jpg",
                source = "ExampleService",
            )
        )

        val vm = viewModel()
        awaitCondition { vm.accountInfo.cloudFavorites == 1 }

        assertEquals(1, vm.accountInfo.cloudFavorites)
        assertEquals(1, vm.accountInfo.totalFavorites)
    }

    @Test fun `working sources are grouped by packageName and counted`() = runTest {
        val sourceRepository = SourceRepository().apply {
            setSources(listOf(ExampleService.getSourceInformation()))
        }

        val vm = viewModel(sourceRepository = sourceRepository)
        awaitCondition { vm.accountInfo.sourceCount == 1 }

        assertEquals(1, vm.accountInfo.sourceCount)
    }

    @Test fun `notes count reflects inserted notes`() = runTest {
        notesDatabase.notesDao().upsertNote(
            NoteItem(
                itemUrl = "https://example.com/1",
                itemTitle = "Title",
                content = "Content",
                timestamp = 0L,
            )
        )

        val vm = viewModel()
        awaitCondition { vm.accountInfo.notesCount == 1 }

        assertEquals(1, vm.accountInfo.notesCount)
    }
}
