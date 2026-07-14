package com.programmersbox.anime.shared.videos

import com.programmersbox.anime.shared.AnimeDesktopSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.prefs.Preferences

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm")

actual class VideoLibrarySource(private val animeDesktopSettings: AnimeDesktopSettings) {

    private val resumePositions = Preferences.userNodeForPackage(VideoLibrarySource::class.java)

    private fun scanVideos(downloadsPath: String): List<SharedVideoContent> {
        val root = File(downloadsPath)
        if (!root.exists()) return emptyList()
        return root
            .walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in VIDEO_EXTENSIONS }
            .map {
                SharedVideoContent(
                    videoName = it.nameWithoutExtension,
                    path = it.absolutePath,
                    duration = 0L,
                    lastPlayedPositionMs = resumePositions.getLong(it.absolutePath, 0L),
                )
            }
            .toList()
    }

    actual fun observeVideos(): Flow<List<SharedVideoContent>> =
        animeDesktopSettings.downloadsDirectory.asFlow().map { scanVideos(it) }

    actual fun getResumePosition(path: String): Long = resumePositions.getLong(path, 0L)

    actual fun setResumePosition(path: String, positionMs: Long) {
        resumePositions.putLong(path, positionMs)
    }

    actual fun delete(content: SharedVideoContent) {
        File(content.path).delete()
    }
}
