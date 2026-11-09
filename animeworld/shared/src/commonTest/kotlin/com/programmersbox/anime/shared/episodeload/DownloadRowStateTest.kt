package com.programmersbox.anime.shared.episodeload

import com.programmersbox.kmpuiviews.ChapterDownloadUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadRowStateTest {

    private fun state(action: EpisodeAction) = EpisodeLoadState(
        action = action,
        episodeUrl = "u",
        episodeName = "Episode 1",
        showTitle = "Show",
        posterUrl = null,
        step = EpisodeLoadStep.FetchingSources,
    )

    @Test
    fun downloadRowStatesMarksOnlyDownload() {
        assertEquals(
            mapOf("u" to ChapterDownloadUiState.Queued),
            downloadRowStates(state(EpisodeAction.Download)),
        )
        assertEquals(emptyMap(), downloadRowStates(state(EpisodeAction.Play)))
        assertEquals(emptyMap(), downloadRowStates(null))
    }
}
