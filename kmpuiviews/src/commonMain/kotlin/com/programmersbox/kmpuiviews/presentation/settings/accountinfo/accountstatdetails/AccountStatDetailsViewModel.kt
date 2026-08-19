package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstatdetails

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
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
) : ViewModel() {

    val state: StateFlow<AccountStatDetailsState>
        field = MutableStateFlow(AccountStatDetailsState())

    init {
        itemDao
            .getAllFavorites()
            .map { list ->
                list
                    .groupBy { it.source }
                    .mapValues { it.value.size }
                    .map {
                        FavoriteInfo(
                            name = it.key,
                            count = it.value.toLong(),
                            color = Random.nextRandomColor()
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
                                color = Random.nextRandomColor(),
                                value = favorites.count.toFloat() / total,
                                label = favorites.name,
                                key = favorites.count
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

data class FavoriteInfo(
    val name: String,
    val count: Long,
    val color: Color,
)