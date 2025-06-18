package com.programmersbox.mangaworld

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.View
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

val DOWNLOAD_FILE_PATH
    get() = Environment
        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/MangaWorld/"

/*val SHOW_ADULT = booleanPreferencesKey("showAdultSources")
val Context.showAdultFlow get() = otakuDataStore.data.map { it[SHOW_ADULT] ?: false }

val FOLDER_LOCATION = stringPreferencesKey("folderLocation")
val Context.folderLocationFlow get() = otakuDataStore.data.map { it[FOLDER_LOCATION] ?: DOWNLOAD_FILE_PATH }

var Context.folderLocation: String by sharedPrefNotNullDelegate(
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/MangaWorld/"
)*/

class CustomHideBottomViewOnScrollBehavior<T : View>(context: Context?, attrs: AttributeSet?) :
    HideBottomViewOnScrollBehavior<T>(context, attrs) {

    var isShowing = true
        private set

    override fun slideDown(child: T) {
        super.slideDown(child)
        isShowing = false
    }

    override fun slideUp(child: T) {
        super.slideUp(child)
        isShowing = true
    }

}

class ChaptersGet private constructor(private val chaptersContex: Context) {

    data class Chapters(
        val name: String,
        val id: String,
        val data: String,
        val assetFileStringUri: String,
        val folder: String = "",
        val folderName: String = "",
        val chapterFolder: String = "",
        val chapterName: String = ""
    )

    /**Returns an Arraylist of [Chapters]   */
    @SuppressLint("InlinedApi")
    fun getAllMangaContent(contentLocation: Uri): List<Chapters> {
        val allVideo = mutableListOf<Chapters>()
        val cursor = chaptersContex.contentResolver.query(
            contentLocation,
            Projections,
            null,
            null,
            "LOWER (" + MediaStore.Files.FileColumns.DATE_TAKEN + ") DESC"
        ) //DESC ASC
        try {
            while (cursor?.moveToNext() == true) {
                val id: Int = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                val contentUri: Uri = Uri.withAppendedPath(contentLocation, id.toString())
                allVideo.add(
                    Chapters(
                        id = id.toString(),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)),
                        data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)),
                        assetFileStringUri = contentUri.toString()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return allVideo
    }

    private fun ContentResolver.registerObserver(
        uri: Uri,
        observer: (selfChange: Boolean) -> Unit
    ): ContentObserver {
        val contentObserver = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                observer(selfChange)
            }
        }
        registerContentObserver(uri, true, contentObserver)
        return contentObserver
    }

    private var contentObserver: ContentObserver? = null

    fun unregister() {
        contentObserver?.let { chaptersContex.contentResolver.unregisterContentObserver(it) }
    }

    val chapters = MutableStateFlow<List<Chapters>>(emptyList())

    fun loadChapters(scope: CoroutineScope, folderLocation: String) {
        scope.launch {
            val imageList = getMangaFolders(chaptersContex, folderLocation)
            chapters.tryEmit(imageList)

            if (contentObserver == null) {
                contentObserver = chaptersContex.contentResolver.registerObserver(externalContentUri) {
                    loadChapters(scope, folderLocation)
                }
            }
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var chaptersGet: ChaptersGet? = null
        val externalContentUri: Uri = MediaStore.Files.getContentUri("external")

        //val internalContentUri: Uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI
        private var cursor: Cursor? = null
        fun getInstance(contx: Context): ChaptersGet? {
            if (chaptersGet == null) chaptersGet = ChaptersGet(contx)
            return chaptersGet
        }

        @SuppressLint("InlinedApi")
        private val Projections = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA
        )

        fun getMangaFolders(context: Context, folderLocation: String): List<Chapters> {
            val contentLocation = externalContentUri
            val allVideo = mutableListOf<Chapters>()
            val cursor = context.contentResolver.query(
                contentLocation,
                Projections,
                MediaStore.Files.FileColumns.DATA + " LIKE ?",
                arrayOf("$folderLocation%"),
                "LOWER (" + MediaStore.Files.FileColumns.DATE_ADDED + ") DESC"
            ) //DESC ASC
            try {
                while (cursor?.moveToNext() == true) {
                    val id: Int = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                    val contentUri: Uri = Uri.withAppendedPath(contentLocation, id.toString())
                    val folder = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
                        .split("/")
                    allVideo.add(
                        Chapters(
                            id = id.toString(),
                            name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)),
                            data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)),
                            assetFileStringUri = contentUri.toString(),
                            folder = folder.dropLast(2).joinToString("/"),
                            folderName = folder.dropLast(2).lastOrNull().orEmpty(),
                            chapterFolder = folder.dropLast(1).joinToString("/"),
                            chapterName = folder.dropLast(1).lastOrNull().orEmpty()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
            return allVideo
        }
    }
}

