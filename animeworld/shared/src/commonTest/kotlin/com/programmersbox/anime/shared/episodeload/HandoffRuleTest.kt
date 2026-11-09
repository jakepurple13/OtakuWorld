package com.programmersbox.anime.shared.episodeload

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HandoffRuleTest {

    @Test
    fun phoneInPortraitWaitsForRotation() {
        assertFalse(shouldFinishHandoff(isLandscape = false, inMultiWindow = false, smallestWidthDp = 360))
        assertFalse(shouldFinishHandoff(isLandscape = false, inMultiWindow = false, smallestWidthDp = 599))
    }

    @Test
    fun phoneInLandscapeFinishes() {
        assertTrue(shouldFinishHandoff(isLandscape = true, inMultiWindow = false, smallestWidthDp = 360))
    }

    @Test
    fun multiWindowFinishesInPortrait() {
        assertTrue(shouldFinishHandoff(isLandscape = false, inMultiWindow = true, smallestWidthDp = 360))
    }

    @Test
    fun largeScreenFinishesInPortrait() {
        assertTrue(shouldFinishHandoff(isLandscape = false, inMultiWindow = false, smallestWidthDp = 600))
        assertTrue(shouldFinishHandoff(isLandscape = false, inMultiWindow = false, smallestWidthDp = 840))
    }
}
