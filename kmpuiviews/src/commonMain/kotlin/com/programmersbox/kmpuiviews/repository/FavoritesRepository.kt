package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.supabaseintegration.auth.AuthManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock

class FavoritesRepository(
    private val dao: ItemDao,
    private val systemAlerter: SystemAlerter,
    private val authManager: AuthManager,
) {
    suspend fun isIncognito(source: String): Boolean {
        //TODO: Maybe also allow specific items?
        // Not many changes would be needed. Just some up changes
        val incognito = dao.getIncognitoSourceSync(source)
        return incognito != null && incognito.isIncognito
    }

    suspend fun addFavorite(db: DbModel) {
        if (isIncognito(db.source)) return
        coroutineScope {
            launch {
                dao.insertFavorite(db)
                systemAlerter.alertFavoritesChange()
            }
        }
    }

    suspend fun removeFavorite(db: DbModel) {
        if (isIncognito(db.source)) return
        coroutineScope {
            launch {
                if (authManager.isLoggedIn()) {
                    dao.softDeleteFavorite(db.url, Clock.System.now().toEpochMilliseconds())
                } else {
                    dao.deleteFavorite(db)
                }
                systemAlerter.alertFavoritesChange()
            }
        }
    }

    suspend fun addWatched(chapterWatched: ChapterWatched) {
        if (isIncognito(chapterWatched.favoriteUrl)) return
        coroutineScope {
            launch {
                dao.insertChapter(chapterWatched)
                systemAlerter.alertChapterChange()
            }
        }
    }

    suspend fun removeWatched(chapterWatched: ChapterWatched) {
        if (isIncognito(chapterWatched.favoriteUrl)) return
        coroutineScope {
            launch {
                if (authManager.isLoggedIn()) {
                    dao.softDeleteChapter(chapterWatched.url, Clock.System.now().toEpochMilliseconds())
                } else {
                    dao.deleteChapter(chapterWatched)
                }
                systemAlerter.alertChapterChange()
            }
        }
    }

    suspend fun toggleNotify(db: DbModel) {
        coroutineScope {
            launch {
                dao.updateFavoriteItem(db.copy(isDirty = authManager.isLoggedIn()))
                systemAlerter.alertFavoritesChange()
            }
        }
    }

    suspend fun getAllFavorites() = dao.getAllFavoritesSync()

    fun isFavorite(
        url: String,
    ) = dao.containsItem(url)

    fun getChapters(
        url: String,
    ) = dao.getAllChapters(url)

    fun getChaptersLocal(
        url: String,
    ) = dao.getAllChapters(url)

    fun getModel(
        url: String,
    ) = dao.getDbModel(url)

    fun getAllFavoritesFlow() = dao.getAllFavorites()
}
