package com.programmersbox.anime.shared.videos

actual class VideoLibrarySource {
    actual fun observeVideos(): kotlinx.coroutines.flow.Flow<List<com.programmersbox.anime.shared.videos.SharedVideoContent>> {
        TODO("Not yet implemented")
    }

    actual fun getResumePosition(path: String): Long {
        TODO("Not yet implemented")
    }

    actual fun setResumePosition(path: String, positionMs: Long) {
    }

    actual fun delete(content: com.programmersbox.anime.shared.videos.SharedVideoContent) {
    }
}