package com.programmersbox.anime.shared.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal actual fun VideoPermissionGate(content: @Composable () -> Unit) {
    // No runtime permissions are needed on desktop - the video library is just a scan of a
    // user-configured downloads folder.
    content()
}

@Composable
internal actual fun VideoThumbnail(path: String, modifier: Modifier) {
    // Video-frame thumbnail extraction relies on Android-only Coil/MediaMetadataRetriever
    // support (see the design doc for this move) - show a generic placeholder instead.
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
        )
    }
}
