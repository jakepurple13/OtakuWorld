package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstats

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.domain.moodtags.MoodRepository
import com.programmersbox.kmpuiviews.domain.moodtags.Moods
import com.programmersbox.sharedcomponents.stats.StatData
import com.programmersbox.sharedcomponents.stats.StatisticsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.programmersbox.kmpuiviews.presentation.favorite.groupFavorites
import com.programmersbox.kmpuiviews.presentation.favorite.moods
import org.jetbrains.compose.resources.getString

/** How many favorites have moods, and the top 3 moods. Hidden (no rows) until a favorite has a mood. */
class MoodStatisticsProvider(
    private val itemDao: ItemDao,
    private val moodRepository: MoodRepository,
) : StatisticsProvider() {
    override val header: String = "🏷 Moods"
    override val key: String = "moods"
    override val contentType: String = "moods"
    override val priority: Int = 2

    // Counted per title, like the Favorites filter: a title saved from two sources is one favorite
    override fun observeStats(): Flow<List<StatData>> = combine(
        itemDao.getAllFavorites(),
        moodRepository.favoriteMoods,
    ) { favorites, moodsByUrl ->
        val groups = groupFavorites(favorites)
        val taggedMoods = groups.map { it.moods(moodsByUrl) }.filter { it.isNotEmpty() }
        if (taggedMoods.isEmpty()) return@combine emptyList()

        // Each title's moods are already distinct, so a mood's list size is how many titles have it
        val top = taggedMoods.flatten()
            .groupBy { it.lowercase() }
            .map { (_, names) -> names.first() to names.size }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase() })
            .take(TOP_MOODS)

        val places = listOf("Top mood", "Second", "Third")
        listOf(
            StatData(
                id = "tagged",
                label = "Tagged favorites",
                description = "Add moods from a favorite's page",
                value = "${taggedMoods.size} of ${groups.size}",
            )
        ) + top.mapIndexed { index, (mood, count) ->
            val name = Moods.label(mood)?.let { getString(it) } ?: mood
            StatData(
                id = "top$index",
                label = places[index],
                description = "Favorites with this mood",
                value = "$name · $count",
            )
        }
    }

    private companion object {
        const val TOP_MOODS = 3
    }
}
