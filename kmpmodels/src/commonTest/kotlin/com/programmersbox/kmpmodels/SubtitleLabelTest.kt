package com.programmersbox.kmpmodels

import kotlin.test.Test
import kotlin.test.assertEquals

class SubtitleLabelTest {
    @Test
    fun cleanSubtitleLabel_unescapes() {
        assertEquals("English - [BestBob] Signs/Songs", cleanSubtitleLabel("English - [BestBob] Signs\\/Songs"))
        assertEquals("\"Dub\" \\ Sub", cleanSubtitleLabel("  \\\"Dub\\\" \\\\ Sub "))
        assertEquals("Español", cleanSubtitleLabel("Espa\\u00F1ol"))
        assertEquals("Plain", cleanSubtitleLabel("Plain"))
    }
}
