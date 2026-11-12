package com.programmersbox.kmpuiviews.utils.composables.transitions

import androidx.compose.animation.EnterExitState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.hypot
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ChapterRevealOriginsTest {

    @BeforeTest
    fun setUp() = ChapterRevealOrigins.clear()

    @Test
    fun takeReturnsRecordedOnce() {
        ChapterRevealOrigins.record("a", Offset(.5f, .25f), nowMs = 0)
        assertEquals(Offset(.5f, .25f), ChapterRevealOrigins.take("a", nowMs = 100))
        assertNull(ChapterRevealOrigins.take("a", nowMs = 100))
    }

    @Test
    fun takeIgnoresOtherKey() {
        ChapterRevealOrigins.record("a", Offset(.5f, .25f), nowMs = 0)
        assertNull(ChapterRevealOrigins.take("b", nowMs = 100))
        assertNotNull(ChapterRevealOrigins.take("a", nowMs = 100))
    }

    @Test
    fun expiresAfterTwoSeconds() {
        ChapterRevealOrigins.record("a", Offset(.5f, .25f), nowMs = 0)
        assertNull(ChapterRevealOrigins.peek(nowMs = 2_001))
        assertNull(ChapterRevealOrigins.take("a", nowMs = 2_001))
        ChapterRevealOrigins.record("a", Offset(.5f, .25f), nowMs = 0)
        assertNotNull(ChapterRevealOrigins.peek(nowMs = 2_000))
    }

    @Test
    fun takeNullKeyReturnsNull() {
        ChapterRevealOrigins.record("a", Offset(.5f, .25f), nowMs = 0)
        assertNull(ChapterRevealOrigins.take(null, nowMs = 0))
    }

    @Test
    fun radiusReachesFarthestCorner() {
        assertEquals(hypot(90f, 180f), revealRadius(Size(100f, 200f), Offset(10f, 20f)), 0.01f)
    }
}

class ChapterOpenTargetTest {
    @Test
    fun visibleIsFullyOpen() = assertEquals(1f, chapterOpenTarget(EnterExitState.Visible, hasEntered = false, covered = false))

    @Test
    fun firstEnterStartsClosed() = assertEquals(0f, chapterOpenTarget(EnterExitState.PreEnter, hasEntered = false, covered = false))

    @Test
    fun uncoveredAfterAnotherScreenPopsStaysOpen() =
        assertEquals(1f, chapterOpenTarget(EnterExitState.PreEnter, hasEntered = true, covered = false))

    @Test
    fun coveredByAnotherScreenStaysOpen() =
        assertEquals(1f, chapterOpenTarget(EnterExitState.PostExit, hasEntered = true, covered = true))

    @Test
    fun poppedCloses() = assertEquals(0f, chapterOpenTarget(EnterExitState.PostExit, hasEntered = true, covered = false))
}
