package com.programmersbox.anime.shared.videos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject

@Composable
internal actual fun rememberVideoDeleter(): (List<SharedVideoContent>) -> Unit {
    val source = koinInject<VideoLibrarySource>()
    return remember(source) { { items -> items.forEach(source::delete) } }
}

internal actual val libraryDeviceName: String = "device"

internal actual val otherVideosGoToTrash: Boolean = false

internal actual val otherVideosDeleteNote: String? = null

@Composable
internal actual fun OtherVideosPrompt(modifier: Modifier) = Unit

internal actual fun libraryFoldersRoute(): androidx.navigation3.runtime.NavKey? = null
