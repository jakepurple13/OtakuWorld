package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Collects everything the daily overview shows about one local day, straight from the DBs. */
class DailyFactsBuilder(
    private val itemDao: ItemDao,
    private val heatMapDao: HeatMapDao,
    private val historyDao: HistoryDao,
    private val reportStore: UpdateCheckReportStore,
    private val genericInfo: KmpGenericInfo,
    private val clock: Clock = Clock.System,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    suspend fun yesterday(): DailyFacts {
        val today = clock.now().toLocalDateTime(zone()).date
        return forDate(today.plus(-1, DateTimeUnit.DAY))
    }

    suspend fun forDate(date: LocalDate): DailyFacts {
        val tz = zone()
        val start = date.atStartOfDayIn(tz).toEpochMilliseconds()
        val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
        val now = clock.now().toEpochMilliseconds()

        val incognito = itemDao.getAllIncognitoSourcesSync()
            .filter { it.isIncognito }
            .flatMap { listOf(it.source, it.name) }
            .toSet()

        val rows = itemDao.getChaptersWatchedBetween(start, end)
            .filter { it.favoriteUrl !in incognito }

        val favorites = itemDao.getFavoritesByUrls(rows.map { it.favoriteUrl }.distinct())
            .associateBy { it.url }
        val recents = rows.map { it.favoriteUrl }.distinct()
            .filter { it !in favorites }
            .mapNotNull { historyDao.getRecentByUrl(it) }
            .associateBy { it.url }
        fun sourceOf(favoriteUrl: String) = favorites[favoriteUrl]?.source ?: recents[favoriteUrl]?.source.orEmpty()

        val watched = rows.filter { sourceOf(it.favoriteUrl) !in incognito }

        val readsByHour = IntArray(24)
        watched.forEach {
            readsByHour[Instant.fromEpochMilliseconds(it.createdAt).toLocalDateTime(tz).hour]++
        }

        val titles = watched.groupBy { it.favoriteUrl }.map { (favoriteUrl, group) ->
            val favorite = favorites[favoriteUrl]
            val recent = recents[favoriteUrl]
            val ordered = group.sortedBy { it.createdAt }
            val total = itemDao.getAllChaptersSync(favoriteUrl).count { !it.isDeleted }
            TitleRead(
                title = favorite?.title ?: recent?.title ?: favoriteUrl.host(),
                url = favoriteUrl,
                imageUrl = favorite?.imageUrl ?: recent?.imageUrl.orEmpty(),
                source = sourceOf(favoriteUrl),
                firstChapter = ordered.first().name,
                lastChapter = ordered.last().name,
                count = group.size,
                caughtUp = favorite != null && favorite.numChapters > 0 && total == favorite.numChapters,
                lastReadAt = ordered.last().createdAt,
            )
        }.sortedByDescending { it.count }

        val updates = itemDao.getNotificationsCreatedBetween(start, maxOf(now, start))
            .filter { it.source !in incognito && it.url !in incognito }
            .map { UpdateItem(it.notiTitle, it.url, it.imageUrl, it.source, it.summaryText) }

        return DailyFacts(
            date = date,
            unit = genericInfo.updateUnit,
            readCount = heatMapDao.getHeatMapByDate(date)?.count ?: 0,
            titles = titles,
            readsByHour = readsByHour.toList(),
            updates = updates,
            failedSources = reportStore.current()?.sourceFailures.orEmpty().map { it.source }.distinct(),
        )
    }

    private fun String.host() = substringAfter("://").substringBefore('/').ifEmpty { this }
}
