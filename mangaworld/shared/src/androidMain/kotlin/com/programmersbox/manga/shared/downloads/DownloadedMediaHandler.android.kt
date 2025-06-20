package com.programmersbox.manga.shared.downloads

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class DownloadedMediaHandler(
    private val context: Context,
) {

    actual fun init(folderLocation: String) {
        //loadChapters(folderLocation)
        loadChapters(
            Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/MangaWorld/"
        )
    }

    actual fun listenToUpdates(): Flow<List<DownloadedChapters>> = chapters.asStateFlow()

    actual fun delete(downloadedChapters: DownloadedChapters) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.contentResolver.delete(
                    downloadedChapters.assetFileStringUri.toUri(),
                    "${MediaStore.Images.Media._ID} = ?",
                    arrayOf(downloadedChapters.id)
                )
            } else {
                File(downloadedChapters.chapterFolder).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun clear() {
        unregister()
    }

    private fun ContentResolver.registerObserver(
        uri: Uri,
        observer: (selfChange: Boolean) -> Unit,
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
        contentObserver?.let { context.contentResolver.unregisterContentObserver(it) }
    }

    val chapters = MutableStateFlow<List<DownloadedChapters>>(emptyList())

    fun loadChapters(folderLocation: String) {
        val imageList = getMangaFolders(context, folderLocation)
        chapters.tryEmit(imageList)

        if (contentObserver == null) {
            contentObserver = context.contentResolver.registerObserver(externalContentUri) {
                val imageList = getMangaFolders(context, folderLocation)
                chapters.tryEmit(imageList)
            }
        }
    }

    private val externalContentUri: Uri = MediaStore.Files.getContentUri("external")


    @SuppressLint("InlinedApi")
    private val projections = arrayOf(
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DATA
    )

    fun getMangaFolders(context: Context, folderLocation: String): List<DownloadedChapters> {
        val contentLocation = externalContentUri
        val allVideo = mutableListOf<DownloadedChapters>()
        val cursor = context.contentResolver.query(
            contentLocation,
            projections,
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
                    DownloadedChapters(
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