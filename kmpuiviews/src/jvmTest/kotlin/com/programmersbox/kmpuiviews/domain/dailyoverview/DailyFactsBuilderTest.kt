package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.HeatMapDatabase
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.kmpuiviews.domain.updatecheck.FailureKind
import com.programmersbox.kmpuiviews.domain.updatecheck.SourceFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import com.programmersbox.kmpuiviews.testing.FakeKmpGenericInfo
import com.programmersbox.kmpuiviews.testing.FakeUpdateCheckReportStore
import com.programmersbox.kmpuiviews.testing.FixedClock
import com.programmersbox.kmpuiviews.testing.createTestHeatMapDatabase
import com.programmersbox.kmpuiviews.testing.createTestHistoryDatabase
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyFactsBuilderTest {

    private val zone = TimeZone.of("America/Phoenix")
    private val yesterday = LocalDate(2026, 11, 10)
    private val clock = FixedClock(LocalDateTime(2026, 11, 11, 10, 0).toInstant(zone))

    private lateinit var items: ItemDatabase
    private lateinit var heatMap: HeatMapDatabase
    private lateinit var history: HistoryDatabase

    @BeforeTest
    fun setUp() {
        items = createTestItemDatabase()
        heatMap = createTestHeatMapDatabase()
        history = createTestHistoryDatabase()
    }

    @AfterTest
    fun tearDown() {
        items.close()
        heatMap.close()
        history.close()
    }

    private fun builder(report: UpdateCheckReport? = null) = DailyFactsBuilder(
        itemDao = items.itemDao(),
        heatMapDao = heatMap.heatMapDao(),
        historyDao = history.historyDao(),
        reportStore = FakeUpdateCheckReportStore(report),
        genericInfo = FakeKmpGenericInfo(),
        clock = clock,
        zone = { zone },
    )

    private fun at(hour: Int, minute: Int = 0, day: Int = 10) =
        LocalDateTime(2026, 11, day, hour, minute).toInstant(zone).toEpochMilliseconds()

    private suspend fun watch(url: String, name: String, favoriteUrl: String, createdAt: Long) =
        items.itemDao().insertChapter(ChapterWatched(url, name, favoriteUrl, createdAt = createdAt, updatedAt = createdAt))

    private fun favorite(url: String, title: String = "Title $url", numChapters: Int = 0, source: String = "Src") =
        DbModel(title, "d", url, "img-$url", source, numChapters = numChapters)

    @Test
    fun `readCount comes from the heat map, not row count`() = runTest {
        heatMap.heatMapDao().insertHeatMap(HeatMapItem(yesterday, 23))
        repeat(5) { watch("c$it", "Ch $it", "fav", at(9, it)) }
        assertEquals(23, builder().yesterday().readCount)
    }

    @Test
    fun `reads at 23-59 local count for yesterday and 00-00 does not`() = runTest {
        watch("a", "A", "fav", at(23, 59))
        watch("b", "B", "fav", at(0, 0, day = 11))
        val facts = builder().yesterday()
        assertEquals(1, facts.readsByHour[23])
        assertEquals(1, facts.readsByHour.sum())
        assertEquals(1, facts.titles.single().count)
    }

    @Test
    fun `rows with createdAt 0 are excluded and readsByHour is all zero`() = runTest {
        watch("a", "A", "fav", 0L)
        val facts = builder().yesterday()
        assertEquals(List(24) { 0 }, facts.readsByHour)
        assertTrue(facts.titles.isEmpty())
    }

    @Test
    fun `titles group by favoriteUrl with first and last chapter by createdAt order`() = runTest {
        items.itemDao().insertFavorite(favorite("fav"))
        watch("c2", "Ch 2", "fav", at(9, 30))
        watch("c1", "Ch 1", "fav", at(9, 0))
        watch("c3", "Ch 3", "fav", at(20, 0))
        watch("o1", "Other 1", "other", at(8, 0))
        val facts = builder().yesterday()
        val t = facts.titles.first()
        assertEquals(2, facts.titles.size)
        assertEquals("fav", t.url)
        assertEquals("Title fav", t.title)
        assertEquals("img-fav", t.imageUrl)
        assertEquals("Src", t.source)
        assertEquals("Ch 1", t.firstChapter)
        assertEquals("Ch 3", t.lastChapter)
        assertEquals(3, t.count)
        assertEquals(at(20, 0), t.lastReadAt)
    }

    @Test
    fun `lastRead is the title read most recently`() = runTest {
        watch("f1", "F1", "frieren", at(22, 0))
        watch("f2", "F2", "frieren", at(23, 6))
        watch("k1", "K1", "kaiju", at(18, 40))
        watch("d1", "D1", "dandadan", at(9, 0))
        watch("d2", "D2", "dandadan", at(10, 0))
        watch("d3", "D3", "dandadan", at(11, 0))
        val facts = builder().yesterday()
        assertEquals("kaiju", facts.titles.last().url)
        assertEquals("frieren", facts.lastRead?.url)
    }

    @Test
    fun `caughtUp is true when watched count for the title equals numChapters`() = runTest {
        items.itemDao().insertFavorite(favorite("done", numChapters = 2))
        items.itemDao().insertFavorite(favorite("partial", numChapters = 3))
        watch("d1", "1", "done", at(9))
        watch("d2", "2", "done", at(10))
        watch("p1", "1", "partial", at(9))
        watch("p2", "2", "partial", at(10))
        val byUrl = builder().yesterday().titles.associateBy { it.url }
        assertTrue(byUrl.getValue("done").caughtUp)
        assertFalse(byUrl.getValue("partial").caughtUp)
    }

    @Test
    fun `title falls back to RecentModel then to the url host when no favorite exists`() = runTest {
        history.historyDao().insertRecentlyViewed(
            RecentModel("Recent Title", "d", "https://recent.example/x", "https://img/r.jpg", "RecentSrc")
        )
        watch("a", "A", "https://recent.example/x", at(9))
        watch("b", "B", "https://nowhere.example/series/1", at(9))
        val byUrl = builder().yesterday().titles.associateBy { it.url }
        assertEquals("Recent Title", byUrl.getValue("https://recent.example/x").title)
        assertEquals("RecentSrc", byUrl.getValue("https://recent.example/x").source)
        assertEquals("nowhere.example", byUrl.getValue("https://nowhere.example/series/1").title)
    }

    @Test
    fun `updates are notifications created from yesterday start until now`() = runTest {
        fun noti(url: String, createdAt: Long) = NotificationItem(
            id = url.hashCode(), url = url, summaryText = "sum $url", notiTitle = "T $url",
            imageUrl = null, source = "Src", contentTitle = "T $url", createdAt = createdAt,
        )
        items.itemDao().insertNotification(noti("before", at(23, 59, day = 9)))
        items.itemDao().insertNotification(noti("in", at(12)))
        items.itemDao().insertNotification(noti("today", at(8, 0, day = 11)))
        items.itemDao().insertNotification(noti("future", at(11, 0, day = 11)))
        val updates = builder().yesterday().updates
        assertEquals(setOf("in", "today"), updates.map { it.url }.toSet())
        assertEquals("sum in", updates.first { it.url == "in" }.summary)
    }

    @Test
    fun `failedSources come from the last report's sourceFailures`() = runTest {
        val kind = FailureKind.entries.first()
        val report = UpdateCheckReport(
            startedAt = 0, finishedAt = 1, favoriteCount = 0, checkedCount = 0, updatedCount = 0,
            sourceFailures = listOf(SourceFailure("A", kind, null, ""), SourceFailure("B", kind, null, "")),
            itemFailures = emptyList(),
        )
        assertEquals(listOf("A", "B"), builder(report).yesterday().failedSources)
    }

    @Test
    fun `incognito favoriteUrl rows are excluded`() = runTest {
        items.itemDao().insertIncognitoSource(IncognitoSource("secret", "Secret", true))
        items.itemDao().insertFavorite(favorite("srcHidden", source = "HiddenSrc"))
        items.itemDao().insertIncognitoSource(IncognitoSource("HiddenSrc", "HiddenSrc", true))
        watch("a", "A", "secret", at(9))
        watch("b", "B", "srcHidden", at(9))
        watch("c", "C", "visible", at(9))
        val facts = builder().yesterday()
        assertEquals(listOf("visible"), facts.titles.map { it.url })
        assertEquals(1, facts.readsByHour.sum())
    }

    @Test
    fun `isQuiet when no reads and no updates`() = runTest {
        val facts = builder().yesterday()
        assertTrue(facts.isQuiet)
        assertEquals(UpdateUnit.Chapters, facts.unit)
        assertEquals(yesterday, facts.date)
    }
}
