package com.programmersbox.manga.shared.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberReaderPlatformActions(): ReaderPlatformActions = remember {
    ReaderPlatformActions(toggleFullscreen = null, exitFullscreen = { false }, openShortcuts = null)
}
