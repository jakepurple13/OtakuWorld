package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.testing.FakeApiService
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetrySourceNamerTest {
    private lateinit var database: ItemDatabase
    private lateinit var sourceRepository: SourceRepository

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
        sourceRepository = SourceRepository()
        sourceRepository.addSources(
            listOf(
                KmpSourceInformation(FakeApiService("MangaDex"), "MangaDex", null, "pkg.mangadex"),
                KmpSourceInformation(FakeApiService("Example"), "Example", null, "js.example"),
            )
        )
    }

    @AfterTest
    fun tearDown() = database.close()

    private fun TestScope.started(): TelemetrySourceNamer =
        TelemetrySourceNamer(sourceRepository, database.itemDao()).also { it.start(backgroundScope) }

    // Room emits on its own dispatcher, outside runTest's virtual clock (same as SetupRepositoryTest)
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(10_000) { while (!condition()) delay(10) }
        }
    }

    // Marks another source incognito and waits until the namer has seen it, so the table has loaded
    private suspend fun awaitLoaded(namer: TelemetrySourceNamer) {
        database.itemDao().insertIncognitoSource(IncognitoSource("pkg.marker", "Marker", isIncognito = true))
        awaitCondition { namer.name("Marker") == "incognito" }
    }

    private suspend fun markIncognito(pkg: String, name: String, on: Boolean = true) =
        database.itemDao().insertIncognitoSource(IncognitoSource(pkg, name, isIncognito = on))

    @Test
    fun `plugin source keeps its name`() = runTest {
        val namer = started()
        awaitLoaded(namer)
        assertEquals("MangaDex", namer.name("MangaDex"))
    }

    @Test
    fun `js package becomes custom`() = runTest {
        val namer = started()
        assertEquals("custom", namer.name("Example"))
        assertEquals("custom", namer.packageName("js.example"))
    }

    @Test
    fun `incognito source becomes incognito`() = runTest {
        markIncognito("pkg.mangadex", "MangaDex")
        val namer = started()
        awaitCondition { namer.name("MangaDex") == "incognito" }
        assertEquals("incognito", namer.name("MangaDex"))
    }

    @Test
    fun `incognito added after start applies`() = runTest {
        val namer = started()
        awaitLoaded(namer)
        assertEquals("MangaDex", namer.name("MangaDex"))
        markIncognito("pkg.mangadex", "MangaDex")
        awaitCondition { namer.name("MangaDex") == "incognito" }
        assertEquals("incognito", namer.name("MangaDex"))
    }

    @Test
    fun `row with incognito off is not renamed`() = runTest {
        markIncognito("pkg.mangadex", "MangaDex", on = false)
        val namer = started()
        awaitLoaded(namer)
        assertEquals("MangaDex", namer.name("MangaDex"))
    }

    @Test
    fun `unknown service name passes through`() = runTest {
        val namer = started()
        awaitLoaded(namer)
        assertEquals("Gone", namer.name("Gone"))
    }

    @Test
    fun `unknown service name with incognito row by name is renamed`() = runTest {
        markIncognito("pkg.gone", "Gone")
        val namer = started()
        awaitCondition { namer.name("Gone") == "incognito" }
        assertEquals("incognito", namer.name("Gone"))
    }

    @Test
    fun `incognito by package name`() = runTest {
        markIncognito("pkg.mangadex", "MangaDex")
        val namer = started()
        awaitCondition { namer.packageName("pkg.mangadex") == "incognito" }
        assertEquals("incognito", namer.packageName("pkg.mangadex"))
    }
}
