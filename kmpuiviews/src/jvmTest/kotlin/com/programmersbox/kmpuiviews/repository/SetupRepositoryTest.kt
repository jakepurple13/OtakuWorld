package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetupRepositoryTest {

    private lateinit var database: ItemDatabase
    private lateinit var job: Job

    // Room's Flow and the DataStore Flow both emit on real dispatchers, not the test
    // dispatcher's virtual clock, so wait for them with real time instead of runTest's
    // built-in time control (see FavoriteViewModelTest.awaitCondition for the same issue).
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(10_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun sourceInformation(name: String, packageName: String = "com.$name") = KmpSourceInformation(
        apiService = ExampleService(),
        name = name,
        icon = null,
        packageName = packageName,
    )

    private fun setupRepository(
        sourceRepository: SourceRepository = SourceRepository(),
        dataStoreHandling: com.programmersbox.datastore.DataStoreHandling = createTestDataStoreHandling(),
        appUpdateCheck: AppUpdateCheck = AppUpdateCheck(),
        currentSourceRepository: CurrentSourceRepository = CurrentSourceRepository(),
    ) = SetupRepository(
        sourceRepository = sourceRepository,
        itemDao = database.itemDao(),
        dataStoreHandling = dataStoreHandling,
        appUpdateCheck = appUpdateCheck,
        currentSourceRepository = currentSourceRepository,
    )

    @AfterTest
    fun tearDown() {
        job.cancel()
        database.close()
    }

    @Test fun `setup writes each source's order into ItemDao`() = runTest {
        database = createTestItemDatabase()
        job = Job()
        val scope = CoroutineScope(job)
        val sourceRepository = SourceRepository()
        val repository = setupRepository(sourceRepository = sourceRepository)

        repository.setup(scope)
        sourceRepository.setSources(
            listOf(
                sourceInformation("First"),
                sourceInformation("Second"),
            )
        )

        awaitCondition { database.itemDao().getSourceOrderSync().size == 2 }

        val order = database.itemDao().getSourceOrderSync().sortedBy { it.order }
        assertEquals(listOf("com.First", "com.Second"), order.map { it.source })
        assertEquals(listOf(0, 1), order.map { it.order })
    }

    @Test fun `setup re-emitting sources inserts newly seen sources`() = runTest {
        database = createTestItemDatabase()
        job = Job()
        val scope = CoroutineScope(job)
        val sourceRepository = SourceRepository()
        val repository = setupRepository(sourceRepository = sourceRepository)

        repository.setup(scope)
        sourceRepository.setSources(listOf(sourceInformation("First")))
        awaitCondition { database.itemDao().getSourceOrderSync().isNotEmpty() }

        // insertSourceOrder uses OnConflictStrategy.IGNORE, keyed on packageName, so an
        // already-known source's stored order (0, from the first emission) is left untouched,
        // even though it's now second in the list and would otherwise be indexed as 1.
        sourceRepository.setSources(
            listOf(
                sourceInformation("Second"),
                sourceInformation("First"),
            )
        )
        awaitCondition { database.itemDao().getSourceOrderSync().size == 2 }

        val order = database.itemDao().getSourceOrderSync().sortedBy { it.source }
        assertEquals(listOf("com.First", "com.Second"), order.map { it.source })
        assertEquals(listOf(0, 0), order.map { it.order })
    }

    @Test fun `setup picks a random source when no current service is set`() = runTest {
        database = createTestItemDatabase()
        job = Job()
        val scope = CoroutineScope(job)
        val sourceRepository = SourceRepository()
        val currentSourceRepository = CurrentSourceRepository()
        val repository = setupRepository(
            sourceRepository = sourceRepository,
            currentSourceRepository = currentSourceRepository,
        )

        repository.setup(scope)
        sourceRepository.setSources(listOf(sourceInformation("First")))

        awaitCondition { currentSourceRepository.asFlow().value != null }

        assertTrue(currentSourceRepository.asFlow().value is ExampleService)
    }

    @Test fun `setup emits the source matching the stored current service name`() = runTest {
        database = createTestItemDatabase()
        job = Job()
        val scope = CoroutineScope(job)
        val sourceRepository = SourceRepository()
        val dataStoreHandling = createTestDataStoreHandling()
        val currentSourceRepository = CurrentSourceRepository()
        val repository = setupRepository(
            sourceRepository = sourceRepository,
            dataStoreHandling = dataStoreHandling,
            currentSourceRepository = currentSourceRepository,
        )

        dataStoreHandling.currentService.set("ExampleService")
        repository.setup(scope)
        sourceRepository.setSources(
            listOf(
                sourceInformation("First"),
                sourceInformation("Second"),
            )
        )

        awaitCondition { currentSourceRepository.asFlow().value != null }

        assertTrue(currentSourceRepository.asFlow().value is ExampleService)
    }
}
