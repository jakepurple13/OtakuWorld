package com.programmersbox.anime.shared.videos

@androidx.compose.runtime.Composable
internal actual fun VideoPermissionGate(content: @androidx.compose.runtime.Composable (() -> Unit)) {
}

@androidx.compose.runtime.Composable
internal actual fun VideoThumbnail(path: String, modifier: androidx.compose.ui.Modifier) {
}

internal actual fun videoExportRoute(path: String): androidx.navigation3.runtime.NavKey? = null
