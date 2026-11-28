package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExceptionPlatformTest {
    @Test
    fun deviceDescriptionNamesOsAndArch() {
        val d = deviceDescription()
        assertTrue(d.contains(" · "), d)
        assertTrue(d.contains(System.getProperty("os.arch")), d)
    }

    @Test
    fun desktopHasNoShareSheet() {
        assertFalse(hasShareSheet)
    }
}
