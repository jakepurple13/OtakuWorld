package com.programmersbox.kmpuiviews.repository

import androidx.compose.ui.util.fastMaxBy
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.utils.FireListenerClosable
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine

class FavoritesRepository(
    private val dao: ItemDao,
    private val firebaseDb: KmpFirebaseConnection,
) {

    suspend fun isIncognito(source: String): Boolean {
        val incognito = dao.getIncognitoSourceByNameSync(source)
        return incognito != null && incognito.isIncognito
    }

    suspend fun addFavorite(db: DbModel) {
        if (isIncognito(db.source)) return
        dao.insertFavorite(db)
        firebaseDb.insertShowFlow(db).collect()
    }

    suspend fun removeFavorite(db: DbModel) {
        if (isIncognito(db.source)) return
        dao.deleteFavorite(db)
        firebaseDb.removeShowFlow(db).collect()
    }

    suspend fun addWatched(chapterWatched: ChapterWatched) {
        dao.insertChapter(chapterWatched)
        firebaseDb.insertEpisodeWatchedFlow(chapterWatched).collect()
    }

    suspend fun removeWatched(chapterWatched: ChapterWatched) {
        dao.deleteChapter(chapterWatched)
        firebaseDb.removeEpisodeWatchedFlow(chapterWatched).collect()
    }

    suspend fun toggleNotify(db: DbModel) {
        dao.updateFavoriteItem(db)
        firebaseDb.toggleUpdateCheckShowFlow(db).collect()
    }

    suspend fun getAllFavorites() = listOf(
        dao.getAllFavoritesSync(),
        firebaseDb.getAllShows().requireNoNulls()
    )
        .flatten()
        .groupBy(DbModel::url)
        .map { it.value.fastMaxBy(DbModel::numChapters)!! }

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
