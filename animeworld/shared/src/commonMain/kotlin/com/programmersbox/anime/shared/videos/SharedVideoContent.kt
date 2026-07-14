package com.programmersbox.anime.shared.videos

import kotlinx.coroutines.flow.Flow

data class SharedVideoContent(
    val videoName: String,
    val path: String,
    val duration: Long,
    val lastPlayedPositionMs: Long,
)

expect class VideoLibrarySource {
    fun observeVideos(): Flow<List<SharedVideoContent>>
    fun getResumePosition(path: String): Long
    fun setResumePosition(path: String, positionMs: Long)
    fun delete(content: SharedVideoContent)
}
