package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.lifecycle.ViewModelStore
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var database: ItemDatabase

    // The ViewModel observes ItemDao's Room-generated Flow, which emits on Room's own
    // (real, non-test-controlled) dispatcher. A test-dispatcher virtual-clock advance
    // doesn't drive that emission, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(10_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun favorite(url: String, title: String = "Title $url") = DbModel(
        title = title,
        description = "Description",
        url = url,
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
    )

    private fun viewModel() = FavoriteViewModel(
        sourceRepository = SourceRepository(),
        favoritesRepository = FavoritesRepository(
            dao = database.itemDao(),
            systemAlerter = SystemAlerter(),
            authManager = FakeAuthManager(),
        ),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        database = createTestItemDatabase()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `inserted favorite shows up in listSources after collection`() = runTest {
        val dao = database.itemDao()
        dao.insertFavorite(favorite("https://example.com/1"))

        val items = dao.getAllFavoritesSync()
        awaitCondition { items.isNotEmpty() }

        assertEquals(1, items.size)
        assertEquals("Title https://example.com/1", items[0].title)
    }
}
