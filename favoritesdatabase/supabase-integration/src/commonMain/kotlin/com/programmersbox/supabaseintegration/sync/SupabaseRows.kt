package com.programmersbox.supabaseintegration.sync

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteItemRow(
    @SerialName("user_id") val userId: String,
    val url: String,
    val title: String,
    val description: String,
    @SerialName("image_url") val imageUrl: String,
    val source: String,
    @SerialName("num_chapters") val numChapters: Int,
    @SerialName("should_check_for_update") val shouldCheckForUpdate: Boolean,
    @SerialName("supabase_id") val supabaseId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

fun FavoriteItemRow.toDbModel() = DbModel(
    url = url, title = title, description = description,
    imageUrl = imageUrl, source = source, numChapters = numChapters,
    shouldCheckForUpdate = shouldCheckForUpdate,
    supabaseId = supabaseId, createdAt = createdAt, updatedAt = updatedAt,
    isDeleted = isDeleted, isDirty = false,
)

fun DbModel.toFavoriteRow(userId: String, timestamp: Long = updatedAt) = FavoriteItemRow(
    userId = userId, url = url, title = title, description = description,
    imageUrl = imageUrl, source = source, numChapters = numChapters,
    shouldCheckForUpdate = shouldCheckForUpdate,
    supabaseId = supabaseId, createdAt = createdAt, updatedAt = timestamp,
    isDeleted = isDeleted,
)

@Serializable
data class ChapterWatchedRow(
    @SerialName("user_id") val userId: String,
    val url: String,
    val name: String,
    @SerialName("favorite_url") val favoriteUrl: String,
    @SerialName("supabase_id") val supabaseId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

fun ChapterWatchedRow.toChapterWatched() = ChapterWatched(
    url = url, name = name, favoriteUrl = favoriteUrl,
    supabaseId = supabaseId, createdAt = createdAt, updatedAt = updatedAt,
    isDeleted = isDeleted, isDirty = false,
)

fun ChapterWatched.toChapterRow(userId: String, timestamp: Long = updatedAt) = ChapterWatchedRow(
    userId = userId, url = url, name = name, favoriteUrl = favoriteUrl,
    supabaseId = supabaseId, createdAt = createdAt, updatedAt = timestamp,
    isDeleted = isDeleted,
)
