package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstats

import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.sharedcomponents.stats.StatData
import com.programmersbox.sharedcomponents.stats.StatisticsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DiscoveryStatisticsProvider(
    private val sourceRepository: SourceRepository,
    private val notesDao: NotesDao,
    private val recommendationDao: RecommendationDao,
    private val historyDao: HistoryDao,
    private val listDao: ListDao,
) : StatisticsProvider() {
    override val header: String = "🔍 Discovery"
    override val key: String = "discovery"
    override val contentType: String = "discovery"
    override val priority: Int = 2

    override fun observeStats(): Flow<List<StatData>> = combine(
        sourceRepository
            .sources
            .map { list ->
                list
                    .filterNot { it.apiService.notWorking }
                    .groupBy { it.packageName }
                    .size
            }
            .map {
                listOf(
                    StatData(
                        id = "sources",
                        label = "Sources",
                        description = "Installed extensions",
                        value = it.toString()
                    )
                )
            },
        combine(
            historyDao.getAllHistoryCount(),
            historyDao.getAllRecentHistoryCount()
        ) { history, recentHistory ->
            listOf(
                StatData(
                    id = "history",
                    label = "Search History",
                    description = "Recent searches",
                    value = history.toString(),
                ),
                StatData(
                    id = "recentHistory",
                    label = "Global Search History",
                    description = "Cross-source searches",
                    value = recentHistory.toString(),
                )
            )
        },
        recommendationDao
            .getRecommendationCount()
            .map {
                listOf(
                    StatData(
                        id = "recommendations",
                        label = "Saved Recommendations",
                        description = "Suggested titles saved",
                        value = it.toString(),
                    )
                )
            },
        combine(
            listDao.getAllListsCount(),
            listDao.getAllListItemsCount()
        ) { lists, listItems ->
            listOf(
                StatData(
                    id = "lists",
                    label = "Lists",
                    description = "$listItems items total",
                    value = lists.toString(),
                )
            )
        },
        notesDao
            .getAllNotesCount()
            .map {
                listOf(
                    StatData(
                        id = "notes",
                        label = "Notes",
                        description = "Notes saved",
                        value = it.toString(),
                    )
                )
            },
    ) { it.toList().flatten() }
}