package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts5
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.time.Clock

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
    var numChapters: Int = 0,
    @ColumnInfo(name = "shouldCheckForUpdate", defaultValue = "true")
    val shouldCheckForUpdate: Boolean = true,
    @ColumnInfo(name = "supabase_id", defaultValue = "")
    val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1")
    val isDirty: Boolean = true,
)

/** FTS5 index over favorite titles and descriptions; Room keeps it in sync with [DbModel]. */
@Entity(tableName = "favorites_fts")
@Fts5(
    contentEntity = DbModel::class,
    tokenizerArgs = ["remove_diacritics", "2"],
    prefix = [2, 3],
)
data class DbModelFts(
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
)

/**
 * One favorites search result. Highlight and snippet text mark matched words with
 * `\u0002` (start) and `\u0003` (end).
 */
data class FavoriteFtsHit(
    val url: String,
    val score: Double,
    val titleHighlight: String,
    val descriptionSnippet: String,
)

@Serializable
@Entity(tableName = "ChapterWatched")
data class ChapterWatched(
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "favoriteUrl")
    val favoriteUrl: String,
    @ColumnInfo(name = "supabase_id", defaultValue = "")
    val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1")
    val isDirty: Boolean = true,
)

@Serializable
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
    val contentTitle: String,
    @ColumnInfo(name = "isShowing", defaultValue = "0")
    val isShowing: Boolean = false,
    @ColumnInfo(name = "supabase_id", defaultValue = "")
    val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1")
    val isDirty: Boolean = true,
)

@Serializable
@Entity("SourceOrder")
data class SourceOrder(
    @PrimaryKey
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "order")
    val order: Int,
    @ColumnInfo(name = "supabase_id", defaultValue = "")
    val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1")
    val isDirty: Boolean = true,
)

@Serializable
@Entity("IncognitoSourceTable")
data class IncognitoSource(
    @PrimaryKey
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "isIncognito", defaultValue = "0")
    val isIncognito: Boolean,
    @ColumnInfo(name = "supabase_id", defaultValue = "")
    val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1")
    val isDirty: Boolean = true,
)