package com.programmersbox.kmpuiviews.providers

import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.utils.printLogs
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val FAVORITES_TABLE = "favorites"
private const val FAVORITES_ID = 1
private const val FAVORITES_ITEM_ID = 2

abstract class FavoritesContentProvider : BaseContentProvider(), KoinComponent {
    private val itemDatabase by inject<ItemDatabase>()

    private val AUTHORITY by lazy { "$applicationId.provider.favorites" }

    private val sUriMatcher by lazy {
        UriMatcher(UriMatcher.NO_MATCH).apply {
            // URI for all favorites
            addURI(AUTHORITY, FAVORITES_TABLE, FAVORITES_ID)

            // URI for a specific favorite item
            addURI(AUTHORITY, "$FAVORITES_TABLE/#", FAVORITES_ITEM_ID)
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String?>?): Int {
        logWhoseCalling()
        val context = context ?: return 0
        val db = itemDatabase.openHelper.writableDatabase

        return when (sUriMatcher.match(uri)) {
            FAVORITES_ID -> {
                // Delete all matching rows
                val count = db.delete("FavoriteItem", selection, selectionArgs)
                context.contentResolver.notifyChange(uri, null)
                count
            }

            FAVORITES_ITEM_ID -> {
                // Delete a specific item by ID
                val id = uri.lastPathSegment
                val count = if (selection.isNullOrEmpty()) {
                    db.delete("FavoriteItem", "url = ?", arrayOf(id))
                } else {
                    // Create a new array with the ID appended
                    val newArgs = selectionArgs?.toMutableList() ?: mutableListOf()
                    newArgs.add(id)
                    db.delete("FavoriteItem", "$selection AND url = ?", newArgs.toTypedArray())
                }
                context.contentResolver.notifyChange(uri, null)
                count
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun getType(uri: Uri): String? {
        return when (sUriMatcher.match(uri)) {
            FAVORITES_ID -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$FAVORITES_TABLE"
            FAVORITES_ITEM_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.$FAVORITES_TABLE"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        logWhoseCalling()
        if (values == null) return null
        val context = context ?: return null

        printLogs { AUTHORITY }
        printLogs { uri }

        when (sUriMatcher.match(uri)) {
            FAVORITES_ID -> {
                val db = itemDatabase.openHelper.writableDatabase
                val url = values.getAsString("url") ?: return null

                // Insert the new favorite
                val rowId = runCatching { db.insert("FavoriteItem", 0, values) }
                    .getOrNull()
                    ?: return null

                if (rowId > 0) {
                    val itemUri = "content://$AUTHORITY/$FAVORITES_TABLE/$url".toUri()
                    context.contentResolver.notifyChange(uri, null)
                    return itemUri
                }
                return null
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?,
    ): Cursor? {
        logWhoseCalling()
        return itemDatabase.query(
            query = SupportSQLiteQueryBuilder
                .builder("FavoriteItem")
                .selection(selection, selectionArgs)
                .columns(projection?.filterNotNull()?.toTypedArray())
                .orderBy(sortOrder)
                .create(),
        )
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String?>?,
    ): Int {
        logWhoseCalling()
        if (values == null) return 0
        val context = context ?: return 0
        val db = itemDatabase.openHelper.writableDatabase

        return when (sUriMatcher.match(uri)) {
            FAVORITES_ID -> {
                // Update all matching rows
                val count = db.update("FavoriteItem", 0, values, selection, selectionArgs)
                if (count > 0) {
                    context.contentResolver.notifyChange(uri, null)
                }
                count
            }

            FAVORITES_ITEM_ID -> {
                // Update a specific item by ID
                val id = uri.lastPathSegment
                val count = if (selection.isNullOrEmpty()) {
                    db.update("FavoriteItem", 0, values, "url = ?", arrayOf(id))
                } else {
                    // Create a new array with the ID appended
                    val newArgs = selectionArgs?.toMutableList() ?: mutableListOf()
                    newArgs.add(id)
                    db.update("FavoriteItem", 0, values, "$selection AND url = ?", newArgs.toTypedArray())
                }
                if (count > 0) {
                    context.contentResolver.notifyChange(uri, null)
                }
                count
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}