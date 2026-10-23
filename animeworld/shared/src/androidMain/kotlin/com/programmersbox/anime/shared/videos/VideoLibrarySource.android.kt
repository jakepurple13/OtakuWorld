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
import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import com.programmersbox.anime.shared.downloads.isInFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File

private val externalContentUri: Uri
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

actual class VideoLibrarySource(
    private val context: Context,
    private val downloadFolder: () -> String,
) {

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("videos", Context.MODE_PRIVATE)

    private fun queryVideos(): List<SharedVideoContent> {
        val projections = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
        )
        val folder = downloadFolder()
        val results = mutableListOf<SharedVideoContent>()
        context.contentResolver.query(
            externalContentUri,
            projections,
            null,
            null,
            "COALESCE(${MediaStore.Video.Media.DATE_TAKEN}, ${MediaStore.Video.Media.DATE_ADDED} * 1000) DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    val contentUri = ContentUris.withAppendedId(externalContentUri, id)
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    if (path?.endsWith(".part") == true) continue
                    val isDownload = isInFolder(path, folder)
                    results.add(
                        SharedVideoContent(
                            videoName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)).orEmpty(),
                            path = contentUri.toString(),
                            duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
                            // The player saves by the content URI it was opened with; older saves used the file path.
                            lastPlayedPositionMs = prefs.getLong(contentUri.toString(), prefs.getLong(path.orEmpty(), 0L)),
                            sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)),
                            dateAddedMs = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)) * 1000L,
                            isDownload = isDownload,
                            subtitleLabels = if (isDownload && path != null) {
                                OfflineSubtitles.find(File(path)).map { it.lang }
                            } else {
                                emptyList()
                            },
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
        trySend(Unit)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                // Signal only; the query runs on the flow's IO dispatcher below.
                trySend(Unit)
            }
        }
        // Resume positions live in prefs, not MediaStore, so re-query when the player saves one.
        val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        val videoPrefs = prefs
        context.contentResolver.registerContentObserver(externalContentUri, true, observer)
        videoPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
            videoPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        }
    }
        .conflate()
        .map { queryVideos() }
        .flowOn(Dispatchers.IO)

    actual fun getResumePosition(path: String): Long = prefs.getLong(path, 0L)

    actual fun setResumePosition(path: String, positionMs: Long) {
        prefs.edit().putLong(path, positionMs).apply()
    }

    actual fun delete(content: SharedVideoContent) {
        try {
            // Resolve before the MediaStore row (and its DATA path) is gone; clean sidecars only after it succeeds.
            val videoFile = OfflineSubtitles.resolveVideoFile(context, content.path)
            val uri = content.path.toUri()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                // content.path is a content:// URI, so delete the resolved file itself, then drop the MediaStore row.
                videoFile?.delete()
            }
            uri?.let { context.contentResolver.delete(it, null, null) }
            // Only drop sidecars once the video is really gone (a 0-row delete must keep its subtitles).
            if (videoFile?.exists() == false) OfflineSubtitles.deleteFor(videoFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun String.toUri(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
