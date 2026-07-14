package com.programmersbox.anime.shared.videoplayer

import androidx.compose.runtime.Composable
import com.programmersbox.anime.shared.VideoNotSupportedScreen
import com.programmersbox.anime.shared.VideoScreen

@Composable
actual fun VideoPlayerUi(videoScreen: VideoScreen) {
    // TODO(user): replace with a real desktop video player implementation.
    // videoScreen.showPath is the stream URL or local file path to play;
    // videoScreen.referer carries any required request header for streaming.
    VideoNotSupportedScreen()
}
