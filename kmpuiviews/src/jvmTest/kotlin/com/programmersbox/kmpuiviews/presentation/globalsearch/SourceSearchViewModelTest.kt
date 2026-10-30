package com.programmersbox.kmpuiviews.presentation.globalsearch

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.testing.FakeExceptionDao
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourceSearchViewModelTest {

    private val viewModelStore = ViewModelStore()
    private val exceptionDao = FakeExceptionDao()

    /** Serves [pages] by 1-based page number; a page listed in [failOn] throws once. */
    private class FakeSource(
        val pages: Map<Int, List<String>>,
        val failOn: MutableSet<Int> = mutableSetOf(),
    ) : KmpApiService {
        override val baseUrl: String = ""
        override val serviceName: String = "fake"
        val requestedPages = mutableListOf<Int>()

        override suspend fun search(searchText: CharSequence, page: Int, list: List<KmpItemModel>): List<KmpItemModel> {
            requestedPages += page
            if (failOn.remove(page)) error("page $page failed")
            return pages[page].orEmpty().map { KmpItemModel(it, "", it, "", this) }
        }
    }

    private fun repositoryWith(source: KmpApiService?) = SourceRepository().apply {
        source?.let { addSource(KmpSourceInformation(it, "Fake", null, "fake.package")) }
    }

    private fun viewModel(repository: SourceRepository) = SourceSearchViewModel(
        handle = Screen.SourceSearchScreen(serviceName = "fake", query = "solo"),
        sourceRepository = repository,
        exceptionDao = exceptionDao,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    private suspend fun awaitIdle(vm: SourceSearchViewModel) = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            delay(20)
            while (vm.isLoading || vm.source == null) delay(10)
        }
    }

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

    @Test
    fun `loads page 1 on open`() = runTest {
        val vm = viewModel(repositoryWith(FakeSource(mapOf(1 to listOf("a", "b")))))
        awaitIdle(vm)

        assertEquals(listOf(1), vm.pages.map { it.number })
        assertEquals(listOf("a", "b"), vm.pages.single().items.map { it.url })
        assertEquals(2, vm.itemCount)
    }

    @Test
    fun `loadMore appends the next page`() = runTest {
        val source = FakeSource(mapOf(1 to listOf("a"), 2 to listOf("b", "c")))
        val vm = viewModel(repositoryWith(source))
        awaitIdle(vm)

        vm.loadMore()
        awaitIdle(vm)

        assertEquals(listOf(1, 2), vm.pages.map { it.number })
        assertEquals(listOf(1, 2), source.requestedPages)
        assertEquals(3, vm.itemCount)
    }

    @Test
    fun `items already loaded are dropped`() = runTest {
        val vm = viewModel(repositoryWith(FakeSource(mapOf(1 to listOf("a", "b"), 2 to listOf("b", "c")))))
        awaitIdle(vm)

        vm.loadMore()
        awaitIdle(vm)

        assertEquals(listOf("c"), vm.pages[1].items.map { it.url })
    }

    @Test
    fun `an empty page ends paging`() = runTest {
        val source = FakeSource(mapOf(1 to listOf("a")))
        val vm = viewModel(repositoryWith(source))
        awaitIdle(vm)

        vm.loadMore()
        awaitIdle(vm)
        vm.loadMore()
        awaitIdle(vm)

        assertTrue(vm.endReached)
        assertEquals(listOf(1, 2), source.requestedPages)
    }

    @Test
    fun `a page of only duplicates ends paging`() = runTest {
        val vm = viewModel(repositoryWith(FakeSource(mapOf(1 to listOf("a"), 2 to listOf("a")))))
        awaitIdle(vm)

        vm.loadMore()
        awaitIdle(vm)

        assertTrue(vm.endReached)
        assertEquals(1, vm.pages.size)
    }

    @Test
    fun `a failed page keeps results and retry loads it`() = runTest {
        val source = FakeSource(mapOf(1 to listOf("a"), 2 to listOf("b")), failOn = mutableSetOf(2))
        val vm = viewModel(repositoryWith(source))
        awaitIdle(vm)

        vm.loadMore()
        awaitIdle(vm)

        assertNotNull(vm.error)
        assertEquals(1, vm.pages.size)
        assertEquals(1, exceptionDao.insertedExceptions.size)

        vm.loadMore()
        awaitIdle(vm)
        assertEquals(listOf(1, 2), source.requestedPages, "loadMore does nothing while an error is showing")

        vm.retry()
        awaitIdle(vm)

        assertNull(vm.error)
        assertEquals(listOf(1, 2), vm.pages.map { it.number })
    }

    @Test
    fun `waits for the source to be installed`() = runTest {
        val repository = repositoryWith(null)
        val vm = viewModel(repository)
        delay(50)
        assertNull(vm.source)

        repository.addSource(KmpSourceInformation(FakeSource(mapOf(1 to listOf("a"))), "Fake", null, "fake.package"))
        awaitIdle(vm)

        assertEquals(1, vm.itemCount)
    }
}
