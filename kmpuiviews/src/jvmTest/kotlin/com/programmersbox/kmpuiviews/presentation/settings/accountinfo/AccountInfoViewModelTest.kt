package com.programmersbox.kmpuiviews.presentation.settings.accountinfo

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.BlurHashDatabase
import com.programmersbox.favoritesdatabase.BookmarkDatabase
import com.programmersbox.favoritesdatabase.DictionaryDatabase
import com.programmersbox.favoritesdatabase.ExceptionDatabase
import com.programmersbox.favoritesdatabase.HeatMapDatabase
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.favoritesdatabase.NotesDatabase
import com.programmersbox.favoritesdatabase.RecommendationDatabase
import com.programmersbox.favoritesdatabase.SettingsDatabase
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

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
    private lateinit var settingsDatabase: SettingsDatabase
    private lateinit var dictionaryDatabase: DictionaryDatabase


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
        heatMapDao = heatMapDatabase.heatMapDao(),
        activityDao = settingsDatabase.activityDao(),
        authManager = FakeAuthManager(),
        providers = emptyList()
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
        settingsDatabase = tempDatabase("account-info-settings-test") {
            Room.databaseBuilder<SettingsDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
        dictionaryDatabase = tempDatabase("account-info-dictionary-test") {
            Room.databaseBuilder<DictionaryDatabase>(name = it).setDriver(BundledSQLiteDriver()).build()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
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
}
