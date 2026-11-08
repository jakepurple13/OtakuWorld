package com.programmersbox.sharedutils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldTranslateTest {
    @Test
    fun `und is skipped`() = assertFalse(shouldTranslate("und", "en"))

    @Test
    fun `same as target is skipped`() = assertFalse(shouldTranslate("fr", "fr"))

    @Test
    fun `different is translated`() = assertTrue(shouldTranslate("ja", "en"))
}
