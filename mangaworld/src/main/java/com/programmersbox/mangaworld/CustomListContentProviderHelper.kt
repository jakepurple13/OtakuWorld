package com.programmersbox.mangaworld

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem

/**
 * Helper class for accessing the CustomListContentProvider.
 * Provides convenient methods for querying, inserting, updating, and deleting
 * from CustomListItem (lists) and CustomListInfo (list entries).
 */
object CustomListContentProviderHelper {

    private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider.customlist"

    private const val LISTS_PATH = "lists"
    private const val LIST_ITEMS_PATH = "list_items"

    /** Base URI for CustomListItem (lists) */
    val LISTS_URI: Uri = "content://$AUTHORITY/$LISTS_PATH".toUri()

    /** Base URI for CustomListInfo (entries/items) */
    val LIST_ITEMS_URI: Uri = "content://$AUTHORITY/$LIST_ITEMS_PATH".toUri()

    /** URI for a specific CustomListItem by uuid */
    fun getListUri(uuid: String): Uri = "content://$AUTHORITY/$LISTS_PATH/$uuid".toUri()

    /** URI for a specific CustomListInfo by uniqueId */
    fun getListItemUri(uniqueId: String): Uri = "content://$AUTHORITY/$LIST_ITEMS_PATH/$uniqueId".toUri()

    // region Queries

    fun getAllLists(context: Context): Cursor? =
        context.contentResolver.query(LISTS_URI, null, null, null, null)

    fun getListByUuid(context: Context, uuid: String): Cursor? =
        context.contentResolver.query(getListUri(uuid), null, null, null, null)

    fun getAllListItems(context: Context): Cursor? =
        context.contentResolver.query(LIST_ITEMS_URI, null, null, null, null)

    /**
     * Get items belonging to a list by uuid using a selection filter.
     */
    fun getItemsForList(context: Context, uuid: String): Cursor? =
        context.contentResolver.query(
            LIST_ITEMS_URI,
            null,
            "uuid = ?",
            arrayOf(uuid),
            null
        )

    // endregion

    // region Inserts

    fun insertList(context: Context, item: CustomListItem): Uri? {
        val values = ContentValues().apply {
            put("uuid", item.uuid)
            put("name", item.name)
            put("time", item.time)
            put("useBiometric", if (item.useBiometric) 1 else 0)
        }
        return context.contentResolver.insert(LISTS_URI, values)
    }

    fun insertListItem(context: Context, info: CustomListInfo): Uri? {
        val values = ContentValues().apply {
            put("uniqueId", info.uniqueId)
            put("uuid", info.uuid)
            put("title", info.title)
            put("description", info.description)
            put("url", info.url)
            put("imageUrl", info.imageUrl)
            put("sources", info.source)
        }
        return context.contentResolver.insert(LIST_ITEMS_URI, values)
    }

    // endregion

    // region Updates

    fun updateList(context: Context, item: CustomListItem): Int {
        val values = ContentValues().apply {
            put("name", item.name)
            put("time", item.time)
            put("useBiometric", if (item.useBiometric) 1 else 0)
        }
        return context.contentResolver.update(
            getListUri(item.uuid),
            values,
            null,
            null
        )
    }

    fun updateListItem(context: Context, info: CustomListInfo): Int {
        val values = ContentValues().apply {
            put("uuid", info.uuid)
            put("title", info.title)
            put("description", info.description)
            put("url", info.url)
            put("imageUrl", info.imageUrl)
            put("sources", info.source)
        }
        return context.contentResolver.update(
            getListItemUri(info.uniqueId),
            values,
            null,
            null
        )
    }

    // endregion

    // region Deletes

    fun deleteList(context: Context, uuid: String): Int =
        context.contentResolver.delete(getListUri(uuid), null, null)

    fun deleteListItem(context: Context, uniqueId: String): Int =
        context.contentResolver.delete(getListItemUri(uniqueId), null, null)

    // endregion

    // region Cursor helpers

    fun cursorToCustomListItems(cursor: Cursor): List<CustomListItem> {
        val items = mutableListOf<CustomListItem>()
        if (cursor.moveToFirst()) {
            do {
                val uuid = cursor.getString(cursor.getColumnIndexOrThrow("uuid"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val time = cursor.getLong(cursor.getColumnIndexOrThrow("time"))
                val useBiometric = cursor.getInt(cursor.getColumnIndexOrThrow("useBiometric")) == 1
                items.add(
                    CustomListItem(
                        uuid = uuid,
                        name = name,
                        time = time,
                        useBiometric = useBiometric
                    )
                )
            } while (cursor.moveToNext())
        }
        return items
    }

    fun cursorToCustomListInfos(cursor: Cursor): List<CustomListInfo> {
        val items = mutableListOf<CustomListInfo>()
        if (cursor.moveToFirst()) {
            do {
                val uniqueId = cursor.getString(cursor.getColumnIndexOrThrow("uniqueId"))
                val uuid = cursor.getString(cursor.getColumnIndexOrThrow("uuid"))
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                val url = cursor.getString(cursor.getColumnIndexOrThrow("url"))
                val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("imageUrl"))
                val source = cursor.getString(cursor.getColumnIndexOrThrow("sources"))
                items.add(
                    CustomListInfo(
                        uniqueId = uniqueId,
                        uuid = uuid,
                        title = title,
                        description = description,
                        url = url,
                        imageUrl = imageUrl,
                        source = source
                    )
                )
            } while (cursor.moveToNext())
        }
        return items
    }

    // endregion
}
