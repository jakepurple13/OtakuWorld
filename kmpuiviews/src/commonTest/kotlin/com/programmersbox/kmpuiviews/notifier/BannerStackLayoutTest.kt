package com.programmersbox.kmpuiviews.notifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BannerStackLayoutTest {
    private val heights = listOf(100f, 80f, 90f, 70f)

    @Test
    fun collapsed() {
        val s = bannerSlots(heights, expanded = false, peekPx = 9f, gapPx = 8f)
        assertEquals(listOf(0f, 9f, 18f, 27f), s.map { it.translationY })
        assertEquals(listOf(1f, 0.95f, 0.90f, 0.85f), s.map { it.scale })
        assertEquals(0f, s[3].alpha)
        assertEquals(listOf(1f, 0f, 0f, 0f), s.map { it.contentAlpha })
        assertEquals(listOf(true, true, true, false), s.map { it.interactive })
    }

    @Test
    fun expanded() {
        val s = bannerSlots(heights, expanded = true, peekPx = 9f, gapPx = 8f)
        assertEquals(listOf(0f, 108f, 196f, 294f), s.map { it.translationY })
        assertTrue(s.all { it.scale == 1f && it.alpha == 1f && it.contentAlpha == 1f && it.interactive })
    }

    @Test
    fun zIndexDecreases() {
        val s = bannerSlots(heights, expanded = false, peekPx = 9f, gapPx = 8f)
        assertTrue(s.zipWithNext().all { (a, b) -> a.zIndex > b.zIndex })
    }
}
