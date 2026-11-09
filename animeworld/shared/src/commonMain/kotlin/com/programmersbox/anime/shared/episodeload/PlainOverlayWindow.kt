package com.programmersbox.anime.shared.episodeload

import androidx.compose.runtime.Composable

/** Call inside a Dialog's content: turns off the platform window's dim and enter animation, where it has them. */
@Composable
internal expect fun PlainOverlayWindow()
