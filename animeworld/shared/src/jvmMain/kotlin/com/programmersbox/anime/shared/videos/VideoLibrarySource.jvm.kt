package com.programmersbox.anime.shared.videos

import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.prefs.Preferences

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "ts")

/** Videos under [root], skipping the `<base>.mp4.part.tracks` folders a DASH download works in. */
internal fun libraryVideoFiles(root: File): Sequence<File> = root
    .walkTopDown()
    .onEnter { !it.name.endsWith(".tracks") }
    .filter { it.isFile && it.extension.lowercase() in VIDEO_EXTENSIONS }

/** Deletes [video] with its subtitle sidecars and index. */
internal fun deleteWithSidecars(video: File) {
    OfflineSubtitles.sidecarFiles(video).forEach { it.delete() }
    video.delete()
}

actual class VideoLibrarySource(private val animeDesktopSettings: AnimeDesktopSettings) {

    private val resumePositions = Preferences.userNodeForPackage(VideoLibrarySource::class.java)

    private fun scanVideos(downloadsPath: String): List<SharedVideoContent> {
        val root = File(downloadsPath)
        if (!root.exists()) return emptyList()
        return libraryVideoFiles(root)
            .map {
                SharedVideoContent(
                    videoName = it.nameWithoutExtension,
                    path = it.absolutePath,
                    duration = 0L,
                    lastPlayedPositionMs = resumePositions.getLong(it.absolutePath, 0L),
                    sizeBytes = it.length(),
                    dateAddedMs = it.lastModified(),
                    isDownload = true,
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
        deleteWithSidecars(File(content.path))
    }
}
