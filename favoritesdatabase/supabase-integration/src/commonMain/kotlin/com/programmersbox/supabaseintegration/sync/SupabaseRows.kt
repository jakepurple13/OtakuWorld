package com.programmersbox.supabaseintegration.sync

import com.programmersbox.favoritesdatabase.ActivityTable
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.favoritesdatabase.NoteItem
import kotlinx.datetime.LocalDate
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

@Serializable
data class BookmarkedChapterRow(
    @SerialName("user_id") val userId: String,
    @SerialName("chapter_url") val chapterUrl: String,
    @SerialName("chapter_name") val chapterName: String,
    @SerialName("parent_url") val parentUrl: String,
    @SerialName("parent_title") val parentTitle: String,
    @SerialName("parent_image_url") val parentImageUrl: String,
    val source: String,
    val timestamp: Long = 0L,
    @SerialName("supabase_id") val supabaseId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

fun BookmarkedChapterRow.toBookmarkedChapter() = BookmarkedChapter(
    chapterUrl = chapterUrl,
    chapterName = chapterName,
    parentUrl = parentUrl,
    parentTitle = parentTitle,
    parentImageUrl = parentImageUrl,
    source = source,
    timestamp = timestamp,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    isDirty = false,
)

fun BookmarkedChapter.toBookmarkedChapterRow(userId: String, timestamp: Long = updatedAt) = BookmarkedChapterRow(
    userId = userId,
    chapterUrl = chapterUrl,
    chapterName = chapterName,
    parentUrl = parentUrl,
    parentTitle = parentTitle,
    parentImageUrl = parentImageUrl,
    source = source,
    timestamp = this.timestamp,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = timestamp,
    isDeleted = isDeleted,
)

@Serializable
data class NoteItemRow(
    @SerialName("user_id") val userId: String,
    @SerialName("item_url") val itemUrl: String,
    @SerialName("item_title") val itemTitle: String,
    val content: String,
    val timestamp: Long = 0L,
    @SerialName("supabase_id") val supabaseId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

fun NoteItemRow.toNoteItem() = NoteItem(
    itemUrl = itemUrl,
    itemTitle = itemTitle,
    content = content,
    timestamp = timestamp,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    isDirty = false,
)

fun NoteItem.toNoteItemRow(userId: String, timestamp: Long = updatedAt) = NoteItemRow(
    userId = userId,
    itemUrl = itemUrl,
    itemTitle = itemTitle,
    content = content,
    timestamp = this.timestamp,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = timestamp,
    isDeleted = isDeleted,
)

@Serializable
data class HistoryItemRow(
    @SerialName("user_id") val userId: String,
    @SerialName("search_text") val searchText: String,
    val time: Long = 0L,
    @SerialName("supabase_id") val supabaseId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

fun HistoryItemRow.toHistoryItem() = HistoryItem(
    searchText = searchText,
    time = time,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    isDirty = false,
)

fun HistoryItem.toHistoryItemRow(userId: String, timestamp: Long = updatedAt) = HistoryItemRow(
    userId = userId,
    searchText = searchText,
    time = time,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = timestamp,
    isDeleted = isDeleted,
)

@Serializable
data class CustomListItemRow(
    @SerialName("user_id") val userId: String,
    val uuid: String,
    val name: String,
    val time: Long = 0L,
    @SerialName("use_biometric") val useBiometric: Boolean = false,
    val description: String = "",
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("supabase_id") val supabaseId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

fun CustomListItemRow.toCustomListItem() = CustomListItem(
    uuid = uuid,
    name = name,
    time = time,
    useBiometric = useBiometric,
    description = description,
    coverImageUrl = coverImageUrl,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    isDirty = false,
)

fun CustomListItem.toCustomListItemRow(userId: String, timestamp: Long = updatedAt) = CustomListItemRow(
    userId = userId,
    uuid = uuid,
    name = name,
    time = time,
    useBiometric = useBiometric,
    description = description,
    coverImageUrl = coverImageUrl,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = timestamp,
    isDeleted = isDeleted,
)

@Serializable
data class CustomListInfoRow(
    @SerialName("user_id") val userId: String,
    @SerialName("unique_id") val uniqueId: String,
    val uuid: String,
    val title: String = "",
    val description: String = "",
    val url: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val source: String = "",
    @SerialName("supabase_id") val supabaseId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

fun CustomListInfoRow.toCustomListInfo() = CustomListInfo(
    uniqueId = uniqueId,
    uuid = uuid,
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = source,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    isDirty = false,
)

fun CustomListInfo.toCustomListInfoRow(userId: String, timestamp: Long = updatedAt) = CustomListInfoRow(
    userId = userId,
    uniqueId = uniqueId,
    uuid = uuid,
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = source,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = timestamp,
    isDeleted = isDeleted,
)

@Serializable
data class HeatMapItemRow(
    @SerialName("user_id") val userId: String,
    val time: String,
    @SerialName("day_count") val dayCount: Int = 0,
    @SerialName("supabase_id") val supabaseId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

fun HeatMapItemRow.toHeatMapItem() = HeatMapItem(
    time = LocalDate.parse(time),
    count = dayCount,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    isDirty = false,
)

fun HeatMapItem.toHeatMapItemRow(userId: String, timestamp: Long = updatedAt) = HeatMapItemRow(
    userId = userId,
    time = time.toString(),
    dayCount = count,
    supabaseId = supabaseId,
    createdAt = createdAt,
    updatedAt = timestamp,
    isDeleted = isDeleted,
)

@Serializable
data class ActivityRow(
    @SerialName("user_id") val userId: String,
    @SerialName("cumulative_seconds") val cumulativeSeconds: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

fun ActivityTable.toActivityRow(userId: String, timestamp: Long = updatedAt) = ActivityRow(
    userId = userId,
    cumulativeSeconds = cumulativeSeconds,
    updatedAt = timestamp,
)
