package com.programmersbox.anime.shared.videoplayer

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.programmersbox.datastore.DataStoreHandler

/**
 * When true, the video player renders through a TextureView and blurs its bars with the user's
 * selected blur kind. When false, it uses a SurfaceView and the plain translucent bars.
 */
val videoPlayerBlur = DataStoreHandler(
    key = booleanPreferencesKey("video_player_blur"),
    defaultValue = true,
)
