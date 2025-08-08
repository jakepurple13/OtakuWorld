package com.programmersbox.kmpuiviews.providers

import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.programmersbox.favoritesdatabase.ListDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val LISTS_PATH = "lists"
private const val LIST_ITEMS_PATH = "list_items"

private const val LISTS_ID = 1
private const val LIST_ID = 2
private const val LIST_ITEMS_ID = 3
private const val LIST_ITEM_ID = 4


/**
 * A ContentProvider exposing the Custom List database (ListDatabase).
 *
 * Authority format: "<applicationId>.provider.customlist"
 * Paths:
 * - content://<authority>/lists -> table CustomListItem
 * - content://<authority>/lists/\* -> a specific CustomListItem by uuid
 * - content://<authority>/list_items -> table CustomListInfo
 * - content://<authority>/list_items/\* -> a specific CustomListInfo by uniqueId
 */
abstract class CustomListContentProvider : BaseContentProvider(), KoinComponent {
    private val listDatabase by inject<ListDatabase>()

    private val AUTHORITY by lazy { "$applicationId.provider.customlist" }

    private val uriMatcher by lazy {
        UriMatcher(UriMatcher.NO_MATCH).apply {
            // Use * to allow non-numeric IDs (UUID, etc.)
            addURI(AUTHORITY, LISTS_PATH, LISTS_ID)
            addURI(AUTHORITY, "$LISTS_PATH/*", LIST_ID)

            addURI(AUTHORITY, LIST_ITEMS_PATH, LIST_ITEMS_ID)
            addURI(AUTHORITY, "$LIST_ITEMS_PATH/*", LIST_ITEM_ID)
        }
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = when (uriMatcher.match(uri)) {
        LISTS_ID -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$LISTS_PATH"
        LIST_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.$LISTS_PATH"
        LIST_ITEMS_ID -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$LIST_ITEMS_PATH"
        LIST_ITEM_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.$LIST_ITEMS_PATH"
        else -> null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?,
    ): Cursor? {
        logWhoseCalling()
        val (table, sel, args) = when (uriMatcher.match(uri)) {
            LISTS_ID -> Triple("CustomListItem", selection, selectionArgs)
            LIST_ID -> {
                val id = uri.lastPathSegment
                Triple(
                    "CustomListItem",
                    selection?.let { "$it AND uuid = ?" } ?: "uuid = ?",
                    (selectionArgs?.toMutableList() ?: mutableListOf()).apply { add(id) }.toTypedArray()
                )
            }

            LIST_ITEMS_ID -> Triple("CustomListInfo", selection, selectionArgs)
            LIST_ITEM_ID -> {
                val id = uri.lastPathSegment
                Triple(
                    "CustomListInfo",
                    selection?.let { "$it AND uniqueId = ?" } ?: "uniqueId = ?",
                    (selectionArgs?.toMutableList() ?: mutableListOf()).apply { add(id) }.toTypedArray()
                )
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        return listDatabase.query(
            SupportSQLiteQueryBuilder
                .builder(table)
                .selection(sel, args)
                .columns(projection?.filterNotNull()?.toTypedArray())
                .orderBy(sortOrder)
                .create()
        )
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        logWhoseCalling()
        if (values == null) return null
        val ctx = context ?: return null
        val db = listDatabase.openHelper.writableDatabase

        return when (uriMatcher.match(uri)) {
            LISTS_ID -> {
                val uuid = values.getAsString("uuid")
                val rowId = db.insert("CustomListItem", 0, values)
                if (rowId > 0) {
                    val itemUri = "content://$AUTHORITY/$LISTS_PATH/${uuid ?: rowId}".toUri()
                    ctx.contentResolver.notifyChange(uri, null)
                    itemUri
                } else null
            }

            LIST_ITEMS_ID -> {
                val uniqueId = values.getAsString("uniqueId")
                val rowId = db.insert("CustomListInfo", 0, values)
                if (rowId > 0) {
                    val itemUri = "content://$AUTHORITY/$LIST_ITEMS_PATH/${uniqueId ?: rowId}".toUri()
                    ctx.contentResolver.notifyChange(uri, null)
                    itemUri
                } else null
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String?>?): Int {
        logWhoseCalling()
        val ctx = context ?: return 0
        val db = listDatabase.openHelper.writableDatabase

        val (table, sel, args) = when (uriMatcher.match(uri)) {
            LISTS_ID -> Triple("CustomListItem", selection, selectionArgs)
            LIST_ID -> {
                val id = uri.lastPathSegment
                Triple(
                    "CustomListItem",
                    selection?.let { "$it AND uuid = ?" } ?: "uuid = ?",
                    (selectionArgs?.toMutableList() ?: mutableListOf()).apply { add(id) }.toTypedArray()
                )
            }

            LIST_ITEMS_ID -> Triple("CustomListInfo", selection, selectionArgs)
            LIST_ITEM_ID -> {
                val id = uri.lastPathSegment
                Triple(
                    "CustomListInfo",
                    selection?.let { "$it AND uniqueId = ?" } ?: "uniqueId = ?",
                    (selectionArgs?.toMutableList() ?: mutableListOf()).apply { add(id) }.toTypedArray()
                )
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val count = db.delete(table, sel, args)
        if (count > 0) ctx.contentResolver.notifyChange(uri, null)
        return count
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String?>?,
    ): Int {
        logWhoseCalling()
        if (values == null) return 0
        val ctx = context ?: return 0
        val db = listDatabase.openHelper.writableDatabase

        val (table, sel, args) = when (uriMatcher.match(uri)) {
            LISTS_ID -> Triple("CustomListItem", selection, selectionArgs)
            LIST_ID -> {
                val id = uri.lastPathSegment
                Triple(
                    "CustomListItem",
                    selection?.let { "$it AND uuid = ?" } ?: "uuid = ?",
                    (selectionArgs?.toMutableList() ?: mutableListOf()).apply { add(id) }.toTypedArray()
                )
            }

            LIST_ITEMS_ID -> Triple("CustomListInfo", selection, selectionArgs)
            LIST_ITEM_ID -> {
                val id = uri.lastPathSegment
                Triple(
                    "CustomListInfo",
                    selection?.let { "$it AND uniqueId = ?" } ?: "uniqueId = ?",
                    (selectionArgs?.toMutableList() ?: mutableListOf()).apply { add(id) }.toTypedArray()
                )
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val count = db.update(table, 0, values, sel, args)
        if (count > 0) ctx.contentResolver.notifyChange(uri, null)
        return count
    }
}
