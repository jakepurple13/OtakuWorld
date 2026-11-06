package com.programmersbox.kmpuiviews.presentation.settings.translationmodels

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatModelSizeTest {
    @Test
    fun megabytesUseOneDecimal() {
        assertEquals("30.0 MB", formatModelSize(30_000_000L))
        assertEquals("27.8 MB", formatModelSize(27_812_345L))
        assertEquals("0.0 MB", formatModelSize(0L))
    }

    @Test
    fun gigabytesUseTwoDecimals() {
        assertEquals("1.00 GB", formatModelSize(999_960_000L))
        assertEquals("1.05 GB", formatModelSize(1_050_000_000L))
    }

    @Test
    fun estimatesArePrefixed() {
        assertEquals("about 30.0 MB", formatModelSize(30_000_000L, estimate = true))
    }
}
