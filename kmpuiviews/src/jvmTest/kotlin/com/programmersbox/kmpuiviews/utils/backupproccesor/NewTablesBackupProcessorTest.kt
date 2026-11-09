package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.favoritesdatabase.SkippedUpdateSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Buffer
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NewTablesBackupProcessorTest {

    private val files = mutableListOf<File>()
    private val closers = mutableListOf<() -> Unit>()

    private fun tempDb(prefix: String) = File.createTempFile(prefix, ".db").also {
        it.deleteOnExit()
        files += it
    }

    private fun itemDatabase() = Room.databaseBuilder<ItemDatabase>(name = tempDb("item-backup-test").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .build()
        .also { closers += it::close }

    private fun historyDatabase() = Room.databaseBuilder<HistoryDatabase>(name = tempDb("history-backup-test").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .build()
        .also { closers += it::close }

    @BeforeTest
    fun setUp() {
        files.clear()
        closers.clear()
    }

    @AfterTest
    fun tearDown() {
        closers.forEach { it() }
        files.forEach { it.delete() }
    }

    @Test
    fun `skipped update sources round trip into a fresh database`() = runTest {
        val source = itemDatabase().itemDao()
        source.insertSkippedUpdateSources(
            listOf(SkippedUpdateSource("eu.kanade.tachiyomi.extension.all.mangadex"), SkippedUpdateSource("com.example.ext"))
        )
        val sink = Buffer()
        val backupResult = SkippedUpdateSourcesBackupProcessor(source).backup(sink)
        assertEquals(2, backupResult.successCount)

        val target = itemDatabase().itemDao()
        val processor = SkippedUpdateSourcesBackupProcessor(target)
        val json = sink.readUtf8()
        val restoreResult = processor.restore(json, Buffer().writeUtf8(json))

        assertEquals(2, restoreResult.successCount)
        assertEquals(
            setOf("eu.kanade.tachiyomi.extension.all.mangadex", "com.example.ext"),
            target.getSkippedUpdateSources().first().toSet(),
        )
        assertEquals(2, processor.currentSummary().itemCount)
        assertEquals(2, processor.parseSummary(json, json.encodeToByteArray()).itemCount)
    }

    @Test
    fun `recently viewed round trip into a fresh database`() = runTest {
        val source = historyDatabase().historyDao()
        val first = RecentModel(
            title = "Helmut",
            description = "desc",
            url = "https://example.com/helmut",
            imageUrl = "https://example.com/helmut.jpg",
            source = "ExampleService",
            timestamp = 100L,
        )
        val second = first.copy(title = "Other", url = "https://example.com/other", timestamp = 200L)
        source.insertRecentlyViewed(first)
        source.insertRecentlyViewed(second)
        val sink = Buffer()
        val backupResult = RecentlyViewedBackupProcessor(source).backup(sink)
        assertEquals(2, backupResult.successCount)

        val target = historyDatabase().historyDao()
        val processor = RecentlyViewedBackupProcessor(target)
        val json = sink.readUtf8()
        val restoreResult = processor.restore(json, Buffer().writeUtf8(json))

        assertEquals(2, restoreResult.successCount)
        assertEquals(listOf(first, second), target.getRecentlyViewed().first())
        assertEquals(2, processor.currentSummary().itemCount)
        assertEquals(2, processor.parseSummary(json, json.encodeToByteArray()).itemCount)
    }
}
