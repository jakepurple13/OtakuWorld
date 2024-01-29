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

    @Delete
    suspend fun deleteFavorite(model: DbModel)

    @Query("SELECT * FROM FavoriteItem")
    fun getAllFavorites(): Flow<List<DbModel>>

    @Query("SELECT * FROM FavoriteItem")
    fun getAllFavoritesSync(): List<DbModel>

    @Query("SELECT * FROM FavoriteItem where shouldCheckForUpdate = 1")
    fun getAllNotifyingFavoritesSync(): List<DbModel>

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(notificationItem: NotificationItem)

    @Delete
    suspend fun deleteNotification(notificationItem: NotificationItem)

    @Query("DELETE FROM Notifications")
    suspend fun deleteAllNotifications(): Int

    @Query("SELECT * FROM Notifications where url = :url")
    fun getNotificationItem(url: String): NotificationItem

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

}