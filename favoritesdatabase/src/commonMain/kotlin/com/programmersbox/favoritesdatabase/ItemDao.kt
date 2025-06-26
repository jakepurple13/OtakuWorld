package com.programmersbox.favoritesdatabase

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(model: DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(vararg model: DbModel)

    @Delete
    suspend fun deleteFavorite(model: DbModel)

    @Query("SELECT * FROM FavoriteItem")
    fun getAllFavorites(): Flow<List<DbModel>>

    @Query("SELECT COUNT(url) FROM FavoriteItem")
    fun getAllFavoritesCount(): Flow<Int>

    @Query("SELECT * FROM FavoriteItem")
    suspend fun getAllFavoritesSync(): List<DbModel>

    @Query("SELECT * FROM FavoriteItem where shouldCheckForUpdate = 1")
    suspend fun getAllNotifyingFavoritesSync(): List<DbModel>

    @Query("SELECT * FROM FavoriteItem where shouldCheckForUpdate = 1")
    fun getAllNotifyingFavorites(): Flow<List<DbModel>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFavoriteItem(dbModel: DbModel)

    @Query("SELECT * FROM FavoriteItem WHERE url=:url")
    fun getDbModel(url: String): Flow<DbModel?>

    @Query("SELECT EXISTS(SELECT * FROM FavoriteItem WHERE url=:url)")
    fun containsItem(url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapterWatched: ChapterWatched)

    @Delete
    suspend fun deleteChapter(chapterWatched: ChapterWatched)

    @Query("SELECT * FROM ChapterWatched where favoriteUrl = :url")
    fun getAllChapters(url: String): Flow<List<ChapterWatched>>

    @Query("SELECT COUNT(url) FROM ChapterWatched")
    fun getAllChaptersCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(notificationItem: NotificationItem)

    @Delete
    suspend fun deleteNotification(notificationItem: NotificationItem)

    @Query("UPDATE Notifications SET isShowing = :isShowing WHERE url = :url")
    suspend fun updateNotification(url: String, isShowing: Boolean)

    @Query("UPDATE Notifications SET isShowing = :isShowing WHERE id = :id")
    suspend fun updateNotification(id: Int, isShowing: Boolean)

    @Update
    suspend fun updateNotification(notificationItem: NotificationItem)

    @Query("SELECT COUNT(isShowing) FROM Notifications where isShowing = 1")
    suspend fun getAllShowingNotificationsCount(): Int

    @Query("DELETE FROM Notifications")
    suspend fun deleteAllNotifications(): Int

    @Query("SELECT * FROM Notifications where url = :url")
    suspend fun getNotificationItem(url: String): NotificationItem?

    @Query("SELECT * FROM Notifications where id = :id")
    suspend fun getNotificationItemById(id: Int): NotificationItem?

    @Query("SELECT * FROM Notifications where url = :url")
    fun getNotificationItemFlow(url: String): Flow<NotificationItem?>

    @Query("SELECT EXISTS(SELECT 1 FROM Notifications WHERE url = :url)")
    fun doesNotificationExistFlow(url: String): Flow<Boolean>

    @Query("SELECT * FROM Notifications")
    fun getAllNotificationsFlow(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM Notifications")
    suspend fun getAllNotifications(): List<NotificationItem>

    @Query("SELECT * FROM Notifications")
    fun getAllNotificationsFlowPaging(): PagingSource<Int, NotificationItem>

    @Query("SELECT COUNT(id) FROM Notifications")
    fun getAllNotificationCount(): Flow<Int>

    @Query("SELECT * FROM SourceOrder ORDER BY `order` ASC")
    fun getSourceOrder(): Flow<List<SourceOrder>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSourceOrder(sourceOrder: SourceOrder)

    @Query("SELECT * FROM SourceOrder ORDER BY `order` ASC")
    suspend fun getSourceOrderSync(): List<SourceOrder>

    @Query("DELETE FROM SourceOrder")
    suspend fun deleteAllSourceOrder()

    @Query("DELETE FROM SourceOrder where source = :source")
    suspend fun deleteSourceOrder(source: String)

    @Query("UPDATE SourceOrder SET `order` = :order WHERE source = :source")
    suspend fun updateSourceOrder(source: String, order: Int)

    @Update
    suspend fun updateSourceOrder(sourceOrder: SourceOrder)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIncognitoSource(incognitoSource: IncognitoSource)

    @Query("SELECT * FROM IncognitoSourceTable")
    fun getAllIncognitoSources(): Flow<List<IncognitoSource>>

    @Query("SELECT COUNT(source) FROM IncognitoSourceTable")
    fun getAllIncognitoSourcesCount(): Flow<Int>

    @Query("SELECT * FROM IncognitoSourceTable")
    suspend fun getAllIncognitoSourcesSync(): List<IncognitoSource>

    @Query("DELETE FROM IncognitoSourceTable")
    suspend fun deleteAllIncognitoSources()

    @Query("DELETE FROM IncognitoSourceTable where source = :source")
    suspend fun deleteIncognitoSource(source: String)

    @Update
    suspend fun updateIncognitoSource(incognitoSource: IncognitoSource)

    @Query("SELECT EXISTS(SELECT 1 FROM IncognitoSourceTable WHERE source = :source)")
    fun doesIncognitoSourceExist(source: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM IncognitoSourceTable WHERE source = :source)")
    suspend fun doesIncognitoSourceExistSync(source: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM IncognitoSourceTable WHERE name = :name)")
    suspend fun doesIncognitoSourceExistByNameSync(name: String): Boolean

    @Query("SELECT * FROM IncognitoSourceTable WHERE source = :source")
    fun getIncognitoSource(source: String): Flow<IncognitoSource?>

    @Query("SELECT * FROM IncognitoSourceTable WHERE name = :name")
    fun getIncognitoSourceByName(name: String): Flow<IncognitoSource?>

    @Query("SELECT * FROM IncognitoSourceTable WHERE source = :source")
    suspend fun getIncognitoSourceSync(source: String): IncognitoSource?

    @Query("SELECT * FROM IncognitoSourceTable WHERE name = :name")
    suspend fun getIncognitoSourceByNameSync(name: String): IncognitoSource?

    @Query("UPDATE IncognitoSourceTable SET isIncognito = :isIncognito WHERE source = :source")
    suspend fun updateIncognitoSource(source: String, isIncognito: Boolean)
}