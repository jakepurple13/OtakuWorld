package com.programmersbox.kmpuiviews.presentation.urlopener

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UrlOpenerViewModelTest {

    private val viewModelStore = ViewModelStore()

    private fun viewModel(sourceRepository: SourceRepository) = UrlOpenerViewModel(sourceRepository)
        .also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    // The ViewModel observes SourceRepository's Flow, which emits on a real
    // (non-test-controlled) dispatcher via launchIn(viewModelScope). A test-dispatcher
    // virtual-clock advance doesn't drive that emission, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private class FakeApiService(
        private val result: KmpItemModel? = null,
    ) : KmpApiService {
        override val baseUrl: String = "https://example.com/"
        override suspend fun sourceByUrl(url: String): KmpItemModel = result ?: error("Not setup")
    }

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    @Test fun `starts with no source list and no chosen source`() = runTest {
        val vm = viewModel(SourceRepository())

        assertTrue(vm.sourceList.isEmpty())
        assertNull(vm.currentChosenSource)
        assertNull(vm.kmpItemModel)
    }

    @Test fun `sources from repository populate sourceList grouped by packageName`() = runTest {
        val sourceRepository = SourceRepository()
        sourceRepository.setSources(listOf(sourceInfo("Example", packageName = "com.example")))

        val vm = viewModel(sourceRepository)
        awaitCondition { vm.sourceList.isNotEmpty() }

        assertEquals(1, vm.sourceList.size)
        assertEquals("Example", vm.sourceList["com.example"]?.single()?.name)
        assertEquals("Example", vm.currentChosenSource?.name)
    }

    @Test fun `notWorking sources are filtered out of sourceList`() = runTest {
        val notWorkingService = object : KmpApiService {
            override val baseUrl: String = "https://example.com/"
            override val notWorking: Boolean = true
        }
        val sourceRepository = SourceRepository()
        sourceRepository.setSources(listOf(sourceInfo("Broken", packageName = "com.broken", apiService = notWorkingService)))

        val vm = viewModel(sourceRepository)
        // Give the flow a moment to be collected; since nothing should ever show up, poll a fixed
        // number of times rather than waiting on a condition that's expected to stay false.
        repeat(20) { delay(10) }

        assertTrue(vm.sourceList.isEmpty())
        assertNull(vm.currentChosenSource)
    }

    @Test fun `open sets kmpItemModel from currentChosenSource's sourceByUrl result`() = runTest {
        val expected = KmpItemModel(
            title = "Title",
            description = "Description",
            url = "https://example.com/item",
            imageUrl = "https://example.com/item.jpg",
            source = ExampleService(),
        )
        val sourceRepository = SourceRepository()
        sourceRepository.setSources(listOf(sourceInfo("Example", apiService = FakeApiService(expected))))

        val vm = viewModel(sourceRepository)
        awaitCondition { vm.currentChosenSource != null }

        vm.open("https://example.com/item")
        awaitCondition { vm.kmpItemModel != null }

        assertEquals(expected, vm.kmpItemModel)
    }

    @Test fun `open sets kmpItemModel to null when sourceByUrl throws`() = runTest {
        val sourceRepository = SourceRepository()
        sourceRepository.setSources(listOf(sourceInfo("Example", apiService = FakeApiService())))

        val vm = viewModel(sourceRepository)
        awaitCondition { vm.currentChosenSource != null }

        vm.open("https://example.com/item")
        // Give the failing coroutine a moment to complete; kmpItemModel should remain null throughout.
        repeat(20) { delay(10) }

        assertNull(vm.kmpItemModel)
    }

    @Test fun `open with no chosen source leaves kmpItemModel null`() = runTest {
        val vm = viewModel(SourceRepository())

        vm.open("https://example.com/item")
        repeat(20) { delay(10) }

        assertNull(vm.kmpItemModel)
    }
}
