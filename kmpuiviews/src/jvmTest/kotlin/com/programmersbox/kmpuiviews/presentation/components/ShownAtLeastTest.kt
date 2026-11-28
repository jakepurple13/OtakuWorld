package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ShownAtLeastTest {

    @Test
    fun aQuickLoadStaysShownForTheMinimum() = runComposeUiTest {
        val loading = mutableStateOf(false)
        var shown = false
        mainClock.autoAdvance = false
        setContent { shown = rememberShownAtLeast(loading.value, minMillis = 600) }
        mainClock.advanceTimeByFrame()
        assertFalse(shown)

        loading.value = true
        mainClock.advanceTimeBy(50)
        assertTrue(shown)

        loading.value = false
        mainClock.advanceTimeBy(300)
        assertTrue(shown)

        mainClock.advanceTimeBy(400)
        assertFalse(shown)
    }

    @Test
    fun aLongLoadHidesAsSoonAsItEnds() = runComposeUiTest {
        val loading = mutableStateOf(true)
        var shown = false
        mainClock.autoAdvance = false
        setContent { shown = rememberShownAtLeast(loading.value, minMillis = 600) }
        mainClock.advanceTimeBy(1_000)
        assertTrue(shown)

        loading.value = false
        mainClock.advanceTimeBy(50)
        assertFalse(shown)
    }
}
