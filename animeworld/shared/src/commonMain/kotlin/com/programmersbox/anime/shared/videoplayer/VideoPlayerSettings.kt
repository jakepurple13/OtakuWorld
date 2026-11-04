package com.programmersbox.anime.shared.videoplayer

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.DataStoreHandler
import com.programmersbox.datastore.DataStoreHandlerObject

/**
 * When true, the player blurs its bars with the user's selected blur kind; otherwise the bars are
 * plain translucent surfaces. On Android it also makes the player render through a TextureView,
 * since a SurfaceView can't be blurred.
 */
val videoPlayerBlur = DataStoreHandler(
    key = booleanPreferencesKey("video_player_blur"),
    defaultValue = true,
)

/** How the player's bars look; see [PlayerStyle]. */
val videoPlayerStyle = DataStoreHandlerObject(
    key = stringPreferencesKey("video_player_style"),
    defaultValue = PlayerStyle.Classic,
    mapToType = { name -> PlayerStyle.entries.firstOrNull { it.name == name } },
    mapToKey = { it.name },
)

enum class PlayerStyle(val label: String, val description: String) {
    Classic("Classic", "Full-width bars that slide in from the edges."),
    Floating("Floating", "Rounded floating pills and a control dock. Shows the most video."),
    Theater("Theater", "No boxes. Blur fades in from the top and bottom edges."),
}
