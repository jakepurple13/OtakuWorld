package com.programmersbox.anime.shared.videoplayer

import androidx.compose.runtime.Composable
import com.programmersbox.anime.shared.VideoScreen

@Composable
expect fun VideoPlayerUi(videoScreen: VideoScreen)
