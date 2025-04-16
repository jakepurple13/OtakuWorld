package com.programmersbox.uiviews.repository

import androidx.compose.ui.util.fastMaxBy
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.utils.FireListenerClosable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine

class FavoritesRepository(
    private val dao: ItemDao,
) {

    suspend fun isIncognito(source: String): Boolean {
        val incognito = dao.getIncognitoSourceByNameSync(source)
        return incognito != null && incognito.isIncognito
    }

    suspend fun addFavorite(db: DbModel) {
        if (isIncognito(db.source)) return
        dao.insertFavorite(db)
        FirebaseDb.insertShowFlow(db).collect()
    }

    suspend fun removeFavorite(db: DbModel) {
        if (isIncognito(db.source)) return
        dao.deleteFavorite(db)
        FirebaseDb.removeShowFlow(db).collect()
    }

    suspend fun addWatched(chapterWatched: ChapterWatched) {
        dao.insertChapter(chapterWatched)
        FirebaseDb.insertEpisodeWatchedFlow(chapterWatched).collect()
    }

    suspend fun removeWatched(chapterWatched: ChapterWatched) {
        dao.deleteChapter(chapterWatched)
        FirebaseDb.removeEpisodeWatchedFlow(chapterWatched).collect()
    }

    suspend fun toggleNotify(db: DbModel) {
        dao.updateFavoriteItem(db)
        FirebaseDb.toggleUpdateCheckShowFlow(db).collect()
    }

    fun isFavorite(
        url: String,
        fireListenerClosable: FireListenerClosable,
    ) = combine(
        fireListenerClosable.findItemByUrlFlow(url),
        dao.containsItem(url)
    ) { f, d -> f || d }

    fun getChapters(
        url: String,
        fireListenerClosable: FireListenerClosable,
    ) = combine(
        fireListenerClosable.getAllEpisodesByShowFlow(url),
        dao.getAllChapters(url)
    ) { f, d -> (f + d).distinctBy { it.url } }

    fun getModel(
        url: String,
        fireListenerClosable: FireListenerClosable,
    ) = combine(
        fireListenerClosable.getShowFlow(url),
        dao.getDbModel(url),
    ) { d, f -> d ?: f }

    fun getAllFavorites(fireListenerClosable: FireListenerClosable) = combine(
        fireListenerClosable.getAllShowsFlow(),
        dao.getAllFavorites()
    ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }

}
