package com.programmersbox.kmpuiviews.presentation.settings.accountinfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleeys.heatmap.model.Heat
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.kmpuiviews.utils.fireListener
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import nl.jacobras.humanreadable.HumanReadable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class AccountInfoViewModel(
    itemDao: ItemDao,
    listDao: ListDao,
    historyDao: HistoryDao,
    blurHashDao: BlurHashDao,
    heatMapDao: HeatMapDao,
    translationModelHandler: TranslationModelHandler,
    sourceRepository: SourceRepository,
    firebaseConnection: KmpFirebaseConnection.KmpFirebaseListener,
    dataStoreHandling: DataStoreHandling,
    recommendationDao: RecommendationDao,
) : ViewModel() {

    private val favoriteListener = fireListener(itemListener = firebaseConnection)

    var accountInfo by mutableStateOf(AccountInfoCount.Empty)
        private set

    init {
        combine(
            favoriteListener
                .getAllShowsFlow()
                .map { it.size },
            itemDao.getAllFavoritesCount(),
            itemDao.getAllNotificationCount(),
            itemDao.getAllIncognitoSourcesCount(),
            historyDao.getAllRecentHistoryCount(),
            listDao.getAllListsCount(),
            listDao.getAllListItemsCount(),
            itemDao.getAllChaptersCount(),
            blurHashDao.getAllHashesCount(),
            flow { emit(translationModelHandler.modelList().size) },
            sourceRepository
                .sources
                .map { list ->
                    list
                        .filterNot { it.apiService.notWorking }
                        .groupBy { it.packageName }
                        .size
                },
            historyDao.getAllHistoryCount(),
            recommendationDao.getRecommendationCount()
        ) { AccountInfoCount(it) }
            .combine(dataStoreHandling.timeSpentDoing.asFlow()) { a, b ->
                val afterText = if (b <= 60) {
                    ""
                } else {
                    "\n($b seconds)"
                }
                a.copy(timeSpentDoing = "${HumanReadable.duration(b.seconds)}$afterText")
            }
            .combine(heatMapDao.getAllHeatMaps()) { a, b ->
                val placeholderHeats = b
                    .minByOrNull { it.time.toEpochDays() }
                    ?.time
                    ?.let { generateHeats(it) }
                    .orEmpty()

                a.copy(
                    heatMaps = placeholderHeats + b.map { Heat(it.time, it.count.toDouble(), it.count) }
                )
            }
            .onEach { accountInfo = it }
            .launchIn(viewModelScope)
    }

}

    @OptIn(ExperimentalTime::class)
    private fun generateHeats(
        startDate: LocalDate,
    ): List<Heat<Int>> {
        val curDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val distance = startDate.monthsUntil(curDate) < 12

        return if(distance) {
            generateSequence(startDate) { date ->
                if (date < curDate) date + DatePeriod(days = 1) else null
            }.map { date ->
                Heat(date, 0.0, 0)
            }.toList()
        } else {
            emptyList()
        }
    }

data class AccountInfoCount(
    val cloudFavorites: Int,
    val localFavorites: Int,
    val notifications: Int,
    val incognitoSources: Int,
    val history: Int,
    val lists: Int,
    val itemsInLists: Int,
    val chapters: Int,
    val blurHashes: Int,
    val translationModels: Int,
    val sourceCount: Int,
    val globalSearchHistory: Int,
    val savedRecommendations: Int,
    val timeSpentDoing: String,
    val heatMaps: List<Heat<Int>>,
) {
    @OptIn(ExperimentalTime::class)
    constructor(array: Array<Int>) : this(
        cloudFavorites = array[0],
        localFavorites = array[1],
        notifications = array[2],
        incognitoSources = array[3],
        history = array[4],
        lists = array[5],
        itemsInLists = array[6],
        chapters = array[7],
        blurHashes = array[8],
        translationModels = array[9],
        sourceCount = array[10],
        globalSearchHistory = array[11],
        savedRecommendations = array[12],
        timeSpentDoing = "0 seconds",
        heatMaps = generateHeats(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
    )

    val totalFavorites: Int
        get() = cloudFavorites + localFavorites

    companion object {
        @OptIn(ExperimentalTime::class)
        val Empty = AccountInfoCount(
            cloudFavorites = 0,
            localFavorites = 0,
            notifications = 0,
            incognitoSources = 0,
            history = 0,
            lists = 0,
            itemsInLists = 0,
            chapters = 0,
            blurHashes = 0,
            translationModels = 0,
            sourceCount = 0,
            globalSearchHistory = 0,
            savedRecommendations = 0,
            timeSpentDoing = "0 seconds",
            heatMaps = generateHeats(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
        )
    }
}