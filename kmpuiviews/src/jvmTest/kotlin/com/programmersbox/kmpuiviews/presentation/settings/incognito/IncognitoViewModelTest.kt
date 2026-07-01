package com.programmersbox.kmpuiviews.presentation.settings.incognito

import androidx.lifecycle.ViewModelStore
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.repository.IncognitoRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IncognitoViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var database: ItemDatabase

    // The ViewModel observes ItemDao's Room-generated Flow, which emits on Room's own
    // (real, non-test-controlled) dispatcher. A test-dispatcher virtual-clock advance
    // doesn't drive that emission, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun viewModel(sourceRepository: SourceRepository = SourceRepository()) = IncognitoViewModel(
        itemDao = database.itemDao(),
        sourceRepository = sourceRepository,
        incognitoRepository = IncognitoRepository(
            dao = database.itemDao(),
            systemAlerter = SystemAlerter(),
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
        Dispatchers.resetMain()
        viewModelStore.clear()
        database.close()
    }

    @Test fun `starts with no incognito models when there are no sources`() = runTest {
        val vm = viewModel()

        assertTrue(vm.incognitoModels.isEmpty())
    }

    @Test fun `working sources show up in incognitoModels`() = runTest {
        val sourceRepository = SourceRepository().apply {
            setSources(listOf(ExampleService.getSourceInformation()))
        }

        val vm = viewModel(sourceRepository)
        awaitCondition { vm.incognitoModels.isNotEmpty() }

        assertEquals(1, vm.incognitoModels.size)
        assertEquals("com.example", vm.incognitoModels[0].sourceInformation.packageName)
        assertFalse(vm.incognitoModels[0].incognitoSource.isIncognito)
    }

    @Test fun `toggleIncognito adds a new incognito source when none exists yet`() = runTest {
        val sourceRepository = SourceRepository().apply {
            setSources(listOf(ExampleService.getSourceInformation()))
        }
        val vm = viewModel(sourceRepository)
        awaitCondition { vm.incognitoModels.isNotEmpty() }

        vm.toggleIncognito(ExampleService.getSourceInformation(), true)
        awaitCondition { vm.incognitoModels.any { it.incognitoSource.isIncognito } }

        assertTrue(vm.incognitoModels[0].incognitoSource.isIncognito)
    }

    @Test fun `toggleIncognito updates an existing incognito source`() = runTest {
        val sourceRepository = SourceRepository().apply {
            setSources(listOf(ExampleService.getSourceInformation()))
        }
        val vm = viewModel(sourceRepository)
        awaitCondition { vm.incognitoModels.isNotEmpty() }

        vm.toggleIncognito(ExampleService.getSourceInformation(), true)
        awaitCondition { vm.incognitoModels.any { it.incognitoSource.isIncognito } }

        vm.toggleIncognito(ExampleService.getSourceInformation(), false)
        awaitCondition { vm.incognitoModels.none { it.incognitoSource.isIncognito } }

        assertEquals(1, vm.incognitoModels.size)
        assertFalse(vm.incognitoModels[0].incognitoSource.isIncognito)
    }
}
