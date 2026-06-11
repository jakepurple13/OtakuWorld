package com.programmersbox.manga.shared.downloads

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

actual class DownloadedMediaHandler(
    private val context: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ContentObserver fires once per MediaStore row change; coalesce bursts into one query
    private val refresh = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val folder = Environment
        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        .toString() + "/MangaWorld/"

    @OptIn(FlowPreview::class)
    actual fun init(folderLocation: String) {
        scope.launch { chapters.update { getMangaFolders(context, folder) } }

        refresh
            .debounce(300)
            .onEach { chapters.update { getMangaFolders(context, folder) } }
            .launchIn(scope)

        if (contentObserver == null) {
            contentObserver = context.contentResolver.registerObserver(externalContentUri) {
                refresh.tryEmit(Unit)
            }
        }
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
        scope.cancel()
    }

    private fun ContentResolver.registerObserver(
        uri: Uri,
        observer: (selfChange: Boolean) -> Unit,
    ): ContentObserver {
        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
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
        contentObserver = null
    }

    val chapters = MutableStateFlow<List<DownloadedChapters>>(emptyList())

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
