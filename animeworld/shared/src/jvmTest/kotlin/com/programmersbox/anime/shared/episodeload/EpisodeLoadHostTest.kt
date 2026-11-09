package com.programmersbox.anime.shared.episodeload

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class EpisodeLoadHostTest {

    private fun state(
        step: EpisodeLoadStep,
        action: EpisodeAction = EpisodeAction.Play,
    ) = EpisodeLoadState(
        action = action,
        episodeUrl = "https://example.com/ep1",
        episodeName = "Episode 1",
        showTitle = "Show",
        posterUrl = null,
        step = step,
    )

    @Test
    fun showsChecklistWhileFetching() = runComposeUiTest {
        setContent { EpisodeLoadingOverlay(state(EpisodeLoadStep.FetchingSources)) {} }
        onNodeWithText("Fetching sources").assertExists()
        onNodeWithText("Resolving stream").assertExists()
        onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun cancelSendsCancel() = runComposeUiTest {
        val decisions = mutableListOf<EpisodeLoadDecision>()
        setContent { EpisodeLoadingOverlay(state(EpisodeLoadStep.FetchingSources)) { decisions += it } }
        onNodeWithText("Cancel").performClick()
        assertEquals(listOf<EpisodeLoadDecision>(EpisodeLoadDecision.Cancel), decisions)
    }

    @Test
    fun failedShowsActions() = runComposeUiTest {
        setContent {
            EpisodeLoadingOverlay(state(EpisodeLoadStep.Failed("Couldn't load Episode 1", "boom", true))) {}
        }
        onNodeWithText("Retry").assertExists()
        onNodeWithText("Try another source").assertExists()
        onNodeWithText("Back").assertExists()
    }

    @Test
    fun failedWithoutPickOtherHidesIt() = runComposeUiTest {
        setContent {
            EpisodeLoadingOverlay(state(EpisodeLoadStep.Failed("Couldn't load Episode 1", null, false))) {}
        }
        onNodeWithText("Retry").assertExists()
        onNodeWithText("Try another source").assertDoesNotExist()
    }

    @Test
    fun downloadShowsNoOverlay() = runComposeUiTest {
        setContent {
            EpisodeLoadingOverlay(state(EpisodeLoadStep.FetchingSources, EpisodeAction.Download)) {}
        }
        onNodeWithText("Fetching sources").assertDoesNotExist()
    }
}
