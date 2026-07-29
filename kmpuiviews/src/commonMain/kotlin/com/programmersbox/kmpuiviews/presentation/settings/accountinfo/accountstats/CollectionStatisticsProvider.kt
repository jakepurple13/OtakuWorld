package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstats

import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.DictionaryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedcomponents.stats.StatData
import com.programmersbox.sharedcomponents.stats.StatisticsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

class CollectionStatisticsProvider(
    private val itemDao: ItemDao,
    private val bookmarkDao: BookmarkDao,
    private val dictionaryDao: DictionaryDao,
) : StatisticsProvider() {
    override val header: String = "⭐ Collection"
    override val contentType: String = "collection"
    override val key: String = "collection"
    override val priority: Int = 1

    override fun observeStats(): Flow<List<StatData>> = combine(
        itemDao.getAllFavoritesCount(),
        itemDao.getAllNotificationCount(),
        itemDao.getAllIncognitoSourcesCount(),
        bookmarkDao.getAllBookmarksCount(),
        dictionaryDao.getCount()
    ) { favorites, notifications, incognitoSources, bookmarks, dictionary ->
        listOf(
            StatData(
                id = "favorites",
                label = "Favorites",
                description = "Items added to favorites",
                value = favorites.toString()
            ),
            StatData(
                id = "notifications",
                label = "Notifications",
                description = "Pending update notifications",
                value = notifications.toString()
            ),
            StatData(
                id = "incognitoSources",
                label = "Incognito Sources",
                description = "Sources browsed privately",
                value = incognitoSources.toString()
            ),
            StatData(
                id = "bookmarks",
                label = "Bookmarks",
                description = "Chapters or Episodes bookmarked",
                value = bookmarks.toString()
            ),
            StatData(
                id = "dictionary",
                label = "Dictionary Entries",
                description = "Entries in the dictionary",
                value = dictionary.toString()
            )
        )
    }
        .onStart {
            emit(
                listOf(
                    StatData(
                        id = "favorites",
                        label = "Favorites",
                        description = "Items added to favorites",
                        value = "0"
                    ),
                    StatData(
                        id = "notifications",
                        label = "Notifications",
                        description = "Pending update notifications",
                        value = "0"
                    ),
                    StatData(
                        id = "incognitoSources",
                        label = "Incognito Sources",
                        description = "Sources browsed privately",
                        value = "0"
                    ),
                    StatData(
                        id = "bookmarks",
                        label = "Bookmarks",
                        description = "Chapters or Episodes bookmarked",
                        value = "0"
                    ),
                    StatData(
                        id = "dictionary",
                        label = "Dictionary Entries",
                        description = "Entries in the dictionary",
                        value = "0"
                    )
                )
            )
        }
}