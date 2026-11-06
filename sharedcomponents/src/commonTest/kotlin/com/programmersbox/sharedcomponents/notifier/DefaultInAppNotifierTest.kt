package com.programmersbox.sharedcomponents.notifier

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultInAppNotifierTest {

    private fun TestScope.notifier(scope: CoroutineScope = backgroundScope) =
        DefaultInAppNotifier(scope, testScheduler.timeSource)

    private fun InAppNotifier.titles() = banners.value.map { it.title }

    @Test
    fun newestFirst() = runTest {
        val n = notifier()
        n.success("A")
        n.info("B")
        assertEquals(listOf("B", "A"), n.titles())
    }

    @Test
    fun defaultDurations() = runTest {
        val n = notifier()
        n.success("plain")
        advanceTimeBy(3_999)
        assertEquals(listOf("plain"), n.titles())
        advanceTimeBy(2)
        assertTrue(n.banners.value.isEmpty())

        n.success("act", action = "View")
        advanceTimeBy(5_999)
        assertEquals(listOf("act"), n.titles())
        advanceTimeBy(2)
        assertTrue(n.banners.value.isEmpty())

        n.error("bad")
        advanceTimeBy(6_999)
        assertEquals(listOf("bad"), n.titles())
        advanceTimeBy(2)
        assertTrue(n.banners.value.isEmpty())

        n.success("custom", durationMillis = 2_000)
        advanceTimeBy(2_001)
        assertTrue(n.banners.value.isEmpty())
    }

    @Test
    fun pauseKeepsRemainingTime() = runTest {
        val n = notifier()
        n.success("A")
        advanceTimeBy(1_000)
        n.setPaused(PauseReason.Expanded, true)
        advanceTimeBy(10_000)
        assertEquals(1, n.banners.value.size)
        n.setPaused(PauseReason.Expanded, false)
        advanceTimeBy(2_999)
        assertEquals(1, n.banners.value.size)
        advanceTimeBy(2)
        assertTrue(n.banners.value.isEmpty())
    }

    @Test
    fun pausedByEitherReason() = runTest {
        val n = notifier()
        n.success("A")
        n.setPaused(PauseReason.Expanded, true)
        n.setPaused(PauseReason.Background, true)
        advanceTimeBy(1_000)
        n.setPaused(PauseReason.Expanded, false)
        advanceTimeBy(10_000)
        assertEquals(1, n.banners.value.size)
        n.setPaused(PauseReason.Background, false)
        advanceTimeBy(4_001)
        assertTrue(n.banners.value.isEmpty())
    }

    @Test
    fun sameKeyReplacesInPlaceAndResetsTimer() = runTest {
        val n = notifier()
        val first = n.info("Ch 1", key = "r", durationMillis = 2_000)
        advanceTimeBy(1_500)
        val second = n.info("Ch 2", key = "r", durationMillis = 2_000)
        assertEquals(first, second)
        assertEquals(listOf("Ch 2"), n.titles())
        advanceTimeBy(1_500)
        assertEquals(listOf("Ch 2"), n.titles())
        advanceTimeBy(501)
        assertTrue(n.banners.value.isEmpty())
    }

    @Test
    fun progressNeverExpiresAndMorphsInPlace() = runTest {
        val n = notifier()
        val h = n.progress("Checking")
        advanceTimeBy(60_000)
        assertEquals(1, n.banners.value.size)
        h.update("Checking 3 of 14", fraction = 3 / 14f)
        val b = n.banners.value.single()
        assertEquals("Checking 3 of 14", b.title)
        assertEquals(3 / 14f, b.fraction)
        assertEquals(BannerKind.Progress, b.kind)

        h.success("3 new")
        val done = n.banners.value.single()
        assertEquals(h.id, done.id)
        assertEquals(BannerKind.Success, done.kind)
        assertEquals("3 new", done.title)
        assertNull(done.fraction)
        advanceTimeBy(4_001)
        assertTrue(n.banners.value.isEmpty())
    }

    @Test
    fun progressResultAfterDismissShowsNewBanner() = runTest {
        val n = notifier()
        val h = n.progress("Casting…")
        n.dismiss(h.id)
        h.error("Couldn't cast")
        val b = n.banners.value.single()
        assertEquals(BannerKind.Error, b.kind)
        assertNotEquals(h.id, b.id)
    }

    @Test
    fun capDropsOldest() = runTest {
        val n = notifier()
        repeat(21) { n.success("n$it") }
        assertEquals(BannerDefaults.MaxBanners, n.banners.value.size)
        assertFalse("n0" in n.titles())
        assertEquals("n20", n.titles().first())
    }

    @Test
    fun actionRunsOnceAndDismisses() = runTest {
        val n = notifier()
        var count = 0
        val id = n.success("A", action = "Go", onAction = { count++ })
        n.performAction(id)
        n.performAction(id)
        runCurrent()
        assertEquals(1, count)
        assertTrue(n.banners.value.isEmpty())
    }

    @Test
    fun actionStartsBeforeBannerLeaves() = runTest {
        val n = notifier()
        var started = false
        var startedWhenGone: Boolean? = null
        val id = n.success("A", action = "Go", onAction = { started = true })
        backgroundScope.launch {
            n.banners.collect { list ->
                if (startedWhenGone == null && list.none { it.id == id }) startedWhenGone = started
            }
        }
        runCurrent()
        n.performAction(id)
        runCurrent()
        assertEquals(true, startedWhenGone)
    }

    @Test
    fun reentrantPerformActionRunsOnce() = runTest {
        val n = notifier()
        var count = 0
        var id: BannerId? = null
        id = n.success("A", action = "Go", onAction = {
            count++
            if (count < 5) n.performAction(id!!)
        })
        n.performAction(id)
        runCurrent()
        assertEquals(1, count)
    }

    @Test
    fun keyedReplacementPostedByActionSurvives() = runTest {
        val n = notifier()
        val id = n.error("Failed", key = "k", action = "Retry", onAction = {
            n.error("Retry failed", key = "k")
        })
        n.performAction(id)
        runCurrent()
        assertEquals(listOf("Retry failed"), n.titles())
    }

    @Test
    fun actionRunsAfterCallerScopeCancelled() = runTest {
        val n = notifier()
        var ran = false
        val callerJob = Job()
        val caller = CoroutineScope(callerJob + StandardTestDispatcher(testScheduler))
        var id: BannerId? = null
        caller.launch { id = n.success("A", action = "Go", onAction = { ran = true }) }
        runCurrent()
        assertNotNull(id)
        callerJob.cancel()
        n.performAction(id!!)
        runCurrent()
        assertTrue(ran)
    }

    @Test
    fun oldTimerDoesNotRemoveKeyedReplacementAtExpiryInstant() = runTest {
        val n = notifier()
        n.info("Ch 1", key = "r", durationMillis = 2_000)
        advanceTimeBy(2_000) // old expiry is queued at exactly t=2000 but has not run
        n.info("Ch 2", key = "r", durationMillis = 2_000)
        runCurrent()
        assertEquals(listOf("Ch 2"), n.titles())
        advanceTimeBy(1_999)
        assertEquals(listOf("Ch 2"), n.titles())
        advanceTimeBy(2)
        assertTrue(n.banners.value.isEmpty())
    }

    @Test
    fun throwingActionDoesNotBreakNotifier() = runTest {
        val n = notifier()
        val id = n.success("A", action = "Go", onAction = { throw IllegalStateException("boom") })
        n.performAction(id)
        runCurrent()
        assertTrue(n.banners.value.isEmpty())
        n.success("next")
        assertEquals(listOf("next"), n.titles())
    }

    @Test
    fun bannerSentWithoutCollectorIsKept() = runTest {
        val n = notifier()
        n.success("A")
        assertEquals(1, n.banners.value.size)
    }

    @Test
    fun timeoutAdjusterApplies() = runTest {
        val n = notifier()
        n.setTimeoutAdjuster { ms, _ -> ms * 2 }
        n.success("A")
        advanceTimeBy(7_999)
        assertEquals(1, n.banners.value.size)
        advanceTimeBy(2)
        assertTrue(n.banners.value.isEmpty())
    }

    @Test
    fun dismissAllClears() = runTest {
        val n = notifier()
        n.success("A")
        n.error("B")
        n.dismissAll()
        assertTrue(n.banners.value.isEmpty())
    }
}
