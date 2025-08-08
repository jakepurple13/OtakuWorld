package com.programmersbox.otakuworld

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.DbModel

@Composable
fun ContentProviderTest() {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            println(it)
            if (it) {
                val response = FavoritesContentProviderHelper.insertFavorite(
                    context,
                    DbModel(
                        title = "Test2",
                        description = "Test",
                        url = "Test2",
                        imageUrl = "Test",
                        source = "Test",
                        numChapters = 0,
                        shouldCheckForUpdate = true
                    )
                )
                println(response)
                println(FavoritesContentProviderHelper.getAllFavoritesAsList(context))
            }
        }
        Button(
            onClick = { launcher.launch("com.programmersbox.mangaworld.noFirebase.READ_WRITE_FAVORITES") },
        ) { Text("Allow Access to favorites") }

        val launcher2 = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            println(it)
            if (it) {
                val response = CustomListContentProviderHelper.getAllLists(context)
                    ?.let { CustomListContentProviderHelper.cursorToCustomListItems(it) }
                    .orEmpty()

                println(response)
            }
        }

        Button(
            onClick = { launcher2.launch("com.programmersbox.mangaworld.noFirebase.READ_WRITE_LISTS") },
        ) { Text("Allow Access to lists") }
    }
}

/**
 * Helper class for accessing the FavoritesContentProvider.
 * Provides convenient methods for querying, inserting, updating, and deleting favorites.
 */
object FavoritesContentProviderHelper {

    private const val AUTHORITY = "com.programmersbox.mangaworld.noFirebase.provider.favorites"
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


/**
 * Helper class for accessing the CustomListContentProvider.
 * Provides convenient methods for querying, inserting, updating, and deleting
 * from CustomListItem (lists) and CustomListInfo (list entries).
 */
object CustomListContentProviderHelper {

    private const val AUTHORITY = "com.programmersbox.mangaworld.noFirebase.provider.customlist"

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

/*
@Serializable
data class CustomList(
    @Embedded
    val item: CustomListItem,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "uuid"
    )
    val list: List<CustomListInfo>,
)

@Serializable
@Entity(tableName = "CustomListItem")
data class CustomListItem(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "time")
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(defaultValue = "0")
    val useBiometric: Boolean = false,
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
@Entity(tableName = "CustomListInfo")
data class CustomListInfo(
    @PrimaryKey
    @ColumnInfo(defaultValue = "0c65586e-f3dc-4878-be63-b134fb46466c")
    val uniqueId: String = Uuid.random().toString(),
    @ColumnInfo("uuid")
    val uuid: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "imageUrl")
    val imageUrl: String,
    @ColumnInfo(name = "sources")
    val source: String,
)


@Serializable
data class DbModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: String,
    var numChapters: Int = 0,
    val shouldCheckForUpdate: Boolean = true,
)*/
