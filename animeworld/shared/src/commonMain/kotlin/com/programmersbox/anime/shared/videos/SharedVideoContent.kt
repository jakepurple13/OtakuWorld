package com.programmersbox.anime.shared.videos

import kotlinx.coroutines.flow.Flow

data class SharedVideoContent(
    val videoName: String,
    val path: String,
    val duration: Long,
    val lastPlayedPositionMs: Long,
    val sizeBytes: Long = 0L,
    val dateAddedMs: Long = 0L,
    val isDownload: Boolean = false,
    val subtitleLabels: List<String> = emptyList(),
    /** Display name of the library folder an "other" video was found in (desktop only, e.g. `~/Movies`). */
    val folder: String? = null,
)

expect class VideoLibrarySource {
    fun observeVideos(): Flow<List<SharedVideoContent>>
    fun getResumePosition(path: String): Long
    fun setResumePosition(path: String, positionMs: Long)
    fun delete(content: SharedVideoContent)
}
