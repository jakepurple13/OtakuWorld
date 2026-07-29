package com.programmersbox.kmpuiviews.presentation.settings.accountinfo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.utils.DateFormatItem
import com.programmersbox.kmpuiviews.utils.KmpHeat
import com.programmersbox.sharedcomponents.stats.StatisticsProvider
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.auth.SupabaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class AccountInfoViewModel(
    itemDao: ItemDao,
    heatMapDao: HeatMapDao,
    activityDao: ActivityDao,
    authManager: AuthManager,
    providers: List<StatisticsProvider>,
) : ViewModel() {

    var accountInfo by mutableStateOf(AccountInfoCount.Empty)
        private set

    var supabaseInfo by mutableStateOf<SupabaseUser?>(null)
        private set

    val uiState = combine(
        providers.map { it.getStats() }
    ) { items -> items.sortedBy { it.priority } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        authManager
            .authState
            .onEach {
                if (it is AuthState.Authenticated) supabaseInfo = it.user
                else if (it is AuthState.Unauthenticated) supabaseInfo = null
            }
            .launchIn(viewModelScope)

        combine(
            itemDao.getAllFavoritesCount(),
            itemDao.getAllChaptersCount(),
            heatMapDao.getDailyAverage()
        ) { AccountInfoCount(it) }
            .combine(activityDao.observeActivity()) { a, b ->
                a.copy(timeSpentDoing = (b?.cumulativeSeconds ?: 0L).seconds.toString())
            }
            .combine(heatMapDao.getAllHeatMaps()) { a, b ->
                a.copy(heatMaps = generateHeats(b))
            }
            .combine(heatMapDao.getHighestActiveCountItem()) { a, b ->
                a.copy(
                    topHeatMap =
                        b?.let { TopHeatMapItem(DateFormatItem.format(it.time), it.count) }
                )
            }
            .flowOn(Dispatchers.IO)
            .onEach { accountInfo = it }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalTime::class)
    private fun generateHeats(
        heatItems: List<HeatMapItem>,
    ): List<KmpHeat<Int>> {
        val startDate = heatItems.minByOrNull { item -> item.time.toEpochDays() }?.time
        val curDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        return generateSequence(startDate) { date ->
            if (date < curDate) date + DatePeriod(days = 1) else null
        }.map { date ->
            val current = heatItems.find { it.time == date }
            KmpHeat(
                current?.time ?: date,
                current?.count?.toDouble() ?: 0.0,
                current?.count ?: 0
            )
        }.toList()
    }
}

@Stable
data class TopHeatMapItem(
    val time: String,
    val count: Int,
)

@Stable
data class AccountInfoCount(
    val localFavorites: Int,
    val chapters: Int,
    val timeSpentDoing: String,
    val heatMaps: List<KmpHeat<Int>>,
    val dailyAverage: Int,
    val topHeatMap: TopHeatMapItem?,
) {
    @OptIn(ExperimentalTime::class)
    constructor(array: Array<Int>) : this(
        localFavorites = array[0],
        chapters = array[1],
        timeSpentDoing = "0 seconds",
        heatMaps = listOf(),
        dailyAverage = array[2],
        topHeatMap = null
    )

    val totalFavorites: Int
        get() = localFavorites

    companion object {
        @OptIn(ExperimentalTime::class)
        val Empty = AccountInfoCount(
            localFavorites = 0,
            chapters = 0,
            timeSpentDoing = "0 seconds",
            heatMaps = listOf(),
            dailyAverage = 0,
            topHeatMap = null
        )
    }
}