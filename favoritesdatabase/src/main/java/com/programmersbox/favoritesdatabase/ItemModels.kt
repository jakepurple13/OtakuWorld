package com.programmersbox.favoritesdatabase

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "FavoriteItem")
data class DbModel(
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "imageUrl")
    val imageUrl: String,
    @ColumnInfo(name = "sources")
    val source: String,
    @ColumnInfo(name = "numChapters", defaultValue = "0")
    var numChapters: Int = 0
)

@Entity(tableName = "ChapterWatched")
data class ChapterWatched(
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "favoriteUrl")
    val favoriteUrl: String
)

@Entity(tableName = "Notifications")
data class NotificationItem(
    @ColumnInfo(name = "id")
    val id: Int,
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "summaryText")
    val summaryText: String,
    @ColumnInfo(name = "notiTitle")
    val notiTitle: String,
    @ColumnInfo(name = "notiPicture")
    val imageUrl: String?,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "contentTitle")
    val contentTitle: String
)