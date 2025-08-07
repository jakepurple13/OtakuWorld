package com.programmersbox.mangaworld

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import com.programmersbox.favoritesdatabase.DbModel

/**
 * Helper class for accessing the FavoritesContentProvider.
 * Provides convenient methods for querying, inserting, updating, and deleting favorites.
 */
object FavoritesContentProviderHelper {

    private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider.favorites"
    private const val FAVORITES_TABLE = "favorites"

    /**
     * The base URI for the favorites content provider
     */
    val CONTENT_URI: Uri = "content://$AUTHORITY/$FAVORITES_TABLE".toUri()

    /**
     * Creates a URI for a specific favorite item
     * @param url The URL of the favorite item
     * @return The URI for the specific favorite item
     */
    fun getItemUri(url: String): Uri = "content://$AUTHORITY/$FAVORITES_TABLE/$url".toUri()

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

    /**
     * Updates an existing favorite in the content provider
     * @param context The context to use for accessing the content resolver
     * @param favorite The favorite with updated values
     * @return The number of rows updated
     */
    fun updateFavorite(context: Context, favorite: DbModel): Int {
        val values = ContentValues().apply {
            put("title", favorite.title)
            put("description", favorite.description)
            put("imageUrl", favorite.imageUrl)
            put("sources", favorite.source)
            put("numChapters", favorite.numChapters)
            put("shouldCheckForUpdate", if (favorite.shouldCheckForUpdate) 1 else 0)
        }

        return context.contentResolver.update(
            getItemUri(favorite.url),
            values,
            null,
            null
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
            getItemUri(url),
            null,
            null
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
        val cursor = getAllFavorites(context) ?: return emptyList()
        val favorites = cursorToFavorites(cursor)
        cursor.close()
        return favorites
    }

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
}