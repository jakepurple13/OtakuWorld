package com.programmersbox.anime.shared.episodeload

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun enteringWithoutIndicatorStillShowsChecklistAndCancel() = runComposeUiTest {
        val enter = MutableTransitionState(false).apply { targetState = true }
        setContent {
            EpisodeLoadingOverlay(state(EpisodeLoadStep.FetchingSources), showIndicator = false, contentEnter = enter) {}
        }
        onNodeWithText("Fetching sources").assertExists()
        onNodeWithText("Cancel").assertExists()
        assertTrue(enter.isIdle && enter.currentState)
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

    @Test
    fun openingPlayerHidesCancel() = runComposeUiTest {
        setContent { EpisodeLoadingOverlay(state(EpisodeLoadStep.OpeningPlayer)) {} }
        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithText("Resolving stream").assertExists()
    }

    @Test
    fun diveAfterHandoff() {
        val opening = state(EpisodeLoadStep.OpeningPlayer)
        assertEquals(opening, coverDiveFrom(opening, null))
    }

    @Test
    fun noDiveAfterCancel() {
        assertNull(coverDiveFrom(state(EpisodeLoadStep.Resolving(null)), null))
        assertNull(coverDiveFrom(state(EpisodeLoadStep.Failed("Couldn't load Episode 1", null, true)), null))
    }

    @Test
    fun noDiveForDownload() {
        assertNull(coverDiveFrom(state(EpisodeLoadStep.OpeningPlayer, EpisodeAction.Download), null))
    }

    @Test
    fun noDiveWhileStillShowing() {
        assertNull(coverDiveFrom(state(EpisodeLoadStep.OpeningPlayer), state(EpisodeLoadStep.OpeningPlayer)))
    }

    @Test
    fun closingCallsOnClosedAfterDive() = runComposeUiTest {
        mainClock.autoAdvance = false
        var closed = false
        setContent {
            EpisodeLoadingOverlay(state(EpisodeLoadStep.OpeningPlayer), closing = true, onClosed = { closed = true }) {}
        }
        mainClock.advanceTimeBy(300)
        assertFalse(closed)
        mainClock.advanceTimeBy(400)
        assertTrue(closed)
    }
}
