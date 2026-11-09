package com.programmersbox.anime.shared.episodeload

import com.programmersbox.kmpuiviews.ChapterDownloadUiState

/** Row state for the episode whose download is still resolving; Play loads never mark a row. */
internal fun downloadRowStates(state: EpisodeLoadState?): Map<String, ChapterDownloadUiState> =
    if (state?.action == EpisodeAction.Download) {
        mapOf(state.episodeUrl to ChapterDownloadUiState.Queued)
    } else {
        emptyMap()
    }
