package com.programmersbox.otakuworld.providers

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import com.programmersbox.otakuworld.DbModel
import io.ktor.util.encodeBase64

class OtakuFavoritesContentProviderHelper(
    private val authority: String,
) {

    private val FAVORITES_TABLE = "favorites"

    /**
     * The base URI for the favorites content provider
     */
    val CONTENT_URI: Uri = "content://$authority/$FAVORITES_TABLE".toUri()

    /**
     * Creates a URI for a specific favorite item
     * @param url The URL of the favorite item
     * @return The URI for the specific favorite item
     */
    fun getItemUri(url: String): Uri = "content://$authority/$FAVORITES_TABLE/${url.encodeBase64()}".toUri()

    /**
     * Retrieves all favorites from the content provider
     * @param context The context to use for accessing the content resolver
     * @return A cursor containing all favorites, or null if an error occurred
     */
    fun getAllFavorites(context: Context): Cursor? {
        return context.contentResolver.query(
            CONTENT_URI,
            null,
            null,
            null,
            null
        )
    }

    fun getAllFavorites(contentResolver: ContentResolver): Cursor? {
        return contentResolver.query(
            CONTENT_URI,
            null,
            null,
            null,
            null
        )
    }

    fun getAllFavoritesFlow(context: Context) = context
        .contentResolver
        .observeUri(CONTENT_URI) { getAllFavorites(context)?.let { cursorToFavorites(it) } }

    /**
     * Retrieves a specific favorite by URL
     * @param context The context to use for accessing the content resolver
     * @param url The URL of the favorite to retrieve
     * @return A cursor containing the favorite, or null if not found or an error occurred
     */
    fun getFavoriteByUrl(context: Context, url: String): Cursor? {
        return context.contentResolver.query(
            getItemUri(url),
            null,
            null,
            null,
            null
        )
    }

    fun getFavoriteByUrlFlow(context: Context, url: String) = context
        .contentResolver
        .observeUri(getItemUri(url)) { getFavoriteByUrl(context, url)?.let { cursorToFavorites(it) } }

    /**
     * Checks if a favorite with the given URL exists
     * @param context The context to use for accessing the content resolver
     * @param url The URL to check
     * @return true if the favorite exists, false otherwise
     */
    fun favoriteExists(context: Context, url: String): Boolean {
        val cursor = getFavoriteByUrl(context, url)
        val exists = (cursor?.count ?: 0) > 0
        cursor?.close()
        return exists
    }

    fun favoriteExistsFlow(context: Context, url: String) = context
        .contentResolver
        .observeUri(getItemUri(url)) { favoriteExists(context, url) }

    /**
     * Inserts a new favorite into the content provider
     * @param context The context to use for accessing the content resolver
     * @param favorite The favorite to insert
     * @return The URI of the newly inserted favorite, or null if insertion failed
     */
    fun insertFavorite(context: Context, favorite: DbModel): Uri? {
        val values = ContentValues().apply {
            put("title", favorite.title)
            put("description", favorite.description)
            put("url", favorite.url)
            put("imageUrl", favorite.imageUrl)
            put("sources", favorite.source)
            put("numChapters", favorite.numChapters)
            put("shouldCheckForUpdate", if (favorite.shouldCheckForUpdate) 1 else 0)
        }

        return context.contentResolver.insert(CONTENT_URI, values)
    }

    fun insertFavorites(context: Context, favorites: List<DbModel>): Int {
        return context
            .contentResolver
            .bulkInsert(
                CONTENT_URI,
                favorites
                    .map { favorite ->
                        ContentValues().apply {
                            put("title", favorite.title)
                            put("description", favorite.description)
                            put("url", favorite.url)
                            put("imageUrl", favorite.imageUrl)
                            put("sources", favorite.source)
                            put("numChapters", favorite.numChapters)
                            put("shouldCheckForUpdate", if (favorite.shouldCheckForUpdate) 1 else 0)
                        }
                    }
                    .toTypedArray()
            )
    }

    /**
     * Updates an existing favorite in the content provider
     * @param context The context to use for accessing the content resolver
     * @param favorite The favorite with updated values
     * @return The number of rows updated
     */
    fun updateFavorite(context: Context, favorite: DbModel): Int {
        val values = ContentValues().apply {
            put("url", favorite.url)
            put("title", favorite.title)
            put("description", favorite.description)
            put("imageUrl", favorite.imageUrl)
            put("sources", favorite.source)
            put("numChapters", favorite.numChapters)
            put("shouldCheckForUpdate", if (favorite.shouldCheckForUpdate) 1 else 0)
        }

        return context.contentResolver.update(
            CONTENT_URI,//getItemUri(favorite.url),
            values,
            "url=?",
            arrayOf(favorite.url)
        )
    }

    /**
     * Deletes a favorite from the content provider
     * @param context The context to use for accessing the content resolver
     * @param url The URL of the favorite to delete
     * @return The number of rows deleted
     */
    fun deleteFavorite(context: Context, url: String): Int {
        return context.contentResolver.delete(
            CONTENT_URI,//getItemUri(url),
            "url=?",
            arrayOf(url)
        )
    }

    /**
     * Converts a cursor to a list of DbModel objects
     * @param cursor The cursor to convert
     * @return A list of DbModel objects
     */
    fun cursorToFavorites(cursor: Cursor): List<DbModel> {
        val favorites = mutableListOf<DbModel>()

        if (cursor.moveToFirst()) {
            do {
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                val url = cursor.getString(cursor.getColumnIndexOrThrow("url"))
                val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("imageUrl"))
                val source = cursor.getString(cursor.getColumnIndexOrThrow("sources"))
                val numChapters = cursor.getInt(cursor.getColumnIndexOrThrow("numChapters"))
                val shouldCheckForUpdate = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCheckForUpdate")) == 1

                favorites.add(
                    DbModel(
                        title = title,
                        description = description,
                        url = url,
                        imageUrl = imageUrl,
                        source = source,
                        numChapters = numChapters,
                        shouldCheckForUpdate = shouldCheckForUpdate
                    )
                )
            } while (cursor.moveToNext())
        }

        return favorites
    }

    /**
     * Retrieves all favorites as a list of DbModel objects
     * @param context The context to use for accessing the content resolver
     * @return A list of DbModel objects representing all favorites
     */
    fun getAllFavoritesAsList(context: Context): List<DbModel> {
        return getAllFavorites(context)
            ?.use { cursorToFavorites(it) }
            ?: return emptyList()
    }

    fun getAllFavoritesAsList(contentResolver: ContentResolver): List<DbModel> {
        return getAllFavorites(contentResolver)
            ?.use { cursorToFavorites(it) }
            ?: return emptyList()
    }

    fun getAllFavoritesAsListFlow(context: Context) = context
        .contentResolver
        .observeUri(CONTENT_URI) { getAllFavoritesAsList(it) }

    /**
     * Retrieves a specific favorite as a DbModel object
     * @param context The context to use for accessing the content resolver
     * @param url The URL of the favorite to retrieve
     * @return The DbModel object, or null if not found
     */
    fun getFavoriteByUrlAsModel(context: Context, url: String): DbModel? {
        val cursor = getFavoriteByUrl(context, url) ?: return null
        val favorites = cursorToFavorites(cursor)
        cursor.close()
        return favorites.firstOrNull()
    }

    fun getFavoriteByUrlAsModelFlow(context: Context, url: String) = context
        .contentResolver
        .observeUri(getItemUri(url)) { getFavoriteByUrlAsModel(context, url) }
}