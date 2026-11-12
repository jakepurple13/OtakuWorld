package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstatdetails

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class AccountStatDetailsViewModel(
    itemDao: ItemDao,
    sourceRepository: SourceRepository,
) : ViewModel() {

    val state: StateFlow<AccountStatDetailsState>
        field = MutableStateFlow(AccountStatDetailsState())

    // One color per source, shared by the chart and the list and kept across updates.
    private val sourceColors = mutableMapOf<String, Color>()

    init {
        itemDao
            .getAllFavorites()
            .map { list ->
                list
                    .groupBy { it.source }
                    .mapValues { it.value.size }
                    .map {
                        FavoriteInfo(
                            source = it.key,
                            name = sourceRepository.displayNameFor(it.key),
                            count = it.value.toLong(),
                            color = sourceColors.getOrPut(it.key) { Random.nextRandomColor() }
                        )
                    }
            }
            .flowOn(Dispatchers.IO)
            .onEach { map ->
                val total = map.sumOf { it.count }
                state.update { accountState ->
                    accountState.copy(
                        favorites = map.sortedByDescending { it.count },
                        favoritesCount = total,
                        circleInfo = map.map { favorites ->
                            CircleInfo(
                                color = favorites.color,
                                value = favorites.count.toFloat() / total,
                                label = favorites.name,
                                key = favorites.source,
                                extraInfo = mapOf(COUNT_KEY to favorites.count),
                            )
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Generates a fully opaque random Jetpack Compose Color.
     */
    private fun Random.nextRandomColor(): Color {
        return Color(
            red = nextFloat(),
            green = nextFloat(),
            blue = nextFloat(),
            alpha = 1f // Change to Random.nextFloat() if you want random transparency
        )
    }
}

@Stable
data class AccountStatDetailsState(
    val favorites: List<FavoriteInfo> = emptyList(),
    val favoritesCount: Long = 0,
    val circleInfo: List<CircleInfo> = emptyList(),
)

internal const val COUNT_KEY = "count"

data class FavoriteInfo(
    val source: String,
    val name: String,
    val count: Long,
    val color: Color,
)