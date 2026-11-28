package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.ExceptionDatabase
import com.programmersbox.favoritesdatabase.ExceptionItem
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionHeadline
import com.programmersbox.kmpuiviews.testing.FakeAiText
import com.programmersbox.kmpuiviews.testing.clearAndJoin
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class ExceptionViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: ExceptionDatabase
    private lateinit var main: ExecutorCoroutineDispatcher
    private val created = mutableListOf<ExceptionViewModel>()

    private val ny = TimeZone.of("America/New_York")
    private val now = LocalDateTime(2026, 11, 27, 12, 0).toInstant(ny).toEpochMilliseconds()
    private val hour = 3_600_000L

    private val ai = FakeAiText()
    private val sources = SourceRepository()

    private class FakeApi(override val baseUrl: String) : KmpApiService

    // Room's Flow emits on its own dispatcher; wait in real time, polling on Main.
    private fun awaitCondition(condition: () -> Boolean) = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(5_000) { while (!condition()) delay(10) }
        }
    }

    private fun insert(time: Long, message: String) = runBlocking { database.exceptionDao().insertException(ExceptionItem(time = time, message = message)) }

    private fun storedIds() = runBlocking { database.exceptionDao().getAllExceptions().first().map { it.id } }

    private fun viewModel(versionName: (KmpSourceInformation) -> String? = { null }) = ExceptionViewModel(
        exceptionDao = database.exceptionDao(),
        sourceRepository = sources,
        aiText = ai,
        versionName = versionName,
        now = { now },
        timeZone = ny,
    ).also {
        created += it
        viewModelStore.put(System.identityHashCode(it).toString(), it)
    }

    @BeforeTest
    fun setUp() {
        main = newSingleThreadContext("exceptions-main")
        Dispatchers.setMain(main)
        dbFile = File.createTempFile("exception-viewmodel-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ExceptionDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        viewModelStore.clearAndJoin()
        runBlocking { created.forEach { it.clearedDeletes?.join() } }
        Dispatchers.resetMain()
        main.close()
        database.close()
        dbFile.delete()
    }

    @Test
    fun rowsParsedAndNewestFirst() {
        insert(now - 2 * hour, ExceptionFixtures.mangaDexTimeout)
        insert(now - hour, ExceptionFixtures.comickJson)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 2 }
        assertEquals(listOf(ExceptionHeadline.Parsing, ExceptionHeadline.TimedOut), vm.rows.value.map { it.parsed.headline })
    }

    @Test
    fun daysGroupedByLocalDate() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        insert(now - 2 * hour, ExceptionFixtures.comickJson)
        insert(now - 24 * hour, ExceptionFixtures.asura403)
        val vm = viewModel()
        awaitCondition { vm.days.value.sumOf { it.rows.size } == 3 }
        assertEquals(2, vm.days.value.size)
        assertEquals(2, vm.days.value[0].rows.size)
        assertEquals(26, vm.days.value[1].date.day)
    }

    @Test
    fun deleteHidesThenUndoRestores() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 1 }
        val id = vm.rows.value.single().id
        vm.delete(id)
        awaitCondition { vm.rows.value.isEmpty() }
        vm.undo(id)
        awaitCondition { vm.rows.value.size == 1 }
        assertEquals(listOf(id), storedIds())
    }

    @Test
    fun undoLastRestoresNewestDelete() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        insert(now - 2 * hour, ExceptionFixtures.comickJson)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 2 }
        val (a, b) = vm.rows.value.map { it.id }
        vm.delete(a)
        vm.delete(b)
        vm.undoLast()
        awaitCondition { vm.rows.value.map { it.id } == listOf(b) }
    }

    @Test
    fun commitDeleteRemovesFromDao() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 1 }
        val id = vm.rows.value.single().id
        vm.delete(id)
        vm.commitDelete(id)
        awaitCondition { storedIds().isEmpty() }
    }

    @Test
    fun pendingDeleteCommittedOnClear() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 1 }
        vm.delete(vm.rows.value.single().id)
        viewModelStore.clearAndJoin()
        runBlocking { vm.clearedDeletes!!.join() }
        assertTrue(storedIds().isEmpty())
    }

    @Test
    fun nextAfterDeletePicksNextThenPrevious() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        insert(now - 2 * hour, ExceptionFixtures.comickJson)
        insert(now - 3 * hour, ExceptionFixtures.asura403)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 3 }
        val (a, b, c) = vm.rows.value.map { it.id }
        assertEquals(c, vm.nextAfterDelete(b))
        assertEquals(b, vm.nextAfterDelete(c))
        assertEquals(b, vm.nextAfterDelete(a))
    }

    @Test
    fun nextAfterDeleteSingleRowIsNull() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 1 }
        assertNull(vm.nextAfterDelete(vm.rows.value.single().id))
    }

    @Test
    fun knownErrorNeverStartsAi() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 1 }
        val row = vm.rows.value.single()
        vm.explain(row)
        runBlocking { delay(200) }
        assertTrue(ai.runs.isEmpty())
        assertEquals(AiTextState.Idle, vm.explanation(row).value)
    }

    @Test
    fun aiTaskSharedPerGroupKey() {
        insert(now - hour, ExceptionFixtures.comickJson)
        insert(now - 2 * hour, ExceptionFixtures.comickJson)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 2 }
        val (r1, r2) = vm.rows.value
        assertSame(vm.explanation(r1), vm.explanation(r2))
        vm.explain(r1)
        awaitCondition { vm.explanation(r1).value is AiTextState.Done }
        vm.explain(r2)
        runBlocking { delay(200) }
        assertEquals(1, ai.inputsFor<AiFeature.ExplainException>().size)
    }

    @Test
    fun uninstalledExtensionFallsBackToId() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 1 }
        val row = vm.rows.value.single()
        assertEquals("en.mangadex", row.sourceName)
        assertNull(row.extensionVersion)
        assertNull(row.source)
    }

    @Test
    fun installedExtensionUsesNameAndVersion() {
        sources.addSource(
            KmpSourceInformation(
                apiService = FakeApi("https://mangadex.org"),
                name = "MangaDex",
                icon = null,
                packageName = "eu.kanade.tachiyomi.extension.en.mangadex",
            )
        )
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        val vm = viewModel(versionName = { "1.4.61" })
        awaitCondition { vm.rows.value.size == 1 }
        val row = vm.rows.value.single()
        assertEquals("MangaDex", row.sourceName)
        assertEquals("1.4.61", row.extensionVersion)
    }

    @Test
    fun appErrorHasNoSource() {
        insert(now - hour, ExceptionFixtures.backupFailure)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 1 }
        assertNull(vm.rows.value.single().sourceName)
    }

    @Test
    fun seenThisWeekCountsRepeats() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        insert(now - 30 * hour, ExceptionFixtures.mangaDexTimeout)
        insert(now - 9 * 24 * hour, ExceptionFixtures.mangaDexTimeout)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 3 }
        assertEquals(2, vm.rows.value.first().seenThisWeek)
    }

    @Test
    fun hourlyCountsFromRows() {
        insert(now - 10 * 60_000, ExceptionFixtures.mangaDexTimeout)
        insert(now - 30 * hour, ExceptionFixtures.comickJson)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 2 }
        awaitCondition { vm.hourly.value.sum() == 1 }
        assertEquals(1, vm.hourly.value[23])
    }

    @Test
    fun summarizeUsesLast24h() {
        insert(now - hour, ExceptionFixtures.comickJson)
        insert(now - 30 * hour, ExceptionFixtures.mangaDexTimeout)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 2 }
        vm.summarize()
        awaitCondition { vm.digest.value is AiTextState.Done }
        @Suppress("UNCHECKED_CAST")
        val lines = ai.inputsFor<AiFeature.ExceptionDigest>().single() as List<String>
        assertEquals(1, lines.size)
        assertTrue(lines.single().startsWith("all.comickfun: JsonDecodingException"), lines.single())
    }

    @Test
    fun deleteAllClearsEverything() {
        insert(now - hour, ExceptionFixtures.mangaDexTimeout)
        insert(now - 2 * hour, ExceptionFixtures.comickJson)
        val vm = viewModel()
        awaitCondition { vm.rows.value.size == 2 }
        vm.deleteAll()
        awaitCondition { storedIds().isEmpty() }
        awaitCondition { vm.rows.value.isEmpty() }
        assertFalse(vm.rows.value.isNotEmpty())
    }
}
