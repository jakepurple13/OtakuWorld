package com.programmersbox.otakuworld

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

private const val favoritesUri = "provider.favorites"
private const val listsUri = "provider.customlist"

private const val mangaWorldPackageName = "com.programmersbox.mangaworld"
private const val animeWorldPackageName = "com.programmersbox.animeworld"
private const val novelWorldPackageName = "com.programmersbox.novelworld"

private const val noCloudFirebaseSuffix = ".noCloudFirebase"
private const val noFirebaseSuffix = ".noFirebase"
private const val fullSuffix = ""

private const val favoritePermissions = "READ_WRITE_FAVORITES"
private const val listPermissions = "READ_WRITE_LISTS"

enum class App {
    MangaWorld,
    AnimeWorld,
    NovelWorld
}

enum class Provider {
    NoCloudFirebase,
    NoFirebase,
    Full
}

class OtakuProvider {
    fun favoritesBuilder(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuFavoritesContentProviderHelper(
        OtakuBuilder()
            .apply(builder)
            .build() + ".$favoritesUri"
    )

    fun favoritesUri(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuBuilder()
        .apply(builder)
        .build() + ".$favoritesUri"

    fun favoritesPermissions(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuBuilder()
        .apply(builder)
        .build() + ".$favoritePermissions"

    fun listsBuilder(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuCustomListContentProviderHelper(
        OtakuBuilder()
            .apply(builder)
            .build() + ".$listsUri"
    )

    fun listsUri(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuBuilder()
        .apply(builder)
        .build() + ".$listsUri"

    fun listPermissions(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuBuilder()
        .apply(builder)
        .build() + ".$listPermissions"

    class OtakuBuilder {
        private var packageName by Delegates.notNull<String>()
        private var suffix by Delegates.notNull<String>()

        var appType: App
            get() = error("App type not set")
            set(value) {
                setPackage(value)
            }

        var provider: Provider
            get() = error("Provider not set")
            set(value) {
                setProvider(value)
            }

        fun setPackage(app: App) = apply {
            packageName = when (app) {
                App.MangaWorld -> mangaWorldPackageName
                App.AnimeWorld -> animeWorldPackageName
                App.NovelWorld -> novelWorldPackageName
            }
        }

        fun setProvider(provider: Provider) = apply {
            suffix = when (provider) {
                Provider.NoCloudFirebase -> noCloudFirebaseSuffix
                Provider.NoFirebase -> noFirebaseSuffix
                Provider.Full -> fullSuffix
            }
        }

        fun build() = "$packageName$suffix"
    }
}

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
    fun getItemUri(url: String): Uri = "content://$authority/$FAVORITES_TABLE/$url".toUri()

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

    fun getAllFavoritesAsListFlow(context: Context) = context
        .contentResolver
        .observeUri(CONTENT_URI) { getAllFavoritesAsList(context) }

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


/**
 * Helper class for accessing the CustomListContentProvider.
 * Provides convenient methods for querying, inserting, updating, and deleting
 * from CustomListItem (lists) and CustomListInfo (list entries).
 */
class OtakuCustomListContentProviderHelper(
    private val authority: String,
) {

    private val LISTS_PATH = "lists"
    private val LIST_ITEMS_PATH = "list_items"

    /** Base URI for CustomListItem (lists) */
    val LISTS_URI: Uri = "content://$authority/$LISTS_PATH".toUri()

    /** Base URI for CustomListInfo (entries/items) */
    val LIST_ITEMS_URI: Uri = "content://$authority/$LIST_ITEMS_PATH".toUri()

    /** URI for a specific CustomListItem by uuid */
    fun getListUri(uuid: String): Uri = "content://$authority/$LISTS_PATH/$uuid".toUri()

    /** URI for a specific CustomListInfo by uniqueId */
    fun getListItemUri(uniqueId: String): Uri = "content://$authority/$LIST_ITEMS_PATH/$uniqueId".toUri()

    // region Queries

    fun getAllLists(context: Context): Cursor? =
        context.contentResolver.query(LISTS_URI, null, null, null, null)

    fun getAllListsFlow(context: Context) = context
        .contentResolver
        .observeUri(LISTS_URI) { getAllLists(context)?.let { cursorToCustomListItems(it) } }

    fun getListByUuid(context: Context, uuid: String): Cursor? =
        context.contentResolver.query(getListUri(uuid), null, null, null, null)

    fun getListByUuidFlow(context: Context, uuid: String) = context
        .contentResolver
        .observeUri(getListUri(uuid)) { getListByUuid(context, uuid) }

    fun getAllListItems(context: Context): Cursor? =
        context.contentResolver.query(LIST_ITEMS_URI, null, null, null, null)

    fun getAllListItemsFlow(context: Context) = context
        .contentResolver
        .observeUri(LIST_ITEMS_URI) { getAllListItems(context) }

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

    fun getItemsForListFlow(context: Context, uuid: String) = context
        .contentResolver
        .observeUri(LIST_ITEMS_URI) { getItemsForList(context, uuid) }

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

internal fun <T> ContentResolver.observeUri(
    uri: Uri,
    getData: suspend () -> T?,
) = callbackFlow<T> {
    launch {
        getData()?.let { send(it) }
    }

    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            launch(Dispatchers.IO) {
                getData()?.let { send(it) }
            }
        }
    }

    registerContentObserver(uri, true, observer)

    awaitClose {
        unregisterContentObserver(observer)
    }
}