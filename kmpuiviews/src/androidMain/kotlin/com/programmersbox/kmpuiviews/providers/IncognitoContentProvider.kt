package com.programmersbox.kmpuiviews.providers

import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.net.toUri
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.utils.printLogs
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val INCOGNITO_TABLE = "incognito"
private const val INCOGNITO_ID = 1
private const val INCOGNITO_DB_TABLE = "IncognitoSourceTable"

abstract class IncognitoContentProvider : BaseContentProvider(), KoinComponent {
    private val itemDatabase by inject<ItemDatabase>()

    private val AUTHORITY by lazy { "$applicationId.provider.incognito" }

    private val sUriMatcher by lazy {
        UriMatcher(UriMatcher.NO_MATCH).apply {
            // URI for all favorites
            addURI(AUTHORITY, INCOGNITO_TABLE, INCOGNITO_ID)
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
        return when (sUriMatcher.match(uri)) {
            INCOGNITO_ID -> itemDatabase.query(
                query = SupportSQLiteQueryBuilder
                    .builder(INCOGNITO_DB_TABLE)
                    .selection(selection, selectionArgs)
                    .columns(projection?.filterNotNull()?.toTypedArray())
                    .orderBy(sortOrder)
                    .create(),
            )

            else -> null
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String?>?): Int {
        logWhoseCalling()
        val context = context ?: return 0
        val db = itemDatabase.openHelper.writableDatabase

        return when (sUriMatcher.match(uri)) {
            INCOGNITO_ID -> {
                // Delete all matching rows
                val count = db.delete(INCOGNITO_DB_TABLE, selection, selectionArgs)
                context.contentResolver.notifyChange(uri, null)
                count
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun getType(uri: Uri): String? {
        return when (sUriMatcher.match(uri)) {
            INCOGNITO_ID -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$INCOGNITO_TABLE"
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
            INCOGNITO_ID -> {
                val db = itemDatabase.openHelper.writableDatabase

                // Insert the new favorite
                val rowId = runCatching { db.insert(INCOGNITO_DB_TABLE, 0, values) }
                    .getOrNull()
                    ?: return null

                if (rowId > 0) {
                    val itemUri = "content://$AUTHORITY/$INCOGNITO_TABLE".toUri()
                    context.contentResolver.notifyChange(uri, null)
                    return itemUri
                }
                return null
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
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
            INCOGNITO_ID -> {
                // Update all matching rows
                val count = db.update(
                    table = INCOGNITO_DB_TABLE,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = values,
                    whereClause = selection,
                    whereArgs = selectionArgs
                )
                if (count > 0) {
                    context.contentResolver.notifyChange(uri, null)
                }
                count
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}