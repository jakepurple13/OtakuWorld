package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.kmpuiviews.telemetry.TelemetrySourceNamer
import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.NoOpAnalytics
import com.programmersbox.telemetry.events.FavoriteAdded
import com.programmersbox.telemetry.events.FavoriteRemoved
import com.programmersbox.telemetry.events.UpdateCheckToggled
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock

class FavoritesRepository(
    private val dao: ItemDao,
    private val systemAlerter: SystemAlerter,
    private val authManager: AuthManager,
    private val sourceRepository: SourceRepository,
    private val clock: Clock = Clock.System,
    private val analytics: Analytics = NoOpAnalytics,
    private val sourceNamer: TelemetrySourceNamer? = null,
) {
    private fun telemetryName(source: String) = sourceNamer?.name(source) ?: source

    /**
     * [source] is an item url or a [com.programmersbox.kmpmodels.KmpApiService.serviceName].
     * Source rows are keyed by package name, so a service name is resolved to its package first.
     */
    suspend fun isIncognito(source: String): Boolean {
        if (dao.getIncognitoSourceSync(source)?.isIncognito == true) return true
        val packageName = sourceRepository.toSourceByApiServiceName(source)?.packageName ?: return false
        return dao.getIncognitoSourceSync(packageName)?.isIncognito == true
    }

    /** [from] is set only for user actions (OpenedFrom); restores and reader auto-saves pass null and send nothing. */
    suspend fun addFavorite(db: DbModel, from: String? = null) {
        if (isIncognito(db.source)) return
        if (from != null) analytics.log(FavoriteAdded(telemetryName(db.source), from))
        coroutineScope {
            launch {
                dao.insertFavorite(
                    if (db.createdAt == 0L) db.copy(createdAt = Clock.System.now().toEpochMilliseconds()) else db
                )
                systemAlerter.alertFavoritesChange()
            }
        }
    }

    suspend fun removeFavorite(db: DbModel, userAction: Boolean = false) {
        if (isIncognito(db.source)) return
        if (userAction) analytics.log(FavoriteRemoved(telemetryName(db.source)))
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

    suspend fun addWatched(chapterWatched: ChapterWatched, stampReadTime: Boolean = true) {
        if (isIncognito(chapterWatched.favoriteUrl)) return
        coroutineScope {
            launch {
                // Only reader reads are stamped; an unstamped row (createdAt 0) never counts as read that day.
                val now = clock.now().toEpochMilliseconds()
                dao.insertChapter(if (stampReadTime) chapterWatched.copy(createdAt = now, updatedAt = now) else chapterWatched)
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
        analytics.log(UpdateCheckToggled(enabled = db.shouldCheckForUpdate))
        coroutineScope {
            launch {
                dao.updateFavoriteItem(
                    db.copy(isDirty = authManager.isLoggedIn(), updatedAt = clock.now().toEpochMilliseconds())
                )
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
