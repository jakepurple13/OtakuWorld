package com.programmersbox.kmpuiviews.presentation.favorite

import kotlin.test.Test
import kotlin.test.assertEquals

class HighlightTextTest {

    @Test fun `markers become ranges on the plain text`() {
        val (text, ranges) = parseHighlight("\u0002Blue\u0003 \u0002Lock\u0003")

        assertEquals("Blue Lock", text)
        assertEquals(listOf(0..3, 5..8), ranges)
    }

    @Test fun `text without markers has no ranges`() {
        assertEquals("Plain text" to emptyList(), parseHighlight("Plain text"))
    }

    @Test fun `snippet with ellipsis keeps offsets`() {
        val (text, ranges) = parseHighlight("… the \u0002mage\u0003 exam")

        assertEquals("… the mage exam", text)
        assertEquals(listOf(6..9), ranges)
    }
}
