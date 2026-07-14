package com.programmersbox.anime.shared.videos

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private val externalContentUri: Uri
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

actual class VideoLibrarySource(private val context: Context) {

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("videos", Context.MODE_PRIVATE)

    private fun queryVideos(): List<SharedVideoContent> {
        val projections = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media._ID,
        )
        val results = mutableListOf<SharedVideoContent>()
        context.contentResolver.query(
            externalContentUri,
            projections,
            null,
            null,
            "LOWER (${MediaStore.Video.Media.DATE_TAKEN}) DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    val contentUri = ContentUris.withAppendedId(externalContentUri, id)
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    results.add(
                        SharedVideoContent(
                            videoName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)).orEmpty(),
                            path = contentUri.toString(),
                            duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
                            lastPlayedPositionMs = prefs.getLong(path.orEmpty(), 0L),
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return results
    }

    actual fun observeVideos(): Flow<List<SharedVideoContent>> = callbackFlow {
        trySend(queryVideos())
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryVideos())
            }
        }
        context.contentResolver.registerContentObserver(externalContentUri, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

    actual fun getResumePosition(path: String): Long = prefs.getLong(path, 0L)

    actual fun setResumePosition(path: String, positionMs: Long) {
        prefs.edit().putLong(path, positionMs).apply()
    }

    actual fun delete(content: SharedVideoContent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                content.path.toUri()?.let {
                    context.contentResolver.delete(it, null, null)
                }
            } else {
                java.io.File(content.path).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun String.toUri(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
