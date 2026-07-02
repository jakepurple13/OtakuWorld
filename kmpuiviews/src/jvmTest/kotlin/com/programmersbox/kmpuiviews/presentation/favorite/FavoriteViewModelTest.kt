package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.lifecycle.ViewModelStore
import androidx.compose.foundation.text.input.TextFieldState
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.domain.customserver.ServerRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.testing.FakeAuthManager
import com.programmersbox.kmpuiviews.testing.FakeKmpFirebaseConnection
import com.programmersbox.kmpuiviews.testing.FakeKmpFirebaseListener
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
import kotlin.test.assertTrue

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
            firebaseDb = FakeKmpFirebaseConnection(),
            serverRepository = ServerRepository(),
            systemAlerter = SystemAlerter(),
            authManager = FakeAuthManager(),
        ),
        firebaseFavoriteListener = FakeKmpFirebaseListener(),
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

    @Test fun `starts with no favorites selected`() = runTest {
        val vm = viewModel()

        assertTrue(vm.listSources.isEmpty())
    }

    @Test fun `inserted favorite shows up in listSources after collection`() = runTest {
        val dao = database.itemDao()
        dao.insertFavorite(favorite("https://example.com/1"))

        val vm = viewModel()
        awaitCondition { vm.listSources.isNotEmpty() }

        assertEquals(1, vm.listSources.size)
        assertEquals("Title https://example.com/1", vm.listSources[0].title)
        assertTrue("ExampleService" in vm.selectedSources)
    }

    @Test fun `searchText filters listSources by title`() = runTest {
        val dao = database.itemDao()
        dao.insertFavorite(favorite("https://example.com/1", title = "Alpha"))
        dao.insertFavorite(favorite("https://example.com/2", title = "Beta"))

        val vm = viewModel()
        awaitCondition { vm.listSources.size == 2 }

        vm.searchText = TextFieldState("alp")
        assertEquals(1, vm.listSources.size)
        assertEquals("Alpha", vm.listSources[0].title)
    }

    @Test fun `newSource toggles membership in selectedSources`() = runTest {
        val vm = viewModel()

        vm.newSource("SomeSource")
        assertTrue("SomeSource" in vm.selectedSources)

        vm.newSource("SomeSource")
        assertTrue("SomeSource" !in vm.selectedSources)
    }
}
