package com.programmersbox.anime.shared.videos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey

/**
 * Deletes videos. Android routes videos other apps saved through the system's delete prompt; desktop
 * moves anything outside the AnimeWorld downloads folder to the Trash.
 */
@Composable
internal expect fun rememberVideoDeleter(): (List<SharedVideoContent>) -> Unit

/** What this device is called in "Other videos on this …". */
internal expect val libraryDeviceName: String

/** Whether deleting a video that isn't an AnimeWorld download moves it to the Trash. */
internal expect val otherVideosGoToTrash: Boolean

/** Extra line for the delete dialog when videos that aren't AnimeWorld downloads are selected. */
internal expect val otherVideosDeleteNote: String?

/** Where "Manage folders" goes; null where library folders aren't configurable (only desktop has them). */
internal expect fun libraryFoldersRoute(): NavKey?

/** Desktop's note while thumbnails and lengths are still being read. Nothing on other platforms. */
@Composable
internal expect fun ThumbnailProgressNotice(modifier: Modifier)

/** Desktop's one-time "Show every video on this computer?" card. Nothing on other platforms. */
@Composable
internal expect fun OtherVideosPrompt(modifier: Modifier)
