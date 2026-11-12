package com.programmersbox.anime.shared

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class VideoScreen(
    val showPath: String,
    val showName: String,
    val downloadOrStream: Boolean,
    val referer: String,
    // Cover color handed over by Details to theme the player
    val seedColor: Int? = null,
    // Episode page URL; streams save their resume position under it, since showPath is often a signed, expiring link
    val episodeUrl: String? = null,
) : NavKey
