package com.programmersbox.favoritesdatabase

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Flowable

@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFavorite(model: DbModel): Completable

    @Delete
    fun deleteFavorite(model: DbModel): Completable

    @Query("SELECT * FROM FavoriteItem")
    fun getAllFavorites(): Flowable<List<DbModel>>

    @Query("SELECT COUNT(*) FROM FavoriteItem WHERE url = :url")
    fun getItemById(url: String): Flowable<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChapter(vachapterWatched: ChapterWatched): Completable

    @Delete
    fun deleteChapter(chapterWatched: ChapterWatched): Completable

    @Query("SELECT * FROM ChapterWatched where favoriteUrl = :url")
    fun getAllChapters(url: String): Flowable<List<ChapterWatched>>

}