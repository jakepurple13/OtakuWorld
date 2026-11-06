package com.programmersbox.kmpuiviews.notifier

import com.programmersbox.sharedcomponents.notifier.DefaultInAppNotifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShowUndoableTest {

    @Test
    fun undoTapReturnsTrueAndRunsOnUndo() = runTest {
        val n = DefaultInAppNotifier(backgroundScope, testScheduler.timeSource)
        var undone = 0
        val result = async { n.showUndoable("Removed", "Undo") { undone++ } }
        runCurrent()
        val id = n.banners.value.single().id
        n.performAction(id)
        runCurrent()
        assertTrue(result.await())
        assertEquals(1, undone)
    }

    @Test
    fun timeoutReturnsFalseWithoutUndo() = runTest {
        val n = DefaultInAppNotifier(backgroundScope, testScheduler.timeSource)
        var undone = 0
        val result = async { n.showUndoable("Removed", "Undo") { undone++ } }
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertFalse(result.await())
        assertEquals(0, undone)
        assertTrue(n.banners.value.isEmpty())
    }
}
