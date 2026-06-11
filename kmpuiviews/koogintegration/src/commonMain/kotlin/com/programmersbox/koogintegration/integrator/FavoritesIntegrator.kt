package com.programmersbox.koogintegration.integrator

import com.programmersbox.favoritesdatabase.ItemDao

class FavoritesIntegrator(
    private val itemDao: ItemDao,
) : KoogIntegrator<String>() {
    override suspend fun map(input: String): String {
        return buildString {
            appendLine("Favorites")
            itemDao
                .getAllFavoritesSync()
                .forEach { item ->
                    appendLine(item.source)
                    appendLine(item.title)
                    appendLine(item.description)
                }
        }
    }

    suspend fun fetchFavorites() = itemDao.getAllFavoritesSync()
    suspend fun getFavoritesByUrls(urls: List<String>) = itemDao.getFavoritesByUrls(urls)
}