package com.programmersbox.kmpuiviews.presentation.settings.updatesources

import androidx.lifecycle.ViewModelStore
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.SkippedUpdateSource
import com.programmersbox.favoritesdatabase.SourceCount
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.updatecheck.FailureKind
import com.programmersbox.kmpuiviews.domain.updatecheck.SourceFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.testing.FakeUpdateCheckReportStore
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateSourcesViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var database: ItemDatabase
    private lateinit var dao: ItemDao

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = createTestItemDatabase()
        dao = database.itemDao()
    }

    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        database.close()
        Dispatchers.resetMain()
    }

    private fun sourceRepository(vararg serviceNames: String) = SourceRepository().apply {
        setSources(
            serviceNames.map { name ->
                KmpSourceInformation(
                    apiService = object : KmpApiService {
                        override val baseUrl = ""
                        override val serviceName = name
                    },
                    name = name,
                    icon = null,
                    packageName = name,
                )
            }
        )
    }

    private fun viewModel(
        report: UpdateCheckReport? = null,
        sourceRepository: SourceRepository = sourceRepository("asura", "comick", "mangapark"),
    ) = UpdateSourcesViewModel(
        itemDao = dao,
        sourceRepository = sourceRepository,
        reportStore = FakeUpdateCheckReportStore(report),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    private fun favorite(url: String, source: String, isDeleted: Boolean = false) = DbModel(
        title = url,
        description = "",
        url = url,
        imageUrl = "",
        source = source,
        isDeleted = isDeleted,
    )

    private suspend fun skipped(): Set<String> = dao.getSkippedUpdateSources().first().toSet()

    // buildUpdateSourceRows

    @Test
    fun `buildUpdateSourceRows sorts by display name ignoring case`() {
        val names = mapOf("a" to "zeta", "b" to "Alpha", "c" to "beta")

        val rows = buildUpdateSourceRows(
            installed = setOf("a", "b", "c"),
            counts = listOf(SourceCount("a", 1), SourceCount("b", 2), SourceCount("c", 3)),
            skipped = emptySet(),
            failedSources = emptySet(),
            displayName = { names.getValue(it) },
        )

        assertEquals(listOf("Alpha", "beta", "zeta"), rows.map { it.name })
        assertEquals(listOf("b", "c", "a"), rows.map { it.source })
        assertEquals(listOf(2, 3, 1), rows.map { it.favorites })
    }

    @Test
    fun `buildUpdateSourceRows marks skipped and failed from sets`() {
        val rows = buildUpdateSourceRows(
            installed = setOf("a", "b", "c"),
            counts = listOf(SourceCount("a", 1), SourceCount("b", 2), SourceCount("c", 3)),
            skipped = setOf("a", "c", "unknown"),
            failedSources = setOf("b", "c"),
            displayName = { it.uppercase() },
        )

        assertEquals(
            listOf(
                UpdateSourceRow("a", "A", 1, skipped = true, failedLastCheck = false),
                UpdateSourceRow("b", "B", 2, skipped = false, failedLastCheck = true),
                UpdateSourceRow("c", "C", 3, skipped = true, failedLastCheck = true),
            ),
            rows,
        )
    }

    @Test
    fun `buildUpdateSourceRows no installed sources gives empty rows`() {
        assertEquals(
            emptyList(),
            buildUpdateSourceRows(emptySet(), listOf(SourceCount("a", 1)), setOf("a"), setOf("a"), displayName = { it }),
        )
    }

    @Test
    fun `buildUpdateSourceRows lists installed sources only, with 0 favorites when none`() {
        val rows = buildUpdateSourceRows(
            installed = setOf("manga", "nofavorites"),
            counts = listOf(SourceCount("manga", 1), SourceCount("anime", 2)),
            skipped = emptySet(),
            failedSources = emptySet(),
            displayName = { it },
        )

        assertEquals(listOf("manga" to 1, "nofavorites" to 0), rows.map { it.source to it.favorites })
    }

    // updateSourcesCheckedCount

    @Test
    fun `updateSourcesCheckedCount counts installed sources only`() = runTest {
        dao.insertFavorites(favorite("1", "manga1"), favorite("2", "anime"))
        dao.insertSkippedUpdateSources(listOf("manga2", "anime").map(::SkippedUpdateSource))

        val count = dao.updateSourcesCheckedCount(sourceRepository("manga1", "manga2", "manga3")).first()

        assertEquals(2 to 3, count)
    }

    // setSkipped

    @Test
    fun `setSkipped true inserts sources`() = runTest {
        dao.insertSkippedUpdateSources(listOf(SkippedUpdateSource("a")))
        val vm = viewModel()

        vm.setSkipped(listOf("b", "c"), skipped = true)

        assertEquals(setOf("a", "b", "c"), skipped())
    }

    @Test
    fun `setSkipped true on already skipped source keeps one row`() = runTest {
        dao.insertSkippedUpdateSources(listOf(SkippedUpdateSource("a")))
        val vm = viewModel()

        vm.setSkipped(listOf("a"), skipped = true)

        assertEquals(listOf("a"), dao.getSkippedUpdateSources().first())
    }

    @Test
    fun `setSkipped false deletes sources`() = runTest {
        dao.insertSkippedUpdateSources(listOf("a", "b", "c").map(::SkippedUpdateSource))
        val vm = viewModel()

        vm.setSkipped(listOf("a", "c", "missing"), skipped = false)

        assertEquals(setOf("b"), skipped())
    }

    // rows

    @Test
    fun `rows combine favorite counts skipped sources and source failures`() = runTest {
        dao.insertFavorites(
            favorite("1", "mangapark"),
            favorite("2", "mangapark"),
            favorite("3", "comick"),
            favorite("4", "asura"),
            favorite("5", "asura", isDeleted = true),
            favorite("6", "deletedonly", isDeleted = true),
            favorite("7", "otherapp"),
        )
        dao.insertSkippedUpdateSources(listOf("comick", "notfavorited").map(::SkippedUpdateSource))
        val report = UpdateCheckReport(
            startedAt = 0,
            finishedAt = 1,
            favoriteCount = 4,
            checkedCount = 4,
            updatedCount = 0,
            sourceFailures = listOf(SourceFailure("mangapark", FailureKind.Http, 403, "HTTP 403")),
            itemFailures = emptyList(),
        )
        val vm = viewModel(report, sourceRepository("asura", "comick", "mangapark", "nofavorites"))

        val rows = vm.rows.first { it != null }!!

        // Fake sources keep the service name as display name. Rows are installed sources only:
        // "otherapp" and "deletedonly" aren't installed, "nofavorites" is installed with no favorites.
        assertEquals(
            listOf(
                UpdateSourceRow("asura", "asura", 1, skipped = false, failedLastCheck = false),
                UpdateSourceRow("comick", "comick", 1, skipped = true, failedLastCheck = false),
                UpdateSourceRow("mangapark", "mangapark", 2, skipped = false, failedLastCheck = true),
                UpdateSourceRow("nofavorites", "nofavorites", 0, skipped = false, failedLastCheck = false),
            ),
            rows,
        )
    }

    @Test
    fun `rows without report mark no failures and update after setSkipped`() = runTest {
        dao.insertFavorites(favorite("1", "asura"), favorite("2", "comick"))
        val vm = viewModel(report = null, sourceRepository = sourceRepository("asura", "comick"))

        val initial = vm.rows.first { it != null }!!
        assertEquals(listOf(false, false), initial.map { it.failedLastCheck })
        assertEquals(listOf(false, false), initial.map { it.skipped })

        vm.setSkipped(listOf("comick"), skipped = true)

        val updated = vm.rows.first { rows -> rows?.any { it.skipped } == true }!!
        assertEquals(listOf("asura" to false, "comick" to true), updated.map { it.source to it.skipped })
    }
}
